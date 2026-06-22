# API reference

This reference documents the public surface of the ReplayCore Java SDK and the
REST endpoints it wraps. It reflects the endpoints ReplayCore exposes to API-key
holders today; see [Coverage and roadmap](#coverage-and-roadmap) for what is and
is not yet available.

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
| `createTimelineMarker(TimelineEventRequest)` → `TimelineMarker` | `POST /v1/api/timeline-events` | `replays:write` |

### `ReplayCoreAsyncClient` (asynchronous)

The same three methods, each returning a `CompletableFuture`. Build via
`builder().buildAsync()`, or wrap an existing `ReplayCoreClient`. A failure
completes the future exceptionally with the same `ReplayCoreException` types.
`blocking()` returns the underlying synchronous client.

### `ReplayCoreClientBuilder`

| Setting | Default | Notes |
| --- | --- | --- |
| `apiKey(String)` | — (required) | Must carry the `rc_live_` prefix. |
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
| `getTenantId()` | `Optional<String>` | `tenant_id` |
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
the `staging-unsigned` sentinel) and its manifest hash is set — i.e. the replay
is finalised and watchable.

### Enums

All enums map an unrecognised future wire value to an `UNKNOWN` constant rather
than failing to deserialise.

- **`Quality`** — `STANDARD` (`standard`), `HD` (`hd`).
- **`StorageTier`** — `HOT` (`hot`), `R2_INFREQUENT_ACCESS` (`r2_ia`).
- **`Visibility`** — `STAFF` (`staff`, legacy ≡ private), `PRIVATE` (`private`),
  `UNLISTED` (`unlisted`), `PUBLIC` (`public`).
- **`ArchiveStatus`** — `ORIGINAL` (`original`), `REDACTED` (`redacted`),
  `CRASH_FINALISED` (`crash-finalised`).
- **`ApiScope`** — `REPLAYS_READ`, `REPLAYS_WRITE`, `SERVERS_READ` (reserved),
  `ANALYTICS_READ` (reserved).

### `Participant`

`getUuid()` (always present), `getName()` (`Optional<String>`), `getRole()`
(`Optional<String>`).

---

## Timeline markers

`createTimelineMarker(TimelineEventRequest)` pins a named moment on a replay's
timeline. Requires the `replays:write` scope.

### `TimelineEventRequest`

Start from one of two targets — supply exactly one:

- `TimelineEventRequest.forReplay(replayId)` — pin to an existing replay.
- `TimelineEventRequest.forActiveRecording(serverId)` — mark the server's
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

## Coverage and roadmap

**Wired today** (real, key-authed endpoints):

- List/search replays — `GET /v1/api/replays`.
- Get one replay's metadata — `GET /v1/api/replays/{id}`.
- Create a timeline marker — `POST /v1/api/timeline-events`.

**Not yet available to API-key holders** (intentionally not exposed by this SDK):

- Signed replay **downloads**. Download URL signing exists in the platform but is
  served only to the recorder plugin (HMAC-authenticated) and the panel session,
  not to customer API keys. The SDK will add `getDownloadUrl(...)` when a
  key-authed download endpoint ships.
- **Server listing** and **analytics**. The `servers:read` and `analytics:read`
  scopes are defined and grantable, but no key-authed endpoint consumes them yet.
  The SDK reserves the scopes (`ApiScope.SERVERS_READ`, `ApiScope.ANALYTICS_READ`)
  so a key minted for them maps cleanly once those endpoints land.

The SDK deliberately does **not** wrap the recorder's HMAC endpoints, the panel's
session-authenticated routes, or any administrative endpoint — none of those are
reachable with a customer API key, and exposing them would be misleading.
