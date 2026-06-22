/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void bookmarkBuilderValidatesBounds() {
        assertThrows(IllegalArgumentException.class, () -> Bookmark.builder(""));
        assertThrows(IllegalArgumentException.class, () -> Bookmark.builder("ok").colour("nope"));
        Bookmark mark = Bookmark.builder("Final Kill").category("combat").colour("#00ff00").build();
        assertEquals("Final Kill", mark.label());
        assertEquals("combat", mark.category().get());
        assertEquals("#00ff00", mark.colour().get());
    }

    /** A trivial in-memory API used to exercise the provider contract. */
    private static final class NoOpApi implements ReplayCoreApi {
        @Override
        public String apiVersion() {
            return "1.0";
        }

        @Override
        public RecordingService recordingService() {
            return new RecordingService() {
                @Override
                public boolean isRecording() {
                    return false;
                }

                @Override
                public java.util.Optional<Long> currentTick() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<RecordingSession> currentSession() {
                    return java.util.Optional.empty();
                }

                @Override
                public boolean addBookmark(Bookmark bookmark) {
                    return false;
                }
            };
        }

        @Override
        public void registerListener(RecordingListener listener) {
        }

        @Override
        public void unregisterListener(RecordingListener listener) {
        }
    }
}
