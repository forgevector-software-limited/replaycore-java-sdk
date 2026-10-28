# API reference

This reference documents the public surface of the ReplayCore Java SDK and the
REST endpoints it wraps. It reflects the endpoints ReplayCore exposes to API-key
holders today; see [Endpoint coverage](#endpoint-coverage) for the current
boundary.

This page documents the `1.7.0` release. Documentation for earlier releases
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
| `createServer(ServerSetupRequest)` → `ServerSetup` | `POST /v1/api/servers` | `servers:write` |
| `renameServer(String id, String name)` → `ServerInstance` | `PATCH /v1/api/servers/{id}` | `servers:write` |
| `deauthoriseServer(String id)` | `DELETE /v1/api/servers/{id}` | `servers:write` |
| `permanentlyRemoveServer(String id)` | `DELETE /v1/api/servers/permanent/{id}` | `servers:write` |
| `cancelServerSetup(String id)` | `DELETE /v1/api/servers/pending/{id}` | `servers:write` |
| `getSetup(String path)` → `ApiResponse` | `GET /v1/api/setup/{path...}` | `setup:read` |
| `requestSetup(String method, String path, Object body)` → `ApiResponse` | `/v1/api/setup/{path...}` | `setup:read` or `setup:write` |
| `requestJson(String method, String path, Object body)` → `ApiResponse` | Any documented JSON `/v1/` endpoint | Endpoint-specific |
| `createTimelineMarker(TimelineEventRequest)` → `TimelineMarker` | `POST /v1/api/timeline-events` | `replays:write` |

### `ReplayCoreAsyncClient` (asynchronous)

Every synchronous operation above has an asynchronous equivalent returning a
`CompletableFuture`. Build via `builder().buildAsync()`, or wrap an existing
`ReplayCoreClient`. A failure
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
| `sessionId(String)` | `session_id` | The replay that represents one recording session; must be a UUID. Matches `getSessionId()`. |

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
| `getStorageMode()` / `isSegmented()` | `Optional<String>` / `boolean` | `storage_mode`; `isSegmented()` is true when it is `"segmented"`. |
| `getSessionId()` | `Optional<String>` | `session_id`. Always sent: the session id of a segmented replay, or the replay's own id otherwise. |
| `getSegmentSeq()` | `OptionalInt` | `segment_seq`. The 0-based segment number; present only on segmented replay rows. |
| `getSessionDurationMs()` | `Optional<Long>` | `session_duration_ms`. Length of the whole session; absent for a single-archive replay. |
| `getRecordingSessionId()` | `Optional<String>` | `recording_session_id`. The recorder's own session id, when sent. |
| `hasThumbnail()` | `boolean` | `has_thumbnail` |
| `hasPrivateThumbnail()` | `boolean` | `has_private_thumbnail` |

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
- **`ApiScope`**: `REPLAYS_READ`, `REPLAYS_WRITE`, `SERVERS_READ`,
  `SERVERS_WRITE`, `SETUP_READ`, `SETUP_WRITE`, `ANALYTICS_READ`,
  `PORTALS_READ`, `PORTALS_WRITE`, `RECORDINGS_WRITE`, `COLLECTIONS_WRITE`,
  `CLIPS_WRITE`, `CATALOG_READ`, `RELEASE_WRITE`, `PLAYBACK_ISSUE`,
  `STAFF_BYPASS`, and `ADMIN`.

### `Participant`

`getUuid()` (always present), `getName()` (`Optional<String>`), `getRole()`
(`Optional<String>`).

---

## Connected servers

`listServers()` returns an unmodifiable list of the Minecraft server instances
connected to the account. It requires `servers:read` and is not paginated.

`createServer`, `renameServer`, `deauthoriseServer`, `permanentlyRemoveServer`
and `cancelServerSetup` require `servers:write`. These calls are actor-bound:
the cloud attributes them to the user who created the key and rechecks that
user's current workspace permissions. A legacy key without that binding cannot
perform management writes.

### `ServerInstance`

| Accessor | Meaning |
| --- | --- |
| `getId()` | Stable ReplayCore server id. |
| `getName()` | Display name. |
| `getStatus()` | `ONLINE`, `OFFLINE`, or `UNKNOWN`. |
| `getLastSeenAt()` | Last report time, when available. |
| `getPluginVersion()` | Last reported ReplayCore plugin version, when available. |
| `getMinecraftVersion()` | Last reported Minecraft version, when available. |
| `getUpdate()` | `PluginUpdate` advice: `getStatus()` (`CURRENT`, `OUTDATED`, `UNSUPPORTED`, `MIGRATE_TO_LOADER`, `LOADER`, `UNKNOWN`), `getMinimumSupportedVersion()`, `getLatestClassicVersion()`, `isLoaderAvailable()`, `getLoaderDownloadUrl()`. |
| `getPlayerCount()` | Live player count; absent while offline or unavailable. |
| `getReplayCount()` | Number of replays recorded by this server instance. |

---

## Workspace setup automation

`getSetup(path)` and `requestSetup(method, path, body)` expose the same
allowlisted setup handlers used by the dashboard. The path is relative to
`/v1/api/setup/`, for example `capture-settings`, `team/members`, or
`team/members/{userId}`. The SDK rejects absolute URLs, traversal and fragments
before making a request.

The setup surface covers:

- capture, death-cam, retention, recording-key and recorder-credential settings;
- categories, grouping, replay visibility and sharing;
- integrations and managed plugin configuration;
- networks, Network Portals, custom domains, feeds and white-labelling;
- resource-pack metadata and URL-based fetching;
- governance and in-game viewing;
- team members, invitations, roles, delegated grants and permission groups.

Reads require `setup:read`; writes require `setup:write`. Only an active
workspace owner can grant `setup:write`. The acting user's live role,
permissions, tier limits and no-escalation rules still apply to every request.
Billing, API-key management, account deletion, replay/media payloads and
operator-only administration are deliberately excluded.

The capability document is available through
`getSetup("capabilities")`. It reports the current resource prefixes, methods,
OpenAPI location and documentation location so an integration can discover the
supported setup boundary.

### `ApiResponse`

Generic calls return the successful HTTP status plus a recursively immutable
JSON tree. Objects are `Map<String,Object>`, arrays are `List<Object>`, and
scalars use normal JDK values. Use `getObject()` when an object is expected,
`getBody()` for any JSON value, or `toJson()` for compact JSON. A successful
`204 No Content` response has no body.

`requestJson(method, path, body)` is a future-compatible escape hatch for other
documented JSON endpoints on the configured ReplayCore origin. It accepts only
absolute `/v1/` paths and never accepts an external URL, so it cannot redirect
the API key to another host.

`requestJson(method, path, body, headers)` adds request headers. It is meant for
`Idempotency-Key`, which makes a mutating call safe to retry, and `If-Match`,
which makes an update conditional on the version last read. `Authorization`,
`Host`, `User-Agent`, `Accept` and `Content-Type` belong to the client and are
refused in any letter case with `IllegalArgumentException`. Header names must be
HTTP tokens, may not repeat, and neither names nor values may contain CR, LF or
NUL. The three-argument form sends no extra headers.

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
| `icon(String)` | `icon` | One of `TimelineEventRequest.ICONS` (`swords`, `skull`, `shield`, `pickaxe`, `blast`, `flask`, `user-plus`, `bookmark`, `flag`, `sparkles`, `hammer`, `star`, `trophy`, `bed`, `diamond`); sent in lower case. |
| `actor(String)` | `actor` | ≤ 80 chars. |
| `integration(String)` | `integration` | 1–64 lowercase letters, digits, `-` or `_`. Lets the marker's visibility follow the integration's team-role rules. |

> Note: the timeline-event endpoint uses camelCase body fields (`replayId`,
> `serverId`, `createdAt`), unlike the snake_case replay endpoints. The SDK
> handles this for you; it is noted here only for transparency.

### `TimelineMarker` (response)

`getId()`, `getReplayId()`, `getTick()`, `getLabel()`, `getCategory()`,
`getColour()`, `getIcon()`, `getActor()`, `getIntegration()`,
`getCreatedAt()` (`Optional<Instant>`).

---

## Network integration: collections and assets

A **collection** is the logical window over a recording that an integration opens
when a match starts and finalises when it ends. An **asset** is a playable cut of
it: the full match (produced when the collection is finalised) or a clip or
highlight. Content starts **held**; its release policy, or an explicit call,
releases it.

### Idempotency and headers

Every mutating call on a collection or asset needs an idempotency key, which the
client sends as the `Idempotency-Key` header. You choose it (1–200 characters, no
CR, LF or NUL) and the SDK never generates one: a key generated per attempt would
defeat retrying. Request objects carry the key (`getIdempotencyKey()`), so a
request can be built once and sent again unchanged; `holdCollection`,
`releaseCollection`, `revokeCollection` and the two batch calls take it as an
argument. Sending the same key again returns the original outcome. None of these
routes read `If-Match`.

| Call | Route | Scope | `Idempotency-Key` |
| --- | --- | --- | --- |
| `createCollection(CollectionRequest)` | `POST /v1/replay-collections` | `collections:write` | Required, on the request. |
| `updateCollection(id, CollectionUpdateRequest)` | `POST /v1/replay-collections/{id}` | `collections:write` | Required, on the request. |
| `finalizeCollection(id, CollectionFinalizeRequest)` | `POST /v1/replay-collections/{id}/finalize` | `collections:write` | Required, on the request. |
| `holdCollection(id, key)` | `POST /v1/replay-collections/{id}/hold` | `release:write` | Required, argument. |
| `releaseCollection(id, key)` | `POST /v1/replay-collections/{id}/release` | `release:write` | Required, argument. |
| `revokeCollection(id, key)` | `POST /v1/replay-collections/{id}/revoke` | `release:write` | Required, argument. Permanent. |
| `createAsset(collectionId, AssetRequest)` | `POST /v1/replay-collections/{id}/assets` | `clips:write` | Required, on the request. |
| `finalizeAsset(id, AssetFinalizeRequest)` | `POST /v1/replay-assets/{id}/finalize` | `clips:write` | Required, on the request. |
| `createAssetsBatch(key, entries)` | `POST /v1/replay-assets/batch` | `clips:write` | Required, argument; each entry also has a client event id. |
| `createTimelineEventsBatch(key, entries)` | `POST /v1/timeline-events/batch` | `collections:write` | Required, argument; each entry also has a client event id. |
| `listCollections(CollectionQuery)` | `GET /v1/replay-collections` | `catalog:read` | None. |
| `getCollection(id)` / `getCollection(id, playerUuid)` | `GET /v1/replay-collections/{id}` | `catalog:read` | None. |
| `listCollectionAssets(id, AssetQuery)` | `GET /v1/replay-collections/{id}/assets` | `catalog:read` | None. |
| `listCollectionTimelineEvents(id)` | `GET /v1/replay-collections/{id}/timeline-events` | `catalog:read` | None. |
| `getCollectionFacets()` | `GET /v1/replay-collections/facets` | `catalog:read` | None. |
| `bulkLookupCollections(matchIds, collectionIds)` | `POST /v1/replay-collections/bulk-lookup` | `catalog:read` | None; a read, at most 100 entries. |
| `getAsset(id)` | `GET /v1/replay-assets/{id}` | `catalog:read` | None. |
| `listPlayerReplays(playerUuid, PlayerReplayQuery)` | `GET /v1/players/{uuid}/replays` | `catalog:read` | None. |
| `issueWatchTicket(assetId, viewerPlayerUuid)` | `POST /v1/replay-assets/{id}/watch-ticket` | `playback:issue` | None. |
| `redeemWatchTicket(assetId, ticket, viewerPlayerUuid)` | `POST /v1/replay-assets/{id}/watch-ticket/redeem` | `catalog:read` | None; the ticket authorises it. |
| `createWatchLink(assetId)` | `POST /v1/replay-assets/{id}/watch-link` | `playback:issue` | None; paid plans only. |
| `replayWebhookDelivery(deliveryId)` | `POST /v1/webhooks/deliveries/{id}/replay` | `collections:write` | None. |

Every call has an asynchronous twin on `ReplayCoreAsyncClient`. Failures use the
exceptions in [Errors](#errors); the stable `code` is on `getCode()`, for example
`MISSING_IDEMPOTENCY_KEY` and `OPERATION_IN_PROGRESS` (409, retry the same key),
`DELIVERY_NOT_REPLAYABLE` (409) and `FORBIDDEN_TIER` (403).

### Requests

| Type | Purpose |
| --- | --- |
| `CollectionRequest` | Open a collection: server, category, mode, release policy, optional roster, teams, spectators, competition fields, metadata and the first open segment. |
| `CollectionUpdateRequest` | Players who joined or left, new teams, merged metadata. |
| `CollectionFinalizeRequest` | End tick, final roster and teams, metadata and the full closed segment list. Moves the collection to `FINALIZING`. |
| `AssetRequest` | A clip or highlight (`EVENT_CLIP`, `CUSTOM_CLIP`, `HIGHLIGHT`) over a segment's tick window, with player relationships. `FULL_MATCH` is refused. |
| `AssetFinalizeRequest` | `ready(key)` or `failed(key, failureCode)`. |
| `ReleasePolicy` | When the full match and clips are released (`ReleaseTrigger`) and their `CollectionVisibility`. |
| `ScopeParticipant`, `ScopeTeam`, `ScopeSegment` | Roster, team and archive-segment entries. |
| `ScopeMarkerEntry`, `ScopeClipEntry` | Entries for the two batch calls, each with a client event id. |
| `CollectionQuery`, `AssetQuery`, `PlayerReplayQuery` | Catalogue filters with `pageSize` and `pageToken`; `CollectionQuery` also takes `customFacet(key, value)`. |

All builders validate bounds locally and throw `IllegalArgumentException`.
Collection and asset ticks are positions on the collection's timeline, not
wall-clock times.

### Responses

| Type | Meaning |
| --- | --- |
| `ReplayCollection` | A collection, with rosters and playback flags when read from the catalogue. |
| `ReplayAsset` | An asset, with relationships on a single read, or participants and a watch URL on a player listing. |
| `CatalogPage<T>` | One cursor-paginated page; `getNextPageToken()`, `hasNextPage()`, `getPageSize()`. |
| `ScopeOperation` | The outcome of an update, finalise, create-asset or finalise-asset call. |
| `ScopeMarker` | A collection timeline marker. |
| `CatalogFacets`, `FacetCount` | Per-dimension counts. |
| `BatchEntryResult` | One result per batch entry, in request order: `ACCEPTED`, `REJECTED` or `RETRYABLE`. The call itself succeeds even when entries are refused. |
| `WatchTicket` | A single-use ticket, or a not-ready notice (`isPreparing()`, `getRetryAfterSeconds()`) when the cloud answers HTTP 202. |
| `WatchTicketRedemption`, `RedeemedSegment` | The segments to play, each with a short-lived URL. Treat the URLs as credentials. |
| `WatchLink` | A durable browser watch link. |
| `WebhookDeliveryReplay` | The delivery status after a replay. |

Enums (`ProcessingState`, `ReleaseState`, `CollectionVisibility`, `AssetKind`,
`EventKind`, `AssetRelation`, `OperationStatus`, `BatchEntryStatus`,
`ReleaseTrigger`) have an `UNKNOWN` constant for values added by a later release.

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
- List and manage connected or pending servers: `/v1/api/servers`.
- Read and change allowlisted workspace configuration:
  `/v1/api/setup/{path...}`.
- Create a timeline marker: `POST /v1/api/timeline-events`.
- Open, update, finalise, hold, release and revoke replay collections, create and
  finalise their assets, and write batches of markers and clips:
  `/v1/replay-collections`, `/v1/replay-assets` and `/v1/timeline-events/batch`.
- Read the collection catalogue, a collection's assets and markers, facets, bulk
  lookups and a player's replays.
- Issue and redeem watch tickets, mint watch links and replay webhook deliveries.

**Not yet available to API-key holders** (intentionally not exposed by this SDK):

- Signed replay **downloads**. The current public API does not provide an
  API-key-authenticated download method.
- **Analytics**. `analytics:read` is reserved, but the current public API does not
  provide an analytics endpoint.

The SDK wraps only documented, API-key-authenticated contracts. The setup
facade intentionally reuses dashboard validation while preserving tenant and
actor identity; recorder credentials, browser sessions and operator-only
administrative routes remain separate access models.

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
