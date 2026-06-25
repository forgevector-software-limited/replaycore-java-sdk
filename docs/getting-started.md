# Getting started

This guide takes you from an empty project to a working ReplayCore integration.

## 1. Get an API key

ReplayCore authenticates SDK requests with an **API key** scoped to your tenant
(your account and its servers). Issue one from the ReplayCore panel:

1. Open the panel and go to **Settings → API keys**.
2. Create a key, give it a descriptive name, and grant it the scopes you need:
   - `replays:read`: list and read replay metadata.
   - `replays:write`: add timeline markers.
3. Copy the key **once**, at creation time. It begins `rc_live_` and is shown
   only then; ReplayCore stores only a hash and can never show it again.

Keep the key out of source control. Read it from an environment variable or your
plugin's config file:

```java
String apiKey = System.getenv("REPLAYCORE_API_KEY");
```

## 2. Add the dependency

See the README for the Gradle and Maven coordinates
(`uk.co.forgevector:replaycore-java-sdk`). The SDK needs Java 8+ and pulls in no
other runtime dependencies.

## 3. Build a client

A client is immutable and thread-safe; build one and reuse it.

```java
ReplayCoreClient client = ReplayCoreClient.builder()
        .apiKey(apiKey)
        .connectTimeout(Duration.ofSeconds(5))
        .readTimeout(Duration.ofSeconds(20))
        .build();
```

The builder validates the key's shape immediately (it must carry the `rc_live_`
prefix) so a typo fails fast, locally, before any request is sent. Whether the
key is live, revoked or expired is decided by the server on the first call.

## 4. Make your first call

```java
ReplayPage page = client.listReplays(ReplayQuery.builder().pageSize(10).build());
System.out.println("got " + page.getResults().size() + " replays");
```

If the key is wrong you will get an `AuthenticationException`; if it lacks the
`replays:read` scope, an `AuthorizationException`.

## 5. Choose blocking or async

- **Blocking** (`ReplayCoreClient`) is the simplest model for scripts, web
  backends, or work you have already moved off the main thread.
- **Async** (`ReplayCoreAsyncClient`, via `builder().buildAsync()`) returns
  `CompletableFuture`s and never blocks the calling thread. Use it inside a
  Minecraft plugin so ReplayCore calls never stall the server tick.

```java
ReplayCoreAsyncClient async = ReplayCoreClient.builder().apiKey(apiKey).buildAsync();
async.listReplays(ReplayQuery.builder().build())
     .thenAccept(page -> getLogger().info("replays: " + page.getResults().size()));
```

By default the async client runs on the common fork-join pool. For heavy use,
supply your own executor:

```java
Executor pool = Executors.newFixedThreadPool(4);
ReplayCoreAsyncClient async = new ReplayCoreAsyncClient(
        ReplayCoreClient.builder().apiKey(apiKey).build(), pool);
```

## 6. Handle errors

Wrap calls in a `try/catch` on `ReplayCoreException`, or branch on the typed
subclasses (`NotFoundException`, `RateLimitException`, …). See the README's error
section and [`api-reference.md`](api-reference.md) for the full list.

## Next steps

- Read the [API reference](api-reference.md) for every method, model and field.
- Read the [security model](security.md) before deploying.
