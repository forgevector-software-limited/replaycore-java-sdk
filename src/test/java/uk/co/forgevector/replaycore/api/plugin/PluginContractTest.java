/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

    @Test
    void scopeEventsAreOptionalForImplementorsThatPredateThem() {
        // ScopeEventFreeMatchApi implements only the three original abstract methods. That it compiles at
        // all is the source-compatibility guarantee for a recorder built before scope events existed.
        ReplayCoreMatchApi predating = new ScopeEventFreeMatchApi();
        assertFalse(predating.supportsScopeEvents());

        IntegrationBookmark marker = IntegrationBookmark.builder("MyGameMode", "kill").build();
        CompletableFuture<Void> tagged = predating.tagScopeEvent("scope-1", marker).toCompletableFuture();
        assertTrue(tagged.isCompletedExceptionally());
        CompletionException taggedFailure = assertThrows(CompletionException.class, () -> tagged.join());
        assertTrue(taggedFailure.getCause() instanceof UnsupportedOperationException);

        CompletableFuture<Void> clipped = predating
                .recordScopeClip("scope-1", ScopeClipRequest.builder(EventKind.KILL).build())
                .toCompletableFuture();
        assertTrue(clipped.isCompletedExceptionally());
        CompletionException clipFailure = assertThrows(CompletionException.class, () -> clipped.join());
        assertTrue(clipFailure.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    void defaultScopeEventStagesAreNotSharedBetweenCalls() {
        // The defaults allocate per call so that one caller completing or cancelling the returned stage
        // cannot corrupt the result every later caller sees.
        ReplayCoreMatchApi predating = new ScopeEventFreeMatchApi();
        IntegrationBookmark marker = IntegrationBookmark.builder("MyGameMode", "kill").build();
        assertNotSame(predating.tagScopeEvent("scope-1", marker).toCompletableFuture(),
                predating.tagScopeEvent("scope-1", marker).toCompletableFuture());
    }

    @Test
    void fakeRoutesScopeEventsToTheScopeTheCallNamed() {
        FakeReplayCoreMatchApi fake = new FakeReplayCoreMatchApi();
        assertTrue(fake.supportsScopeEvents());

        ReplayScope first = openScope(fake, "begin:m1", "m1");
        ReplayScope second = openScope(fake, "begin:m2", "m2");

        fake.tagScopeEvent(first.scopeId(), IntegrationBookmark.builder("Duels", "kill").build())
                .toCompletableFuture().join();
        fake.tagScopeEvent(second.scopeId(), IntegrationBookmark.builder("Duels", "kill").build())
                .toCompletableFuture().join();
        fake.recordScopeClip(second.scopeId(), ScopeClipRequest.builder(EventKind.KILL)
                        .preRollTicks(120L)
                        .postRollTicks(60L)
                        .build())
                .toCompletableFuture().join();

        // Two concurrent matches: each marker is addressed to the match it was raised for, and no other.
        assertEquals(2, fake.tagScopeEventCalls().size());
        assertEquals(first.scopeId(), fake.tagScopeEventCalls().get(0).scopeId());
        assertEquals(second.scopeId(), fake.tagScopeEventCalls().get(1).scopeId());
        assertEquals(1, fake.recordScopeClipCalls().size());
        assertEquals(second.scopeId(), fake.recordScopeClipCalls().get(0).scopeId());
        assertEquals(EventKind.KILL, fake.recordScopeClipCalls().get(0).request().eventKind());
    }

    @Test
    void fakeRejectsScopeEventsForUnknownAndEndedScopes() {
        FakeReplayCoreMatchApi fake = new FakeReplayCoreMatchApi();
        IntegrationBookmark marker = IntegrationBookmark.builder("Duels", "kill").build();

        CompletableFuture<Void> unknown = fake.tagScopeEvent("no-such-scope", marker).toCompletableFuture();
        assertTrue(unknown.isCompletedExceptionally());

        ReplayScope scope = openScope(fake, "begin:m1", "m1");
        fake.endScope(scope.scopeId(), EndScopeRequest.builder("end:m1").build());

        CompletableFuture<Void> afterEnd = fake.tagScopeEvent(scope.scopeId(), marker).toCompletableFuture();
        assertTrue(afterEnd.isCompletedExceptionally());
    }

    @Test
    void scopeClipRequestClampsWindowAndRecordsRelationships() {
        UUID killer = UUID.randomUUID();
        UUID victim = UUID.randomUUID();
        ScopeClipRequest request = ScopeClipRequest.builder(EventKind.KILL)
                .preRollTicks(999_999L)
                .postRollTicks(-5L)
                .killer(killer)
                .victim(victim)
                .build();

        // Over-long and negative windows are clamped rather than rejected.
        assertEquals(ScopeClipRequest.MAX_PRE_ROLL_TICKS, request.preRollTicks());
        assertEquals(0L, request.postRollTicks());
        Map<UUID, AssetRelationship> relationships = request.relationships();
        assertEquals(AssetRelationship.KILL, relationships.get(killer));
        assertEquals(AssetRelationship.DEATH, relationships.get(victim));
        assertNull(request.clientEventId());
        assertThrows(UnsupportedOperationException.class,
                () -> request.relationships().put(UUID.randomUUID(), AssetRelationship.PARTICIPANT));
    }

    @Test
    void scopeClipRequestRejectsAnUnusableClientEventId() {
        assertThrows(IllegalArgumentException.class,
                () -> ScopeClipRequest.builder(EventKind.KILL).clientEventId("   ").build());
        assertThrows(IllegalArgumentException.class,
                () -> ScopeClipRequest.builder(EventKind.KILL).clientEventId("bad\tid").build());
        assertThrows(IllegalArgumentException.class, () -> ScopeClipRequest.builder(EventKind.KILL)
                .clientEventId(repeat('x', ScopeClipRequest.MAX_CLIENT_EVENT_ID_LENGTH + 1))
                .build());
        assertThrows(NullPointerException.class, () -> ScopeClipRequest.builder(null).build());
    }

    @Test
    void scopeClipRequestGivesAPlayerOneMeaning() {
        UUID player = UUID.randomUUID();
        ScopeClipRequest request = ScopeClipRequest.builder(EventKind.ROUND_END)
                .participant(player)
                .killer(player)
                .build();

        assertEquals(1, request.relationships().size());
        assertEquals(AssetRelationship.KILL, request.relationships().get(player));
    }

    private static ReplayScope openScope(FakeReplayCoreMatchApi fake, String key, String matchId) {
        return fake.beginScope(BeginScopeRequest
                        .builder(key, matchId, "duels", "ranked-1v1", "post-match").build())
                .toCompletableFuture().join();
    }

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    /** A match API implementing only the methods that existed before scope events, to pin source compatibility. */
    private static final class ScopeEventFreeMatchApi implements ReplayCoreMatchApi {
        @Override
        public CompletionStage<ReplayScope> beginScope(BeginScopeRequest request) {
            return new CompletableFuture<ReplayScope>();
        }

        @Override
        public CompletionStage<Void> updateScope(String scopeId, ScopeUpdate update) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<FinalizeResult> endScope(String scopeId, EndScopeRequest request) {
            return new CompletableFuture<FinalizeResult>();
        }
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
