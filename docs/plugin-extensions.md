# Plugin extension contract

Alongside the REST client, the SDK ships an **in-process extension contract** in
the package `uk.co.forgevector.replaycore.api.plugin`. It lets a companion plugin,
running on the same Bukkit/Spigot/Paper/Folia server as the ReplayCore recorder,
observe the recording lifecycle and add live timeline bookmarks — from inside the
game tick, rather than over HTTP.

## Status

This is a **forward-looking contract**. The interfaces describe the supported way
to integrate in process and are grounded in the recorder's real structure:

- The recorder already registers services through the platform's
  `ServicesManager` (the same mechanism it uses for its existing services), which
  is the discovery pattern `ReplayCoreProvider` follows.
- The recorder already maintains an internal capture sink that can place a
  bookmark on the active recording and report whether recording is live and at
  which tick — the capability `RecordingService` projects.
- The recorder drives a clear session lifecycle (start, rotate, stop) — the
  boundaries `RecordingListener` reports.

Which parts are live depends on the recorder version installed on a given server.
Always discover availability at runtime (below) and degrade gracefully when the
recorder is absent or older.

This contract is intentionally **annotation-only**. An addon can observe sessions
and enrich a recording the host already chose to make; it cannot start, stop,
download, delete, or read the bytes of a recording, and it cannot reach another
tenant. Those operations stay with the server's capture policy and the
authenticated REST surface, which keeps the in-process surface free of any
privilege-escalation path.

## Discovering the API

```java
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreApi;
import uk.co.forgevector.replaycore.api.plugin.ReplayCoreProvider;

Optional<ReplayCoreApi> maybe = ReplayCoreProvider.get();
if (!maybe.isPresent()) {
    getLogger().warning("ReplayCore not present; integration features disabled.");
    return;
}
ReplayCoreApi api = maybe.get();
```

Resolve the API after ReplayCore has enabled — for example order your plugin
after `ReplayCore` (a `softdepend`) and resolve on your own enable. Check
`api.apiVersion()` before using newer capabilities.

## Observing the recording lifecycle

```java
import uk.co.forgevector.replaycore.api.plugin.RecordingListener;
import uk.co.forgevector.replaycore.api.plugin.RecordingSession;

api.registerListener(new RecordingListener() {
    @Override
    public void onRecordingStarted(RecordingSession session) {
        getLogger().info("recording started: " + session.sessionId());
    }

    @Override
    public void onRecordingStopped(RecordingSession session) {
        // The replay now enters cloud finalisation. Its metadata becomes
        // available through the REST client once ReplayMetadata.isReady() is true.
        getLogger().info("recording stopped: " + session.sessionId());
    }
});
```

Callbacks fire on the server's main thread, so keep them quick and non-blocking.
Offload anything heavy — including REST calls — to another thread.

`RecordingSession` is a read-only snapshot: `sessionId()`, `serverId()`,
`integration()`, `startedAt()`. The `sessionId()` matches the RFC-0006
`session_id` that segmented replays expose through the REST API, so it is your
join key between an in-process session and the replay metadata you later fetch.

## Adding live bookmarks

```java
import uk.co.forgevector.replaycore.api.plugin.Bookmark;
import uk.co.forgevector.replaycore.api.plugin.RecordingService;

RecordingService recording = api.recordingService();

if (recording.isRecording()) {
    boolean accepted = recording.addBookmark(
            Bookmark.builder("Final Kill")
                    .category("combat")
                    .colour("#ff5555")
                    .build());
    // accepted == false if the recorder dropped it (not recording, budget hit,
    // or capture policy disallowed it). It never throws for an ordinary drop.
}
```

`RecordingService` also exposes `currentTick()` and `currentSession()`, so you
can correlate addon state with the recording timeline, or remember a tick to pin
a marker against later through the REST client.

## Relationship to the REST timeline-event endpoint

`RecordingService.addBookmark(...)` is the in-process counterpart of the REST
`createTimelineMarker(...)`: same idea (mark a named moment), but emitted from
within the tick loop against whatever recording is live now, with no network round
trip. Use the in-process path for live, low-latency tagging on the recording host;
use the REST path from an external service, or to annotate an existing replay
after the fact.

## Compatibility

`ReplayCoreApi.apiVersion()` returns a `major.minor` string. The major component
changes only on an incompatible change; check it before relying on capabilities
added after `1.0`. Because availability depends on the recorder build, treat every
in-process call as best-effort and guard with `ReplayCoreProvider.get()`.
