/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals("1.0", api.apiVersion());
    }

    /** A trivial in-memory API used to exercise the provider and umbrella contract. */
    private static final class NoOpApi implements ReplayCoreApi {
        @Override
        public String apiVersion() {
            return "1.0";
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
