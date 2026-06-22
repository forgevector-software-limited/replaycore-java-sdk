# Security model

This SDK is a public deliverable that talks to production ReplayCore systems on
behalf of server owners. It is designed so that using it can never become a path
into ReplayCore's systems or another customer's data. This document states the
model explicitly.

## Authentication: one tenant-scoped API key

The SDK authenticates every request with a single **API key**, supplied to the
builder and sent as `Authorization: Bearer rc_live_<...>`. There is no other
credential and no embedded secret anywhere in the SDK.

- Keys are issued from the ReplayCore panel and are bound, at creation, to exactly
  one tenant.
- ReplayCore stores only a SHA-256 hash of the key, never the key itself, and
  shows the plaintext only once at creation.
- On each request the server resolves the key to its tenant and **injects that
  tenant** into the request context. Every query the request makes is scoped to
  that tenant on the server side.

The SDK cannot change, widen, or spoof that tenant. There is no parameter,
header, or method that names a tenant; the key alone decides it.

## Tenant isolation

Because tenant scoping is enforced by the server from the resolved key:

- You can only list and read **your own** replays. A replay belonging to another
  tenant is not just hidden — it is indistinguishable from one that does not
  exist. `getReplay` on a foreign id returns `NotFoundException`
  (`REPLAY_NOT_FOUND`), exactly as for a non-existent id. This prevents probing
  for other tenants' data.
- You can only annotate replays in your own tenant. A timeline marker request
  cannot name a foreign tenant; the server resolves the target replay (or active
  recording) within your tenant only.

## Least privilege via scopes

A key carries a fixed set of scopes chosen at issue time:

- `replays:read` — list and read replay metadata.
- `replays:write` — add timeline markers.
- `servers:read`, `analytics:read` — reserved; no key-authed endpoint consumes
  them yet.

The SDK requests only the scope an endpoint needs. If a key lacks it, the call
fails with `AuthorizationException` (`INSUFFICIENT_SCOPE`); the SDK cannot work
around a missing scope. Issue read-only keys for read-only integrations.

## No privilege-escalation surface

The SDK wraps **only** the customer-API-key endpoints (`/v1/api/...`). It does
not expose, and provides no way to reach:

- the recorder's HMAC-signed plugin endpoints (server registration, uploads,
  exports, batch delete, webhooks, privacy operations);
- the panel's session-authenticated routes (account, billing, sharing, settings);
- any administrative endpoint (tenant management, licence management, support).

None of those accept a customer API key, so they are unreachable regardless — but
the SDK does not present them either, so it cannot mislead a caller into believing
they are available.

## Input validation

Request builders validate input before anything leaves the JVM, mirroring the
server's own validation: page size and duration bounds, mutually-exclusive
filters, label/category/actor lengths, and `#rrggbb` colour format. User-supplied
values placed into a URL are percent-encoded, so a filter value cannot inject
extra query parameters or alter the request path. This is defence in depth; the
server validates independently and is the authority.

## Handling the key safely

The key is the only secret involved. Treat it accordingly:

- **Never hard-code it.** Load it from an environment variable, a secrets manager,
  or your plugin's config file (kept out of version control).
- **Never commit it.** The project `.gitignore` excludes `.env` and
  `local.properties`; keep your key out of any tracked file.
- The SDK never logs, prints, or echoes the key, and sends it only over the
  configured base URL (HTTPS in production).
- If a key is exposed, revoke it in the panel and issue a new one. Revocation
  takes effect immediately — a revoked key resolves to the same `401
  INVALID_API_KEY` as an unknown one.

## Transport

The default base URL is `https://api.replaycore.com` (TLS). The builder accepts
an alternative base URL only for legitimate staging use and requires it to be
`http(s)`. The SDK does not follow redirects, so a request cannot be silently
re-pointed at another host.

## Rate limiting

ReplayCore rate-limits per tenant, separately for reads and writes. When a limit
is hit the SDK raises `RateLimitException`, exposing the server's advised back-off
via `getRetryAfter()`. Respect it rather than retrying immediately.
