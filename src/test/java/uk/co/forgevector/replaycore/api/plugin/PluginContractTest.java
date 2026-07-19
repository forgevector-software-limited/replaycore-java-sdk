/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PluginContractTest {

    @AfterEach
    void reset() {
        ReplayCoreProvider.clear();
    }

    @Test
    void providerIsEmptyUntilSet() {
        ReplayCoreProvider.clear();
        assertFalse(ReplayCoreProvider.get().isPresent());
    }

    @Test
    void providerReturnsRegisteredApi() {
        ReplayCoreApi api = new NoOpApi();
        ReplayCoreProvider.set(api);
        assertTrue(ReplayCoreProvider.get().isPresent());
        assertEquals(api, ReplayCoreProvider.get().get());
    }

    @Test
    void providerRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> ReplayCoreProvider.set(null));
    }

    @Test
    @SuppressWarnings("deprecation") // exercises the retained, deprecated Bookmark for source compatibility
    void bookmarkBuilderValidatesBounds() {
        assertThrows(IllegalArgumentException.class, () -> Bookmark.builder(""));
        assertThrows(IllegalArgumentException.class, () -> Bookmark.builder("ok").colour("nope"));
        Bookmark mark = Bookmark.builder("Final Kill").category("combat").colour("#00ff00").build();
        assertEquals("Final Kill", mark.label());
        assertEquals("combat", mark.category().get());
        assertEquals("#00ff00", mark.colour().get());
    }

    @Test
    void integrationBookmarkBuilderValidatesAndEncodes() {
        assertThrows(IllegalArgumentException.class, () -> IntegrationBookmark.builder("", "type").build());
        assertThrows(IllegalArgumentException.class, () -> IntegrationBookmark.builder("src", "  ").build());
        IntegrationBookmark mark = IntegrationBookmark.builder("MyGameMode", "objective")
                .severity(IntegrationBookmark.Severity.WARNING)
                .message("Captured the flag")
                .build();
        assertEquals("MyGameMode", mark.source());
        assertEquals("objective", mark.type());
        assertEquals(IntegrationBookmark.Severity.WARNING, mark.severity());
        // The wire payload carries the required fields and the lower-case severity label.
        String payload = mark.toPayload();
        assertTrue(payload.contains("source=MyGameMode"));
        assertTrue(payload.contains("type=objective"));
        assertTrue(payload.contains("severity=warning"));
    }

    @Test
    void umbrellaSubApisNegotiateCapabilityViaOptional() {
        ReplayCoreApi api = new NoOpApi();
        // Always-present surfaces are returned directly; optional ones are empty when unavailable.
        assertNotNull(api.timeline());
        assertNotNull(api.recordingControl());
        assertFalse(api.clips().isPresent());
        assertFalse(api.killReplay().isPresent());
        assertEquals("1.1", api.apiVersion());
    }

    @Test
    void matchSurfaceDefaultsToEmptyForImplementorsThatPredateIt() {
        // NoOpApi deliberately does not override matches(). That it compiles at all is the
        // source-compatibility guarantee; that it reports an empty optional is the runtime one.
        assertFalse(new NoOpApi().matches().isPresent());
    }

    @Test
    void fakeMatchApiDrivesBothDeliveryPaths() {
        FakeReplayCoreMatchApi fake = new FakeReplayCoreMatchApi();
        List<ReplayOperationResult> ready = new ArrayList<ReplayOperationResult>();
        fake.registerListener(new RecordingListener() {
            @Override
            public void onAssetReady(ReplayOperationResult result) {
                ready.add(result);
            }
        });

        ReplayScope scope = fake.beginScope(BeginScopeRequest
                        .builder("begin:m1", "m1", "duels", "ranked-1v1", "post-match")
                        .participants(Collections.singletonList(
                                ReplayParticipant.builder(UUID.randomUUID(), "Steve").build()))
                        .build())
                .toCompletableFuture().join();
        assertEquals("m1", scope.externalMatchId());

        // A repeated idempotency key returns the original scope rather than opening a second one.
        ReplayScope repeated = fake.beginScope(BeginScopeRequest
                        .builder("begin:m1", "m1", "duels", "ranked-1v1", "post-match").build())
                .toCompletableFuture().join();
        assertEquals(scope.scopeId(), repeated.scopeId());

        // endScope stays pending until the test drives it to a terminal outcome.
        CompletionStage<FinalizeResult> finalizing = fake.endScope(scope.scopeId(),
                EndScopeRequest.builder("end:m1")
                        .teams(Collections.singletonList(
                                ReplayTeam.builder("red").result("won").placement(1).build()))
                        .build());
        assertFalse(finalizing.toCompletableFuture().isDone());

        fake.completeReady(scope.scopeId(), "asset-1", "https://api.replaycore.com/a/asset-1", null);

        FinalizeResult result = finalizing.toCompletableFuture().join();
        assertEquals(ProcessingState.READY, result.processingState());
        assertEquals("asset-1", result.assetId().get());
        // The same outcome is delivered on the listener path, for a caller that has since restarted.
        assertEquals(1, ready.size());
        assertEquals(ProcessingState.READY, ready.get(0).processingState());
    }

    @Test
    void killReplaySupportsCommandAndWebOnlyValues() {
        UUID replayId = UUID.randomUUID();
        KillReplay command = new KillReplay(replayId, "/replaycore watch token", 2_000L);
        assertEquals("/replaycore watch token", command.command());
        assertNull(command.webUrl());

        KillReplay webOnly = new KillReplay(replayId, "", 2_000L, "https://replaycore.com/watch/token");
        assertEquals("", webOnly.command());
        assertEquals("https://replaycore.com/watch/token", webOnly.webUrl());
        assertTrue(webOnly.valid(1_999L));
        assertFalse(webOnly.valid(2_000L));
        assertThrows(IllegalArgumentException.class, () -> new KillReplay(replayId, "", 2_000L, ""));
    }

    @Test
    void killReplayWebLookupDefaultsToCommandLookup() {
        UUID replayId = UUID.randomUUID();
        KillReplay expected = new KillReplay(replayId, "/replaycore watch token", 2_000L);
        KillReplayApi api = playerId -> Optional.of(expected);

        assertEquals(expected, api.latestKillReplayWithWebUrl(UUID.randomUUID()).get());
    }

    /** A trivial in-memory API used to exercise the provider and umbrella contract. */
    private static final class NoOpApi implements ReplayCoreApi {
        @Override
        public String apiVersion() {
            return "1.1";
        }

        @Override
        public ReplayCoreTimelineApi timeline() {
            return bookmark -> false;
        }

        @Override
        public RecordingControlApi recordingControl() {
            return new RecordingControlApi() {
                @Override
                public boolean isRecording() {
                    return false;
                }

                @Override
                public java.util.OptionalLong currentTick() {
                    return java.util.OptionalLong.empty();
                }

                @Override
                public java.util.Optional<RecordingSession> currentSession() {
                    return java.util.Optional.empty();
                }
            };
        }

        @Override
        public java.util.Optional<ReplayCoreClipApi> clips() {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<KillReplayApi> killReplay() {
            return java.util.Optional.empty();
        }

        @Override
        public void registerListener(RecordingListener listener) {
        }

        @Override
        public void unregisterListener(RecordingListener listener) {
        }
    }
}
