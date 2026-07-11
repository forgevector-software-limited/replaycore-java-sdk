# Security

The SDK uses a ReplayCore API key for remote developer API calls. Treat that key
like a password for the scopes granted to it.

## Create narrow keys

Create a separate key for each integration and grant only the scopes it uses:

- `replays:read` lists and reads replay metadata.
- `replays:write` adds timeline markers.
- `servers:read` lists connected server instances.
- `analytics:read` is reserved and is not required by the current SDK.

Revoke a key from the ReplayCore dashboard when an integration is retired or a
credential may have been exposed. Use different keys for development and
production.

## Store keys safely

- Load keys from an environment variable, secret store, or protected server
  configuration. Do not hard-code them in Java source.
- Keep local configuration out of version control.
- Do not include keys in logs, exception reports, screenshots, support messages,
  command output, or test fixtures.
- Avoid logging HTTP request headers. The `Authorization` header contains the
  complete key.

ReplayCore shows a newly created key once. Store it at creation time. If it is
lost, revoke it and create another rather than trying to recover it.

## Account boundaries

The API key determines which ReplayCore account and scopes a request may use.
The SDK does not provide a tenant override or an administrative client. A request
for data outside the key's account is not returned to the caller.

Access control remains a server-side responsibility. Client-side validation is
provided for earlier, clearer errors and must not be treated as an authorisation
boundary.

## Transport

The default API origin is `https://api.replaycore.com`. Requests do not follow
redirects.

`ReplayCoreClientBuilder.baseUrl(...)` exists for controlled development and
staging environments. The API key is sent to the configured origin, so never
point a real key at an origin you do not operate and trust. Use HTTPS for every
remote environment.

## Failures and retries

Remote failures are returned through the checked `ReplayCoreException` hierarchy.
Error details are suitable for diagnosis but may contain customer-supplied names
or identifiers, so apply the same log access controls used for other server logs.

For `RateLimitException`, wait for `getRetryAfter()` when it is present. Do not
retry authentication or authorisation failures in a tight loop. Network retries
should be bounded and use back-off.

## Dependencies and releases

The SDK has no third-party runtime dependencies. Test and build dependencies are
still security-relevant, so use a tagged release, review dependency updates, and
keep the SDK current. Do not depend on a mutable branch for a production plugin.
