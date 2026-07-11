# Plugin extensions

The package `uk.co.forgevector.replaycore.api.plugin` is the supported Java
contract for plugins running on the same Minecraft server as ReplayCore. Contract
version `1.1` provides recording state, lifecycle events, timeline bookmarks,
on-demand clips, and recent kill-replay links.

## Add the contract to your plugin

Use the SDK as a compile-only dependency when calling the in-process API. The
running ReplayCore plugin supplies these classes:

```groovy
dependencies {
    compileOnly 'com.github.forgevector-software-limited:replaycore-java-sdk:v1.1.2'
}
```

```yaml
# plugin.yml
softdepend: [ReplayCore]
```

Do not shade or relocate the `api.plugin` package into an integration plugin.
ReplayCore and the integration must use the same API classes for service
discovery. If ReplayCore is an optional dependency, keep integration code in a
class that is loaded only after you have confirmed ReplayCore is present.

The REST client has a different deployment model. It may be included in or
shaded into a standalone plugin that makes remote API calls.

## Discover the API

Resolve the API after ReplayCore has enabled:

```java
import java.util.Optional;
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreApi;
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider;

Optional<ReplayCoreApi> available = ReplayCoreProvider.get();
if (!available.isPresent()) {
    getLogger().info("ReplayCore integration is unavailable.");
    return;
}

ReplayCoreApi replayCore = available.get();
if (!replayCore.apiVersion().startsWith("1.")) {
    getLogger().warning("Unsupported ReplayCore API version: " + replayCore.apiVersion());
    return;
}
```

`timeline()` and `recordingControl()` are available while ReplayCore is enabled.
`clips()` and `killReplay()` return `Optional` because those features depend on
the server's ReplayCore configuration.

## Read recording state

```java
import uk.co.forgevector.replaycore.api.plugin.RecordingControlApi;

RecordingControlApi recording = replayCore.recordingControl();
if (recording.isRecording()) {
    recording.currentTick().ifPresent(tick ->
            getLogger().fine("ReplayCore tick: " + tick));
}
```

`currentSession()` returns the active `RecordingSession`, including its stable
session id, server id, optional integration name, and start time. It is a
read-only snapshot and does not start or stop recording.

## Observe recording lifecycle

```java
import uk.co.forgevector.replaycore.api.plugin.RecordingListener;
import uk.co.forgevector.replaycore.api.plugin.RecordingSession;

RecordingListener listener = new RecordingListener() {
    @Override
    public void onRecordingStarted(RecordingSession session) {
        getLogger().info("ReplayCore recording started: " + session.sessionId());
    }

    @Override
    public void onRecordingStopped(RecordingSession session) {
        getLogger().info("ReplayCore recording stopped: " + session.sessionId());
    }
};

replayCore.registerListener(listener);
```

Unregister listeners when your plugin disables. Lifecycle callbacks run on the
server thread, so return quickly and move file, database, or network work to an
appropriate scheduler.

## Add a timeline bookmark

Use `IntegrationBookmark` for new integrations:

```java
import uk.co.forgevector.replaycore.api.plugin.IntegrationBookmark;

boolean accepted = replayCore.timeline().tagTimelineEvent(
        IntegrationBookmark.builder("MyGameMode", "objective_captured")
                .severity(IntegrationBookmark.Severity.INFO)
                .player(player.getUniqueId(), player.getName())
                .arena(arenaId)
                .message("Captured the red flag")
                .metadata("team", "blue")
                .build());

if (!accepted) {
    getLogger().fine("ReplayCore did not record the bookmark.");
}
```

`source` identifies your integration and `type` identifies the event. Keep both
stable so viewer filters and later analysis remain consistent. Bookmark values
are bounded by the SDK. An ordinary refusal is reported as `false`, for example
when no recording is active.

## Save a clip

The optional clip API mirrors the server's ReplayCore clip commands without
making the integration dispatch commands:

```java
import java.time.Duration;
import java.util.UUID;
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreClipApi;

UUID requester = staff.getUniqueId();
UUID target = reportedPlayer.getUniqueId();

replayCore.clips().ifPresent(clips -> {
    ReplayCoreClipApi.SaveResult result =
            clips.saveClip(requester, target, Duration.ofSeconds(30));
    if (result != ReplayCoreClipApi.SaveResult.SAVING) {
        getLogger().fine("ReplayCore clip not saved: " + result);
    }
});
```

For a window whose end is not known in advance, call `startClip(requester,
target)` and later `stopClip(requester, target)`. Inspect the returned enum rather
than assuming the request was accepted. ReplayCore applies the server's clip
availability, duration, cooldown, and capacity rules to API calls as well as
commands.

## Surface a recent kill replay

When death-cam is enabled, an integration can obtain the latest still-valid
replay offered for a player:

```java
import uk.co.forgevector.replaycore.api.plugin.KillReplay;

replayCore.killReplay().ifPresent(killReplays ->
        killReplays.latestKillReplay(player.getUniqueId()).ifPresent(replay -> {
            if (replay.valid(System.currentTimeMillis())) {
                player.sendMessage("Replay: " + replay.command());
            }
        }));
```

`KillReplay` in `v1.1.2` exposes `replayId()`, `command()`, `expiresAtMillis()`,
and `valid(nowMillis)`. The `1.2.0-SNAPSHOT` source also provides `webUrl()` and
`latestKillReplayWithWebUrl()` for browser-only links. Do not compile against
those additions until using a release that contains them. An empty result means
there is no current link to display.

## Compatibility

- Check the major component of `apiVersion()` before using the contract.
- Detect optional features with `clips()` and `killReplay()` on every enable.
- `Bookmark`, `RecordingService`, and the related legacy accessors remain for
  source compatibility but are deprecated. New integrations should use
  `IntegrationBookmark`, `ReplayCoreTimelineApi`, and `RecordingControlApi`.
- Compile and test against the oldest ReplayCore release your integration claims
  to support.
