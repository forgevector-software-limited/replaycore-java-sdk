# API reference

This reference documents the public surface of the ReplayCore Java SDK and the
REST endpoints it wraps. It reflects the endpoints ReplayCore exposes to API-key
holders today; see [Endpoint coverage](#endpoint-coverage) for the current
boundary.

This page documents the `1.3.1` release. Documentation for earlier releases
remains available from each release's Git tag.

The generated Javadoc is the authoritative, method-level reference. Build it with
`./gradlew javadoc` and open `build/docs/javadoc/index.html`.

---

## Clients

### `ReplayCoreClient` (synchronous)

Configured through `ReplayCoreClient.builder()`. Immutable and thread-safe.

| Method | Wraps | Required scope |
| --- | --- | --- |
| `listReplays(ReplayQuery)` → `ReplayPage` | `GET /v1/api/replays` | `replays:read` |
| `getReplay(String id)` → `ReplayMetadata` | `GET /v1/api/replays/{id}` | `replays:read` |
| `listServers()` → `List<ServerInstance>` | `GET /v1/api/servers` | `servers:read` |
| `createTimelineMarker(TimelineEventRequest)` → `TimelineMarker` | `POST /v1/api/timeline-events` | `replays:write` |

### `ReplayCoreAsyncClient` (asynchronous)

The same four methods, each returning a `CompletableFuture`. Build via
`builder().buildAsync()`, or wrap an existing `ReplayCoreClient`. A failure
completes the future exceptionally with the same `ReplayCoreException` types.
`blocking()` returns the underlying synchronous client.

### `ReplayCoreClientBuilder`

| Setting | Default | Notes |
| --- | --- | --- |
| `apiKey(String)` | n/a (required) | Must carry the `rc_live_` prefix. |
| `baseUrl(String)` | `https://api.replaycore.com` | Override for staging. Must be http(s). |
| `connectTimeout(Duration)` | 10s | `Duration.ZERO` means no timeout. |
| `readTimeout(Duration)` | 30s | `Duration.ZERO` means no timeout. |
| `userAgent(String)` | `replaycore-java-sdk/<version>` | Append your app name to be identifiable. |
| `transport(HttpTransport)` | JDK `HttpURLConnection` | Mainly for testing. |

---

## Listing replays

`listReplays(ReplayQuery)` returns one page, newest first (by start time
descending). Results are cursor-paginated.

### `ReplayQuery`

Build with `ReplayQuery.builder()`. All filters are optional and AND-combined.
The builder validates bounds locally, matching the server's own checks.

| Builder method | Query param | Meaning |
| --- | --- | --- |
| `pageSize(int)` | `page_size` | 1–100, default 20. |
| `pageToken(String)` | `page_token` | Opaque cursor from a prior page. |
| `search(String)` | `q` | Case-insensitive substring over server name, game mode, server id. |
| `serverId(String)` | `server` | Exact server id. |
| `gameMode(String)` | `game_mode` | Exact (case-insensitive) game mode / integration. |
| `flagged(boolean)` | `flagged` | Replays with an open flag, or without. |
| `starred(boolean)` | `starred` | Starred replays, or not. |
| `playerUuid(String)` | `player_uuid` | Exact participant UUID. Exclusive with `player`. |
| `player(String)` | `player` | Case-insensitive participant-name substring. Exclusive with `playerUuid`. |
| `startedAfter(Instant)` | `started_after` | Lower time bound (inclusive). |
| `startedBefore(Instant)` | `started_before` | Upper time bound (exclusive). |
| `durationMinMs(long)` | `duration_min_ms` | 0–86 400 000 ms. |
| `durationMaxMs(long)` | `duration_max_ms` | 0–86 400 000 ms. |

Cross-field rules enforced at `build()`: `player`/`playerUuid` are mutually
exclusive; `startedAfter` must be before `startedBefore`; `durationMinMs` must
not exceed `durationMaxMs`.

### `ReplayPage`

| Accessor | Meaning |
| --- | --- |
| `getResults()` → `List<ReplayMetadata>` | The replays on this page (unmodifiable). |
| `hasNextPage()` → `boolean` | Whether a further page exists. |
| `getNextPageToken()` → `Optional<String>` | Cursor for the next page. |
| `getPageSize()` → `int` | Page size the server applied. |

Fetch the next page with `ReplayQuery.nextPageOf(page).build()`.

---

## Replay metadata

`ReplayMetadata` is an immutable view of a replay. Nullable fields are exposed as
`Optional`. Field names mirror the REST response.

| Accessor | Type | Wire field |
| --- | --- | --- |
| `getId()` | `String` | `id` |
| `getTenantId()` | `Optional<String>` | Owning account id (`tenant_id` on the wire). |
| `getServerId()` | `Optional<String>` | `server_id` |
| `getServerName()` | `Optional<String>` | `server_name` |
| `getDisplayName()` | `Optional<String>` | `display_name` |
| `getIntegration()` | `Optional<String>` | `integration` |
| `getQuality()` | `Quality` | `quality` |
| `getStartedAt()` | `Optional<Instant>` | `started_at` |
| `getEndedAt()` | `Optional<Instant>` | `ended_at` |
| `getDurationMs()` / `getDuration()` | `Optional<Long>` / `Optional<Duration>` | `duration_ms` |
| `getSizeBytes()` | `Optional<Long>` | `size_bytes` |
| `getStorageTier()` | `StorageTier` | `storage_tier` |
| `getRetentionUntil()` | `Optional<Instant>` | `retention_until` |
| `getVisibility()` | `Visibility` | `visibility` |
| `getSignatureKid()` | `Optional<String>` | `signature_kid` |
| `getManifestHash()` | `Optional<String>` | `manifest_hash` |
| `hasStaffArchive()` | `boolean` | `has_staff_archive` |
| `getRedactedFrom()` | `Optional<String>` | `redacted_from` |
| `getArchiveStatus()` | `ArchiveStatus` | `archive_status` |
| `isCrashFinalised()` | `boolean` | `crash_finalised` |
| `isStarred()` | `boolean` | `starred` |
| `getRecoveryStatus()` | `Optional<String>` | `recovery_status` |
| `getFormatVersion()` | `int` | `format_version` |
| `getParticipants()` | `List<Participant>` | `participants` |
| `getStorageMode()` / `isSegmented()` | `Optional<String>` / `boolean` | `storage_mode` |
| `getSessionId()` | `Optional<String>` | `session_id` |

### `isReady()`

The cloud has no single "status" field. `isReady()` returns `true` when the
replay has been signed with a real key (its signing key id is present and is not
the `staging-unsigned` sentinel) and its manifest hash is set, i.e. the replay
is finalised and watchable.

### Enums

All enums map an unrecognised future wire value to an `UNKNOWN` constant rather
than failing to deserialise.

- **`Quality`**: `STANDARD` (`standard`), `HD` (`hd`).
- **`StorageTier`**: `HOT` (`hot`), `R2_INFREQUENT_ACCESS` (`r2_ia`).
- **`Visibility`**: `STAFF` (`staff`, legacy ≡ private), `PRIVATE` (`private`),
  `UNLISTED` (`unlisted`), `PUBLIC` (`public`).
- **`ArchiveStatus`**: `ORIGINAL` (`original`), `REDACTED` (`redacted`),
  `CRASH_FINALISED` (`crash-finalised`).
- **`ApiScope`**: `REPLAYS_READ`, `REPLAYS_WRITE`, `SERVERS_READ`, and
  `ANALYTICS_READ` (reserved).

### `Participant`

`getUuid()` (always present), `getName()` (`Optional<String>`), `getRole()`
(`Optional<String>`).

---

## Connected servers

`listServers()` returns an unmodifiable list of the Minecraft server instances
connected to the account. It requires `servers:read` and is not paginated.

### `ServerInstance`

| Accessor | Meaning |
| --- | --- |
| `getId()` | Stable ReplayCore server id. |
| `getName()` | Display name. |
| `getStatus()` | `ONLINE`, `OFFLINE`, or `UNKNOWN`. |
| `getLastSeenAt()` | Last report time, when available. |
| `getPluginVersion()` | Last reported ReplayCore plugin version, when available. |
| `getPlayerCount()` | Live player count; absent while offline or unavailable. |
| `getReplayCount()` | Number of replays recorded by this server instance. |

---

## Timeline markers

`createTimelineMarker(TimelineEventRequest)` pins a named moment on a replay's
timeline. Requires the `replays:write` scope.

### `TimelineEventRequest`

Start from one of two targets (supply exactly one):

- `TimelineEventRequest.forReplay(replayId)`: pin to an existing replay.
- `TimelineEventRequest.forActiveRecording(serverId)`: mark the server's
  currently active recording (the cloud resolves the concrete replay).

| Builder method | Body field | Constraint |
| --- | --- | --- |
| `tick(long)` | `tick` | Required, non-negative. |
| `label(String)` | `label` | Required, 1–120 chars. |
| `category(String)` | `category` | ≤ 60 chars. |
| `colour(String)` | `colour` | `#rrggbb`. |
| `actor(String)` | `actor` | ≤ 80 chars. |

> Note: the timeline-event endpoint uses camelCase body fields (`replayId`,
> `serverId`, `createdAt`), unlike the snake_case replay endpoints. The SDK
> handles this for you; it is noted here only for transparency.

### `TimelineMarker` (response)

`getId()`, `getReplayId()`, `getTick()`, `getLabel()`, `getCategory()`,
`getColour()`, `getActor()`, `getCreatedAt()` (`Optional<Instant>`).

---

## Errors

All failures extend `ReplayCoreException` (checked).

| Exception | Trigger | Notable accessors |
| --- | --- | --- |
| `AuthenticationException` | HTTP 401 | `getCode()`, `getStatusCode()` |
| `AuthorizationException` | HTTP 403 (missing scope) | `getCode()` |
| `NotFoundException` | HTTP 404 | `getCode()` |
| `RateLimitException` | HTTP 429 | `getRetryAfter()` → `Duration` |
| `ReplayCoreApiException` | any other 4xx/5xx | `getStatusCode()`, `getCode()`, `getDetail()` |
| `ReplayCoreTransportException` | could not reach server / unparseable body | `getCause()` |

ReplayCore returns errors as RFC 9457 `application/problem+json`. The SDK parses
the stable `code` field into `getCode()`; prefer branching on it (or the HTTP
status) over the human-readable `detail`.

---

## Endpoint coverage

**Wired today** (real, key-authed endpoints):

- List/search replays: `GET /v1/api/replays`.
- Get one replay's metadata: `GET /v1/api/replays/{id}`.
- List connected servers: `GET /v1/api/servers`.
- Create a timeline marker: `POST /v1/api/timeline-events`.

**Not yet available to API-key holders** (intentionally not exposed by this SDK):

- Signed replay **downloads**. The current public API does not provide an
  API-key-authenticated download method.
- **Analytics**. `analytics:read` is reserved, but the current public API does not
  provide an analytics endpoint.

The SDK deliberately wraps only the documented public developer API. Dashboard,
recorder, and administrative routes use separate access models and are outside
this SDK's contract.

---

## In-process plugin contract

The package `uk.co.forgevector.replaycore.api.plugin` is a separate, non-REST
surface for plugins running on the same server as the recorder. It carries no
API key and makes no HTTP call; the running ReplayCore plugin supplies the
implementations.

| Surface | Availability | Purpose |
| --- | --- | --- |
| `ReplayCoreTimelineApi` | Always | Tag a custom event onto the live recording. |
| `RecordingControlApi` | Always | Read whether recording is live, the current tick, and the active session. |
| `ReplayCoreClipApi` | `Optional` | Save an on-demand clip. |
| `KillReplayApi` | `Optional` | Resolve a player's most recent death replay. |
| `ReplayCoreMatchApi` | `Optional` | Open, update and end a logical match scope over the continuous recording. |

`ReplayCoreMatchApi` is how a server that does not restart between games
publishes one replay per game: `beginScope` and `endScope` mark a tick-window on
the recording without starting, stopping or cutting it, so scopes can overlap and
death cams keep working throughout. See
[Plugin extensions](plugin-extensions.md) for the worked example, and
`FakeReplayCoreMatchApi` for testing an integration without a recorder.
