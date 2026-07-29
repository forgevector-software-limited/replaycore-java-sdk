# Plugin extensions

The package `uk.co.forgevector.replaycore.api.plugin` is the supported Java
contract for plugins running on the same Minecraft server as ReplayCore. Contract
version `1.1` provides recording state, lifecycle events, timeline bookmarks,
on-demand clips, recent kill-replay links, match scopes and externally managed
player-shaped subjects.

Match scopes were added to the contract as an optional capability rather than a
version bump, so a recorder that offers them still reports `apiVersion()` as
`1.1`. Detect the surface with `matches()` or `externalSubjects()`, not with a
version comparison.

## Add the contract to your plugin

Use the SDK as a compile-only dependency when calling the in-process API. The
running ReplayCore plugin supplies these classes:

```groovy
dependencies {
    compileOnly 'com.github.forgevector-software-limited:replaycore-java-sdk:v1.6.0'
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
`clips()`, `killReplay()`, `matches()` and `externalSubjects()` return
`Optional` because those features depend on the recorder version and server
configuration.

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

### Address a marker or clip to one match

`tagTimelineEvent` writes onto the recording's own timeline, which carries one
tick track for the whole server. While a single match is in progress that is
unambiguous. With several open at once it is not: every match's markers land on
the same track, and `arenaId` does not separate them, because the recorder keeps
that field as descriptive metadata and never routes on it.

`tagScopeEvent` routes instead. The marker is written against the collection the
scope id names, and each match's timeline is read back by selecting on that
collection, so a marker raised for one match is not selected by another match's
query. `recordScopeClip` does the same for a playable clip of the moment, minting
one asset that carries a killer, victim and participant row rather than one clip
per viewer.

Both are `default` methods that complete exceptionally on a recorder that
predates them, so check once at startup rather than per event:

```java
import uk.co.forgevector.replaycore.api.plugin.EventKind;
import uk.co.forgevector.replaycore.api.plugin.ScopeClipRequest;

private boolean scopeEvents;

// on enable
replayCore.matches().ifPresent(matches -> scopeEvents = matches.supportsScopeEvents());

// on a kill, with the scope id held on the match object it belongs to
public void onKill(Game game, UUID killer, UUID victim) {
    if (!scopeEvents) {
        return;
    }
    String scopeId = scopeIdsByMatch.get(game.id());
    if (scopeId == null) {
        return;
    }
    replayCore.matches().ifPresent(matches -> matches.recordScopeClip(scopeId,
            ScopeClipRequest.builder(EventKind.KILL)
                    .preRollTicks(120L)   // 6s before the kill
                    .postRollTicks(60L)   // 3s after it
                    .killer(killer)
                    .victim(victim)
                    .build()));
}
```

The window is given as tick offsets around the moment of the call, never as an
absolute tick and never as a `Duration`: 20 ticks is one second on an unlagged
server, and ticks pause when the server is empty, so no wall-clock quantity is a
valid tick quantity. The recorder clamps toward less footage, never more, at the
scope's start, at an archive rotation and at the scope's end.

Routing is guaranteed; footage is not filtered. A clip is a tick window over the
one shared recording, so it renders everything captured in those ticks, including
an unrelated match running at the same time in the same world.

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

The fake reports `supportsScopeEvents()` as `true`, so an integration's capability
branch takes the same path in tests as against a current recorder.
`tagScopeEventCalls()` and `recordScopeClipCalls()` keep the scope id each call
named, which is what lets a test with two matches open assert that a marker or
clip was addressed to the right one.

## Record externally managed player-shaped subjects

`ReplayCoreExternalSubjectApi` is the Bukkit-free capture contract for a bot,
NPC, simulation actor or remote game engine that owns authoritative
player-shaped state. It does not invent movement or interpolate missing input.
The integration publishes the facts it owns and ReplayCore writes only accepted
frames and events.

The surface is optional. Resolve it once after ReplayCore enables:

```java
Optional<ReplayCoreExternalSubjectApi> available = replayCore.externalSubjects();
if (!available.isPresent()) {
    getLogger().info("This ReplayCore recorder does not expose external subjects.");
    return;
}
ReplayCoreExternalSubjectApi external = available.get();
```

### Open one logical external recording

`openScope` takes the integration's stable key, its own recording id and bounded
catalogue metadata. Repeating the same compound identity resolves the same
logical recording instead of creating a duplicate.

```java
ReplayScope scope = external.openScope(
        "practice-engine",
        match.id(),
        ReplayScopeOptions.builder("post-match")
                .category("practice")
                .mode(match.mode())
                .worlds(Collections.singletonList(match.worldName()))
                .metadata(Collections.singletonMap("queue", match.queueName()))
                .build());
```

A scope is a window over the continuous recorder. Opening or ending it never
starts, stops, rotates or cuts physical capture, and independent scopes may be
open concurrently.

### Register and publish an actor

Register each subject with a stable UUID, profile name, signed texture and
initial transform. The returned handle is opaque and valid only for that
subject, scope and recorder instance.

```java
SubjectTransform initial = SubjectTransform
        .builder(worldId, spawnX, spawnY, spawnZ)
        .rotation(yaw, pitch)
        .headYaw(headYaw)
        .onGround(true)
        .pose(SubjectPose.STANDING)
        .build();

ExternalSubjectDescriptor descriptor =
        ExternalSubjectDescriptor.playerShaped(
                actorId, actorName, signedTexture, initial);

ExternalSubjectHandle handle =
        external.registerExternalSubject(scope, descriptor);
```

Publish complete authoritative frames at replay ticks:

```java
ExternalSubjectFrame frame = ExternalSubjectFrame
        .builder(replayTick, transform)
        .metadata(metadata)
        .equipment(equipment)
        .effects(effects)
        .build();

ExternalSubjectPublishResult result =
        external.publishExternalSubjectFrame(handle, frame);

if (result == ExternalSubjectPublishResult.BACKPRESSURE) {
    slowThePublisher();
}
```

Publication is local, bounded and non-blocking. `ACCEPTED` means ReplayCore's
durable recorder boundary accepted the complete record. `BACKPRESSURE` means it
did not, so the integration can slow or retry without ReplayCore silently
dropping a previously accepted record. Other result values describe invalid,
closed or unavailable handles and must also be handled explicitly.

Use `publishExternalSubjectEvent` for discrete visual facts such as teleport,
equipment, animation and effect changes. Use
`updateExternalSubjectPresentation` for a versioned name, skin or visibility
change. Dedicated despawn, respawn and unregister calls maintain the same
canonical lifecycle state as their event equivalents.

### Finish and retrieve the durable result

End the logical recording with an idempotency key:

```java
external.endScope(scope, EndScopeRequest.builder("end:" + match.id()).build())
        .thenAccept(result ->
                getLogger().info("Replay finalising: " + result.state()));
```

The immediate result may still be recording or processing. Use the compound
integration identity for restart-safe lookup or waiting:

```java
external.awaitTerminalState(
        "practice-engine", match.id(), Duration.ofMinutes(10))
        .thenAccept(result -> {
            if (result.state() == ReplayState.PLAYABLE) {
                result.optionalWatchUrl().ifPresent(url ->
                        getLogger().info("Replay ready: " + url));
            } else if (result.state() == ReplayState.FAILED) {
                getLogger().warning("Replay failed: "
                        + result.optionalFailureCode().orElse("unknown"));
            }
        });
```

`ReplayResult` keeps local and cloud collection identities separate and exposes
the durable operation, asset, processing, playback and retry fields. Terminal
results are versioned and immutable, so retries cannot rewrite a previously
reported outcome.

## Read a player's replays and mint a watch link

`ReplayCatalogApi` reads the replay catalogue for one player and mints a
short-lived watch link, so a network can build its own browser and its own watch
button without a separate developer key on the server. The call is carried to the
cloud over the recorder's own recording key, so the addon never handles a bearer
key itself.

The catalogue is a registered service. Look it up on enable and keep it when it is
present.

```java
import uk.co.forgevector.replaycore.api.plugin.ReplayCatalogApi;
import uk.co.forgevector.replaycore.api.plugin.ReplayCatalogEntry;
import uk.co.forgevector.replaycore.api.plugin.ReplayCatalogQuery;
import uk.co.forgevector.replaycore.api.plugin.WatchTicketRequest;
import org.bukkit.plugin.RegisteredServiceProvider;

RegisteredServiceProvider<ReplayCatalogApi> registration =
        getServer().getServicesManager().getRegistration(ReplayCatalogApi.class);
if (registration == null) {
    return; // this recorder build or server config does not offer the catalogue
}
ReplayCatalogApi catalog = registration.getProvider();
```

`listForPlayer` returns that player's replays, newest first and already embargo
gated, one page at a time. Read a page, then follow `nextCursor()` until it is
absent. Pass a `ReplayCatalogQuery` with no facets set for an unfiltered first
page, or set filters on the builder to narrow by game type, clip type, match or
processing state.

```java
catalog.listForPlayer(playerUuid, ReplayCatalogQuery.builder().build())
        .thenAccept(page -> {
            for (ReplayCatalogEntry entry : page.entries()) {
                getLogger().info(entry.assetId() + " " + entry.kind());
            }
        });
```

`createWatchTicket` mints a single-use, short-lived ticket for one asset and one
viewer. Redeeming that ticket into playable bytes is a viewer-side REST call and
is not part of this interface, so this method only ever performs the mint.

```java
catalog.createWatchTicket(WatchTicketRequest.builder(assetId).build())
        .thenAccept(result -> {
            if (result.ready()) {
                // hand result.ticket() to your website's player
            }
        });
```

Both methods return a `CompletionStage` and never block the calling thread. A call
that is refused or fails completes the stage exceptionally rather than throwing
inline, so branch on the stage's outcome.

## Redact a hidden or disguised player

`RecordingControlApi.updateSubject` sets a per-subject capture-visibility override
live, for a privacy reason the recorder cannot detect on its own such as a
disguise, a moderation shadow, or a vanish provider outside the recorder's own
convention. Pass `CaptureVisibility.HIDDEN` or `CaptureVisibility.REDACTED` to
gate what is captured for that player, and `CaptureVisibility.VISIBLE` to clear
the override. The player's real id is always kept, so an erasure request still
resolves.

```java
import uk.co.forgevector.replaycore.api.plugin.CaptureVisibility;
import java.util.Collections;

replayCore.recordingControl().updateSubject(
        playerUuid, realName, disguiseName, CaptureVisibility.REDACTED,
        Collections.emptyMap());
```

`updateSubject` is a `default` method, so a recorder that predates it links fine
and reports the override as not applied. It changes capture only for the UUID it
names and cannot reach another player or another tenant. It works on the 1.8
legacy lane, the modern lane, and Folia.

## Compatibility

- Check the major component of `apiVersion()` before using the contract.
- Detect optional features with `clips()`, `killReplay()`, `matches()` and
  `externalSubjects()` on every enable. `matches()` and `externalSubjects()` are
  `default` methods returning an empty `Optional`, so an older recorder reports
  no surface rather than failing to link.
- Detect the scope-addressed event surface with `supportsScopeEvents()` once on
  enable. `tagScopeEvent` and `recordScopeClip` are `default` methods, so a
  recorder that predates them links fine and completes every such call
  exceptionally; branch on the capability rather than on a returned stage.
- Detect the replay catalogue with
  `getServicesManager().getRegistration(ReplayCatalogApi.class)` on enable. A
  recorder build or server config that does not offer it returns no registration
  rather than failing to link, so guard on a null registration.
- `Bookmark`, `RecordingService`, and the related legacy accessors remain for
  source compatibility but are deprecated. New integrations should use
  `IntegrationBookmark`, `ReplayCoreTimelineApi`, and `RecordingControlApi`.
- Compile and test against the oldest ReplayCore release your integration claims
  to support.
