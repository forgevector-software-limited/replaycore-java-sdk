# Plugin extensions

The package `uk.co.forgevector.replaycore.api.plugin` is the supported Java
contract for plugins running on the same Minecraft server as ReplayCore. Contract
version `1.1` provides recording state, lifecycle events, timeline bookmarks,
on-demand clips, recent kill-replay links, and match scopes.

Match scopes were added to the contract as an optional capability rather than a
version bump, so a recorder that offers them still reports `apiVersion()` as
`1.1`. Detect the surface with `matches()`, not with a version comparison.

## Add the contract to your plugin

Use the SDK as a compile-only dependency when calling the in-process API. The
running ReplayCore plugin supplies these classes:

```groovy
dependencies {
    compileOnly 'com.github.forgevector-software-limited:replaycore-java-sdk:v1.2.0'
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
`clips()`, `killReplay()` and `matches()` return `Optional` because those
features depend on the server's ReplayCore configuration.

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

`KillReplay` exposes `replayId()`, `command()`, `expiresAtMillis()`, and
`valid(nowMillis)`. Since `v1.2.0` it also provides `webUrl()`, paired with
`latestKillReplayWithWebUrl()`, for a browser link where no in-game relay
command is available. A value from `latestKillReplay()` always has a non-blank
command; one from `latestKillReplayWithWebUrl()` may be web-only, in which case
`command()` is the empty string. An empty result means there is no current link
to display.

Death cams work the same way inside a match scope as outside one, because a
scope never interrupts the recording the death replay is cut from.

## Mark match boundaries with scopes

A game-mode server usually does not restart between games. One arena backend
runs match after match, often several at once, while ReplayCore records the
server continuously. `ReplayCoreMatchApi` marks where each game begins and ends
on that one recording, so each game is published as its own replay.

A scope is a tick-window over the recording, not a recording control. Opening or
ending one never starts, stops, rotates or cuts the physical recording. Two
consequences matter in practice:

- Several scopes can be open at once, so concurrent arenas on one backend each
  get their own replay.
- Everything that depends on continuous capture keeps working inside a scope.
  Kill and death cams are unaffected: the recorder still holds the rolling
  history a death replay is cut from, so `killReplay()` resolves during and
  after a match exactly as it does without scopes.

Every call is safe from the main thread. Nothing blocks on cloud I/O, and
ordinary operational failures arrive through the returned `CompletionStage`
rather than as a thrown exception, so ReplayCore being unavailable cannot stall
or cancel a match.

### Open a scope when a game starts

`beginScope` takes an idempotency key, your own match id, a category, a mode,
and the id of the release policy to apply. Re-sending the same idempotency key
returns the original scope instead of opening a second one, so a retry after a
timeout is safe.

```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import uk.co.forgevector.replaycore.api.plugin.BeginScopeRequest;
import uk.co.forgevector.replaycore.api.plugin.ParticipantRole;
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreMatchApi;
import uk.co.forgevector.replaycore.api.plugin.ReplayParticipant;

private final Map<String, String> scopeIdsByMatch = new ConcurrentHashMap<String, String>();

void onGameStart(Game game) {
    replayCore.matches().ifPresent(matches -> {
        List<ReplayParticipant> players = new ArrayList<ReplayParticipant>();
        for (Player player : game.players()) {
            players.add(ReplayParticipant.builder(player.getUniqueId(), player.getName())
                    .teamId(game.teamIdOf(player))
                    .role(ParticipantRole.PARTICIPANT)
                    .build());
        }

        matches.beginScope(BeginScopeRequest.builder(
                        "begin:" + game.id(),   // idempotency key
                        game.id(),              // your own match id
                        "duels",                // category
                        game.mode(),            // mode within the category
                        "post-match")           // release policy id
                        .worlds(Collections.singletonList(game.worldName()))
                        .participants(players)
                        .build())
                .thenAccept(scope -> scopeIdsByMatch.put(game.id(), scope.scopeId()))
                .exceptionally(error -> {
                    getLogger().warning("ReplayCore scope not opened for "
                            + game.id() + ": " + error.getMessage());
                    return null;
                });
    });
}
```

If the scope never opens, the match carries on and the recording is untouched;
only the per-game replay is missing.

### Update a scope while the game runs

Use `updateScope` for players joining or leaving mid-game, team snapshots, or
metadata to merge:

```java
import uk.co.forgevector.replaycore.api.plugin.ScopeUpdate;

void onSpectatorJoin(Game game, Player watcher) {
    String scopeId = scopeIdsByMatch.get(game.id());
    if (scopeId == null) {
        return;
    }

    replayCore.matches().ifPresent(matches -> matches.updateScope(scopeId,
            ScopeUpdate.builder()
                    .participantsJoined(Collections.singletonList(
                            ReplayParticipant.builder(watcher.getUniqueId(), watcher.getName())
                                    .role(ParticipantRole.SPECTATOR)
                                    .build()))
                    .build()));
}
```

### End the scope when the game finishes

```java
import java.util.Arrays;
import uk.co.forgevector.replaycore.api.plugin.EndScopeRequest;
import uk.co.forgevector.replaycore.api.plugin.ProcessingState;
import uk.co.forgevector.replaycore.api.plugin.ReplayTeam;

void onGameEnd(Game game, String winningTeamId, String losingTeamId) {
    String scopeId = scopeIdsByMatch.remove(game.id());
    if (scopeId == null) {
        return;
    }

    replayCore.matches().ifPresent(matches -> matches.endScope(scopeId,
            EndScopeRequest.builder("end:" + game.id())
                    .teams(Arrays.asList(
                            ReplayTeam.builder(winningTeamId).result("won").placement(1).build(),
                            ReplayTeam.builder(losingTeamId).result("lost").placement(2).build()))
                    .metadata("duration_seconds", Long.toString(game.durationSeconds()))
                    .build())
            .thenAccept(result -> {
                if (result.processingState() == ProcessingState.READY) {
                    result.watchUrl().ifPresent(url -> game.broadcast("Replay: " + url));
                }
            })
            .exceptionally(error -> {
                getLogger().warning("ReplayCore scope not ended for "
                        + game.id() + ": " + error.getMessage());
                return null;
            }));
}
```

The `FinalizeResult` handed to `thenAccept` reflects what is known the moment
the call returns, which is often still an in-progress `ProcessingState` such as
`QUEUED` or `UPLOADING` rather than `READY`. Treat it as an acknowledgement, not
as the finished replay.

### Receive the eventual outcome

The replay usually becomes playable after `endScope` has returned, and possibly
after the server has restarted. `RecordingListener` delivers that outcome:

```java
import uk.co.forgevector.replaycore.api.plugin.ReplayOperationResult;

replayCore.registerListener(new RecordingListener() {
    @Override
    public void onAssetReady(ReplayOperationResult result) {
        result.watchUrl().ifPresent(url ->
                getLogger().info("Replay ready for " + result.collectionId() + ": " + url));
    }

    @Override
    public void onAssetFailed(ReplayOperationResult result) {
        getLogger().warning("Replay failed for " + result.collectionId() + ": "
                + result.failureCode().orElse("unknown")
                + (result.retryable() ? " (will be retried)" : ""));
    }
});
```

Both callbacks are `default` no-ops, so an existing `RecordingListener` keeps
compiling and linking unchanged.

### Test without a server

`FakeReplayCoreMatchApi` is an in-memory implementation shipped in the main
source tree, so an integration can be tested with no recorder and no cloud
backend. It records every call for assertions, and leaves the `endScope` stage
pending until the test drives it with `completeReady`, `completeFailed` or
`completeFinalize`, which also fires the matching listener callback.

```java
FakeReplayCoreMatchApi fake = new FakeReplayCoreMatchApi();

ReplayScope scope = fake.beginScope(BeginScopeRequest.builder(
                "begin:m1", "m1", "duels", "ranked-1v1", "post-match").build())
        .toCompletableFuture().join();

CompletionStage<FinalizeResult> finalizing =
        fake.endScope(scope.scopeId(), EndScopeRequest.builder("end:m1").build());

fake.completeReady(scope.scopeId(), "asset-1",
        "https://api.replaycore.com/replay-assets/asset-1", null);

assertEquals(1, fake.beginScopeCalls().size());
assertTrue(finalizing.toCompletableFuture().isDone());
```

## Compatibility

- Check the major component of `apiVersion()` before using the contract.
- Detect optional features with `clips()`, `killReplay()` and `matches()` on
  every enable. `matches()` is a `default` method returning an empty `Optional`,
  so an older recorder reports no match surface rather than failing to link.
- `Bookmark`, `RecordingService`, and the related legacy accessors remain for
  source compatibility but are deprecated. New integrations should use
  `IntegrationBookmark`, `ReplayCoreTimelineApi`, and `RecordingControlApi`.
- Compile and test against the oldest ReplayCore release your integration claims
  to support.
