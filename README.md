# ReplayCore Java SDK

The official Java SDK for the [ReplayCore](https://replaycore.com) developer API.

ReplayCore records Minecraft server gameplay and lets it be watched back, 1:1, in
a browser. This SDK lets plugin developers and server owners work with their own
replays directly from Java — listing and inspecting recordings, and annotating
their timelines — without hand-rolling HTTP calls.

It is built for the Minecraft plugin ecosystem: it targets **Java 8** bytecode,
has **zero third-party runtime dependencies** (JDK only), and offers both a
blocking and a non-blocking (`CompletableFuture`) client so it slots cleanly into
a Bukkit, Spigot, Paper or Folia plugin.

---

## Requirements

- Java 8 or newer at runtime.
- A ReplayCore **API key** scoped to your server/tenant, issued from the
  ReplayCore panel. Keys begin with `rc_live_`.

## Installation

Releases are distributed through [JitPack](https://jitpack.io). Add the JitPack
repository, then the dependency.

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.forgevector-software-limited:replaycore-java-sdk:v1.0.0'
}
```

**Maven**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.forgevector-software-limited</groupId>
    <artifactId>replaycore-java-sdk</artifactId>
    <version>v1.0.0</version>
</dependency>
```

If you shade the SDK into a plugin jar, no relocation is required — the SDK
brings no transitive dependencies to clash with the server's classpath.

## Quickstart

```java
import uk.co.forgevector.replaycore.api.client.ReplayCoreClient;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;

ReplayCoreClient client = ReplayCoreClient.builder()
        .apiKey(System.getenv("REPLAYCORE_API_KEY"))   // never hard-code the key
        .build();

ReplayPage page = client.listReplays(
        ReplayQuery.builder()
                .pageSize(25)
                .gameMode("bedwars")
                .starred(true)
                .build());

for (ReplayMetadata replay : page.getResults()) {
    System.out.println(replay.getDisplayName().orElse(replay.getId())
            + " — ready=" + replay.isReady());
}
```

### Fetch one replay

```java
ReplayMetadata replay = client.getReplay("11111111-1111-1111-1111-111111111111");
replay.getDuration().ifPresent(d -> System.out.println("length: " + d));
System.out.println("visibility: " + replay.getVisibility());
```

### Page through results

```java
ReplayQuery query = ReplayQuery.builder().pageSize(50).build();
ReplayPage page = client.listReplays(query);

while (true) {
    page.getResults().forEach(r -> process(r));
    if (!page.hasNextPage()) {
        break;
    }
    page = client.listReplays(ReplayQuery.nextPageOf(page).build());
}
```

### Add a timeline marker

Pin a named moment on a replay — for example from your own anti-cheat or
mini-game plugin:

```java
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;

TimelineMarker marker = client.createTimelineMarker(
        TimelineEventRequest.forReplay(replay.getId())
                .tick(1200)
                .label("Final Death")
                .category("combat")
                .colour("#ff5555")
                .build());

System.out.println("created marker " + marker.getId());
```

You can also target a server's **currently active** recording, which is handy
for live tagging, using `TimelineEventRequest.forActiveRecording(serverId)`.
Writing markers requires a key with the `replays:write` scope.

### Non-blocking use inside a plugin

Never block the main server thread on network I/O. Build the async client and
chain off the future:

```java
import uk.co.forgevector.replaycore.api.client.ReplayCoreAsyncClient;

ReplayCoreAsyncClient client = ReplayCoreClient.builder()
        .apiKey(apiKey)
        .buildAsync();

client.getReplay(replayId)
      .thenAccept(replay -> getLogger().info("ready=" + replay.isReady()))
      .exceptionally(err -> { getLogger().warning(err.getMessage()); return null; });
```

## Error handling

Every remote failure is a checked `ReplayCoreException`. Server errors map to
typed subclasses so you can branch cleanly:

```java
import uk.co.forgevector.replaycore.api.exception.*;

try {
    ReplayMetadata replay = client.getReplay(id);
} catch (NotFoundException e) {
    // 404 — no such replay in your tenant
} catch (AuthenticationException e) {
    // 401 — key missing, invalid, revoked or expired
} catch (AuthorizationException e) {
    // 403 — key lacks the required scope
} catch (RateLimitException e) {
    // 429 — back off for e.getRetryAfter()
} catch (ReplayCoreApiException e) {
    // any other 4xx/5xx — inspect e.getStatusCode() / e.getCode()
} catch (ReplayCoreTransportException e) {
    // could not reach ReplayCore at all
} catch (ReplayCoreException e) {
    // base type — catch this alone if you do not need to distinguish
}
```

## Security

The SDK authenticates with a single, tenant-scoped API key and never reaches
beyond the tenant that key belongs to. There are no embedded secrets, and no
method can access another tenant's data or an administrative endpoint. See
[`docs/security.md`](docs/security.md) for the full model.

## In-process plugin extensions

Alongside the REST client, the SDK ships a forward-looking, in-process extension
contract (package `uk.co.forgevector.replaycore.api.plugin`) for addons that run
on the same server as the ReplayCore recorder — observing recording lifecycle and
adding live bookmarks. Its availability depends on the recorder version on the
server; see [`docs/plugin-extensions.md`](docs/plugin-extensions.md).

## Documentation

- [Getting started](docs/getting-started.md)
- [API reference](docs/api-reference.md)
- [Security model](docs/security.md)
- [Plugin extension contract](docs/plugin-extensions.md)
- Full Javadoc: run `./gradlew javadoc` and open `build/docs/javadoc/index.html`.

## Building from source

```bash
./gradlew build      # compile, run tests, assemble jar + sources + javadoc jars
./gradlew javadoc    # generate API docs into build/docs/javadoc
```

A Maven build (`mvn verify`) is also provided via `pom.xml`.

## Licence

Copyright (c) ForgeVector Software Limited. All rights reserved. See
[`LICENSE`](LICENSE).
