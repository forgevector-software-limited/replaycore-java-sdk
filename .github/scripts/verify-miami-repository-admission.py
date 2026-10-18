#!/usr/bin/env python3
# Copyright (c) ForgeVector Software Limited. Author: ForgeVector Software Limited.
"""Read-only SDK/Ops proof of the existing organization hook lease. Never acquires or extends it."""
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import sys
import time

UID, GID = 989, 982
RUNNER = Path('/srv/replaycore-miami/rc-miami-ci/runners/ci-1')
RUNTIME = Path('/run/user/989/replaycore-ci-admission-v1')
HELPER = Path('/usr/local/libexec/replaycore-miami/ci-resource-admission.py')
HOOK = HELPER.parent / 'repositories/runner-admission-hook.py'
HELPER_SHA = '61d8c1ce515f5bbf966efd373ed97c8455a8ff221f7d447365f7f13bee51df31'
HOOK_SHA = '9cfaa4fc92ee13112414c79fadfa2e966c5108b514a9c04f1531d2bc83552a25'
MAINTENANCE = Path('/srv/replaycore-miami/locks/ci-maintenance.lock')
CGROUP = '/user.slice/user-989.slice/actions.runner.forgevector-software-limited-replaycore.miami-replaycore-1.service'
SCHEMA, PROFILE = 'replaycore-miami-job-admission/v1', 'protected-producer-v1'
REQUIRED_REMAINING_SECONDS = 6960  # 110 minute job, 300 second cleanup, 60 second margin.
DISK_BYTES, FLOOR_BYTES = 12884901888, 100000000000


def require(ok, reason):
    if not ok:
        raise RuntimeError(reason)


def integer(value):
    return isinstance(value, int) and not isinstance(value, bool)


def validate_native_limits(cpu, memory, swap):
    values = cpu.split()
    require(len(values) == 2 and all(x.isdigit() for x in values), 'native CPU quota missing')
    quota, period = map(int, values)
    require(period > 0 and quota == 2 * period, 'native CPU quota differs')
    require(memory.strip() == '24000000000' and swap.strip() == '0', 'native memory or swap ceiling differs')


def parse_stat(raw):
    fields = raw[raw.rfind(')') + 2:].split()
    require(len(fields) >= 20 and fields[0] not in ('Z', 'X'), 'process is not live')
    require(fields[19].isdigit(), 'process start time missing')
    return int(fields[1]), fields[19]


def parse_flags(argv, role):
    require(len(argv) >= 3 and argv[:3] == ['/usr/bin/python3', str(HELPER), role], 'helper command differs')
    tail = argv[3:]
    require(len(tail) % 2 == 0, 'helper flags malformed')
    result = {}
    for key, value in zip(tail[::2], tail[1::2]):
        require(key.startswith('--') and key not in result, 'duplicate helper flag')
        result[key] = value
    require('--test-mode' not in result and '--test-root' not in result, 'test helper refused')
    return result


def fd_lock_matches(text, pid, mode, device, inode):
    for line in text.splitlines():
        fields = line.split()
        if len(fields) != 9 or fields[:1] != ['lock:'] or fields[2:5] != ['FLOCK', 'ADVISORY', mode]:
            continue
        try:
            major, minor, item = fields[6].split(':')
            if (int(fields[5]), int(major, 16), int(minor, 16), int(item)) == (pid, os.major(device), os.minor(device), inode):
                return fields[7:] == ['0', 'EOF']
        except (ValueError, OverflowError):
            continue
    return False


def validate_records(state, ready, reservation, now_ns, boot_id, worker, holder, watcher):
    require(set(state) >= {'worker_pid', 'worker_start_ticks', 'holder_pid', 'holder_start_ticks',
                          'watcher_pid', 'watcher_start_ticks', 'lease'}, 'hook state incomplete')
    lease = state['lease']
    require(isinstance(lease, str) and re.fullmatch('[0-9a-f]{32}', lease) is not None, 'lease identity invalid')
    for prefix, identity in [('worker', worker), ('holder', holder), ('watcher', watcher)]:
        require(state[prefix + '_pid'] == identity[0] and state[prefix + '_start_ticks'] == identity[1], 'hook process identity differs')
    fields = ('schema', 'profile', 'boot_id', 'acquired_monotonic_ns',
              'hold_deadline_monotonic_ns', 'watch_deadline_monotonic_ns')
    for value in (state, ready, reservation):
        require(all(key in value for key in fields), 'required long-lease evidence missing')
        require(value['schema'] == SCHEMA and value['profile'] == PROFILE and value['boot_id'] == boot_id, 'admission profile or boot differs')
        require(all(integer(value[key]) for key in fields[3:]), 'lease clocks invalid')
        require(all(value[key] == state[key] for key in fields), 'lease evidence disagrees')
    acquired, deadline, watch_deadline = (state[key] for key in fields[3:])
    require(0 < acquired <= now_ns and deadline - acquired == 7200 * 10**9, 'producer lease ceiling differs')
    require(watch_deadline - acquired == 7500 * 10**9, 'watchdog ceiling differs')
    require(deadline - now_ns >= REQUIRED_REMAINING_SECONDS * 10**9, 'producer lease has insufficient time remaining')
    require(ready.get('status') == 'acquired' and integer(ready.get('weight')) and integer(ready.get('capacity'))
            and ready.get('weight') == 1 and ready.get('capacity') == 1,
            'an actual 1/1 grant is required')
    require(ready.get('pid') == holder[0], 'ready holder differs')
    require(reservation.get('pid') == holder[0] and reservation.get('start_ticks') == holder[1], 'reservation holder differs')
    require(reservation.get('worker_pid') == worker[0] and reservation.get('worker_start_ticks') == worker[1], 'reservation worker differs')
    for value in (ready, reservation):
        require(integer(value.get('disk_bytes')) and integer(value.get('disk_floor_bytes'))
                and value.get('disk_bytes') == DISK_BYTES and value.get('disk_floor_bytes') == FLOOR_BYTES,
                'disk reservation differs')
        require(integer(value.get('disk_device')) and value['disk_device'] >= 0, 'disk device invalid')
    require(ready['disk_device'] == reservation['disk_device'], 'disk devices disagree')
    return lease, (deadline - now_ns) // 10**9


class LinuxProbe:
    def read(self, path, limit):
        with Path(path).open('rb') as stream:
            data = stream.read(limit + 1)
        require(len(data) <= limit, 'bounded read exceeded')
        return data

    def text(self, path, limit=16384):
        return self.read(path, limit).decode('utf-8', errors='strict')

    def private_json(self, path):
        info = Path(path).lstat()
        require(stat.S_ISREG(info.st_mode) and info.st_uid == UID and info.st_gid == GID and stat.S_IMODE(info.st_mode) == 0o600,
                'private admission file differs')
        def unique(pairs):
            value = {}
            for key, item in pairs:
                require(key not in value, 'duplicate admission field')
                value[key] = item
            return value
        raw = self.read(path, 8192)
        return json.loads(raw, object_pairs_hook=unique), hashlib.sha256(raw).hexdigest()

    def process(self, pid):
        require(integer(pid) and pid > 1, 'process ID invalid')
        proc = Path('/proc') / str(pid)
        require(proc.stat().st_uid == UID, 'foreign process owner')
        parent, ticks = parse_stat(self.text(proc / 'stat'))
        require(self.text(proc / 'cgroup').strip() == '0::' + CGROUP, 'process outside native runner cgroup')
        return parent, ticks

    def worker(self):
        pid = os.getpid()
        for _ in range(32):
            parent, ticks = self.process(pid)
            if (Path('/proc') / str(pid) / 'exe').resolve(strict=True) == RUNNER / 'bin/Runner.Worker':
                return pid, ticks
            require(parent > 1 and parent != pid, 'owning worker not found')
            pid = parent
        raise RuntimeError('owning worker ancestry limit')

    def command(self, pid):
        return [x.decode('utf-8', errors='strict') for x in self.read(Path('/proc') / str(pid) / 'cmdline', 8192).split(b'\0') if x]

    def root_file(self, path):
        for part in (Path(path), *Path(path).parents):
            info = part.lstat()
            require(not stat.S_ISLNK(info.st_mode) and info.st_uid == 0 and not info.st_mode & 0o022,
                    'host helper or lock ancestry is writable')

    def lock(self, pid, path, mode, owner, permissions):
        path = Path(path)
        target = path.lstat()
        require(stat.S_ISREG(target.st_mode) and target.st_uid == owner and target.st_gid == GID
                and stat.S_IMODE(target.st_mode) == permissions,
                'lock identity differs')
        fds = list((Path('/proc') / str(pid) / 'fd').iterdir())
        require(len(fds) <= 128, 'holder FD limit')
        for fd in fds:
            try:
                info = fd.stat()
                if (info.st_dev, info.st_ino) != (target.st_dev, target.st_ino):
                    continue
                if fd_lock_matches(self.text(Path('/proc') / str(pid) / 'fdinfo' / fd.name), pid, mode, target.st_dev, target.st_ino):
                    final = path.lstat()
                    require((final.st_dev, final.st_ino) == (target.st_dev, target.st_ino), 'lock path changed')
                    return
            except FileNotFoundError:
                continue
        raise RuntimeError('holder does not own required kernel lock')


REPOSITORIES = {
    '1277032224': ('forgevector-software-limited/replaycore-java-sdk', 'sdk', 'replaycore-java-sdk'),
    '1296762518': ('forgevector-software-limited/replaycore-ops', 'ops', 'replaycore-ops'),
}
def repository_context(environment):
    identity = environment.get('GITHUB_REPOSITORY_ID')
    require(identity in REPOSITORIES, 'SDK/Ops repository ID required')
    repository, key, leaf = REPOSITORIES[identity]
    require(environment.get('GITHUB_REPOSITORY') == repository, 'repository name differs')
    workspace = str(RUNNER / '_work' / leaf / leaf)
    require(environment.get('GITHUB_WORKSPACE') == workspace, 'repository workspace differs')
    home = '/srv/replaycore-miami/rc-miami-ci/repository-homes/' + key
    for name, value in [('HOME', home), ('TMPDIR', home + '/tmp'), ('XDG_CACHE_HOME', home + '/.cache'),
                        ('GRADLE_USER_HOME', home + '/.gradle'), ('NPM_CONFIG_CACHE', home + '/.cache/npm'),
                        ('DOCKER_HOST', ''), ('DBUS_SESSION_BUS_ADDRESS', '')]:
        require(environment.get(name) == value, 'root-hook job environment differs: ' + name)
    return int(identity), key


def main():
    require((os.getuid(), os.getgid()) == (UID, GID), 'Miami CI UID/GID required')
    require(os.environ.get('GITHUB_ACTIONS') == 'true' and os.environ.get('RUNNER_OS') == 'Linux'
            and os.environ.get('RUNNER_NAME') == 'miami-replaycore-1', 'Miami GitHub runner required')
    repository_id, repository_key = repository_context(os.environ)
    probe = LinuxProbe()
    for directory in (RUNTIME, RUNTIME / 'ready'):
        info = directory.lstat()
        require(stat.S_ISDIR(info.st_mode) and info.st_uid == UID and info.st_gid == GID
                and stat.S_IMODE(info.st_mode) == 0o700, 'private admission directory differs')
    worker = probe.worker()
    state_path = RUNTIME / ('hook-' + str(worker[0]) + '-' + worker[1] + '.json')
    state, state_sha = probe.private_json(state_path)
    require(state.get('repository_id') == repository_id and state.get('runner_name') == 'miami-replaycore-1', 'hook repository binding differs')
    holder = (state.get('holder_pid'), state.get('holder_start_ticks'))
    watcher = (state.get('watcher_pid'), state.get('watcher_start_ticks'))
    require(probe.process(holder[0])[1] == holder[1] and probe.process(watcher[0])[1] == watcher[1], 'helper process changed')
    lease = state.get('lease')
    require(isinstance(lease, str) and re.fullmatch('[0-9a-f]{32}', lease), 'hook lease invalid')
    ready, ready_sha = probe.private_json(RUNTIME / 'ready' / (lease + '.json'))
    reservation, reservation_sha = probe.private_json(RUNTIME / ('reservation-' + lease + '.json'))
    boot = probe.text('/proc/sys/kernel/random/boot_id', 64).strip()
    require(re.fullmatch('[0-9a-f-]{36}', boot) is not None, 'boot ID invalid')
    validate_records(state, ready, reservation, time.monotonic_ns(), boot, worker, holder, watcher)
    for file, expected in ((HELPER, HELPER_SHA), (HOOK, HOOK_SHA)):
        probe.root_file(file)
        require(hashlib.sha256(probe.read(file, 262144)).hexdigest() == expected, 'installed admission source differs')
    limits = Path('/sys/fs/cgroup/user.slice/user-989.slice')
    validate_native_limits(probe.text(limits / 'cpu.max', 64), probe.text(limits / 'memory.max', 64),
                           probe.text(limits / 'memory.swap.max', 64))
    for pid, role in ((holder[0], 'hold'), (watcher[0], 'watch')):
        flags = parse_flags(probe.command(pid), role)
        require(flags.get('--token') == lease and flags.get('--worker-pid') == str(worker[0])
                and flags.get('--worker-start-ticks') == worker[1], 'helper worker binding differs')
        if role == 'hold':
            expected = {'--weight':'1', '--capacity':'1', '--hold-seconds':'7200', '--disk-bytes':str(DISK_BYTES),
                        '--disk-floor-bytes':str(FLOOR_BYTES), '--disk-path':str(RUNNER), '--maintenance-lock':str(MAINTENANCE)}
        else:
            expected = {'--holder-pid':str(holder[0]), '--holder-start-ticks':holder[1], '--watch-seconds':'7500'}
        require(all(flags.get(key) == value for key, value in expected.items()), 'helper resource or deadline flags differ')
    require(probe.text(RUNTIME / 'capacity', 32).strip() == '1', 'host admission capacity differs')
    probe.lock(holder[0], RUNTIME / 'slot-00.lock', 'WRITE', UID, 0o600)
    probe.lock(holder[0], MAINTENANCE, 'READ', 0, 0o660)
    require(Path(os.environ['GITHUB_WORKSPACE']).stat().st_dev == reservation['disk_device'], 'workspace filesystem differs')
    space = os.statvfs(RUNNER)
    require(space.f_bavail * space.f_frsize >= FLOOR_BYTES, 'live free disk below safety floor')
    for identity in (worker, holder, watcher):
        require(probe.process(identity[0])[1] == identity[1], 'process changed during proof')
    require(probe.private_json(state_path)[1] == state_sha, 'hook evidence changed during proof')
    require(probe.private_json(RUNTIME / 'ready' / (lease + '.json'))[1] == ready_sha
            and probe.private_json(RUNTIME / ('reservation-' + lease + '.json'))[1] == reservation_sha,
            'grant evidence changed during proof')
    require(probe.text('/proc/sys/kernel/random/boot_id', 64).strip() == boot, 'boot changed during proof')
    _, remaining = validate_records(state, ready, reservation, time.monotonic_ns(), boot, worker, holder, watcher)
    print(json.dumps({'status':'verified-inherited', 'repository_id':repository_id, 'repository_key':repository_key, 'profile':PROFILE, 'weight':1, 'capacity':1,
                      'worker_pid':worker[0], 'worker_start_ticks':worker[1], 'remaining_seconds':remaining,
                      'hook_sha256':state_sha, 'ready_sha256':ready_sha, 'reservation_sha256':reservation_sha}))


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, TypeError, KeyError, RuntimeError) as error:
        print('Miami inherited producer admission refused: ' + str(error), file=sys.stderr)
        sys.exit(1)
