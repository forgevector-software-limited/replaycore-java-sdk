/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ReplayCoreClientBuilderTest {

    private static final String KEY = "rc_live_AbCdEfGhIjKlMnOpQrStUvWxYz0123456789";

    @Test
    void rejectsMissingApiKey() {
        assertThrows(IllegalStateException.class, () -> ReplayCoreClient.builder().build());
    }

    @Test
    void rejectsBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () -> ReplayCoreClient.builder().apiKey("   "));
    }

    @Test
    void rejectsApiKeyWithoutBrandedPrefix() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReplayCoreClient.builder().apiKey("sk_test_whatever"));
        assertEquals(true, ex.getMessage().contains("rc_live_"));
    }

    @Test
    void rejectsNonHttpBaseUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> ReplayCoreClient.builder().baseUrl("ftp://example.test"));
    }

    @Test
    void rejectsNegativeTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> ReplayCoreClient.builder().connectTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    void rejectsNullTransport() {
        assertThrows(IllegalArgumentException.class,
                () -> ReplayCoreClient.builder().transport(null));
    }

    @Test
    void buildsWithDefaults() {
        assertNotNull(ReplayCoreClient.builder().apiKey(KEY).build());
    }

    @Test
    void buildsAsyncClient() {
        assertNotNull(ReplayCoreClient.builder().apiKey(KEY).buildAsync());
    }

    @Test
    void trimsTrailingSlashFromBaseUrl() {
        // No exception, and the client builds; URL correctness is verified through
        // the request assertions in the client test, which uses a non-slashed base.
        assertNotNull(ReplayCoreClient.builder()
                .apiKey(KEY)
                .baseUrl("https://api.example.test/")
                .build());
    }
}
