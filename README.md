# ReplayCore Java SDK

The official Java SDK for the [ReplayCore](https://replaycore.com) developer API.

ReplayCore records Minecraft server gameplay and lets it be watched back, 1:1, in
a browser. This SDK lets plugin developers and server owners work with their own
replays directly from Java, list connected server instances, and annotate replay
timelines without hand-rolling HTTP calls.

It is built for the Minecraft plugin ecosystem: it targets **Java 8** bytecode,
has **zero third-party runtime dependencies** (JDK only), and offers both a
blocking and a non-blocking (`CompletableFuture`) client so it slots cleanly into
a Bukkit, Spigot, Paper or Folia plugin.

---

## Requirements

- Java 8 or newer at runtime.
- A ReplayCore **API key** scoped to your account, issued from the ReplayCore
  dashboard. Keys begin with `rc_live_`.

## Installation

Releases are distributed through [JitPack](https://jitpack.io). Add the JitPack
repository, then the dependency.

The latest tagged release is `v1.4.0`, which reports SDK version `1.4.0` and
tracks the matching ReplayCore product release. Use a tagged release for
production rather than a mutable branch build.

Some earlier product documentation named a `v1.1.5` coordinate. That tag was
never published, so a build depending on it cannot resolve. Move to `v1.4.0`.

**Gradle**

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.forgevector-software-limited:replaycore-java-sdk:v1.4.0'
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
    <version>v1.4.0</version>
</dependency>
```

The REST client may be included in or shaded into a standalone plugin. For the
in-process extension API, use a compile-only dependency and do not shade or
relocate the `api.plugin` package. See [Plugin extensions](docs/plugin-extensions.md).

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
            + ", ready=" + replay.isReady());
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

Pin a named moment on a replay, for example from your own anti-cheat or
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
    // 404: no such replay in your ReplayCore account
} catch (AuthenticationException e) {
    // 401: key missing, invalid, revoked or expired
} catch (AuthorizationException e) {
    // 403: key lacks the required scope
} catch (RateLimitException e) {
    // 429: back off for e.getRetryAfter()
} catch (ReplayCoreApiException e) {
    // any other 4xx/5xx: inspect e.getStatusCode() / e.getCode()
} catch (ReplayCoreTransportException e) {
    // could not reach ReplayCore at all
} catch (ReplayCoreException e) {
    // base type: catch this alone if you do not need to distinguish
}
```

## Security

The SDK contains no embedded credential. Remote calls use the API key and scopes
you configure, so keys must be stored securely and kept out of logs and source
control. See [`docs/security.md`](docs/security.md) for the operating guidance.

## In-process plugin extensions

Alongside the REST client, the SDK ships the supported in-process extension
contract (package `uk.co.forgevector.replaycore.api.plugin`) for plugins running
on the same server as ReplayCore. It covers recording state and lifecycle,
timeline bookmarks, clips, recent kill-replay links, match scopes, reading a
player's replay catalogue and minting watch links in process, and per-subject
capture privacy for hidden or disguised players. See
[`docs/plugin-extensions.md`](docs/plugin-extensions.md).

### Match scopes

Most game-mode servers never restart between games: one arena backend runs
matches back to back, or several at once. `ReplayCoreMatchApi` marks where each
game starts and ends on the one continuous recording, so each game is published
as its own replay without the recorder ever stopping. Because a scope is a
tick-window rather than a recording control, several can be open at the same
time, and the death-cam keeps working throughout.

```java
ReplayCoreApi api = ReplayCoreProvider.get()
        .orElseThrow(() -> new IllegalStateException("ReplayCore not present"));

api.matches().ifPresent(matches -> matches.beginScope(
        BeginScopeRequest.builder(matchId, matchId, "duels", "ranked-1v1", "post-match")
                .participants(Arrays.asList(
                        ReplayParticipant.builder(steve, "Steve").build(),
                        ReplayParticipant.builder(alex, "Alex").build()))
                .build())
        .thenAccept(scope -> scopeIds.put(matchId, scope.scopeId())));
```

Full worked example, including ending the scope and reading the outcome, in
[`docs/plugin-extensions.md`](docs/plugin-extensions.md).

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
