/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReplayQueryTest {

    @Test
    void emptyQueryHasNoParameters() {
        assertTrue(ReplayQuery.builder().build().toQueryParameters().isEmpty());
    }

    @Test
    void rejectsPageSizeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().pageSize(0));
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().pageSize(101));
    }

    @Test
    void rejectsBlankFilters() {
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().search("  "));
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().serverId(""));
    }

    @Test
    void rejectsPlayerAndPlayerUuidTogether() {
        ReplayQuery.Builder b = ReplayQuery.builder().player("Steve").playerUuid("uuid-1");
        assertThrows(IllegalArgumentException.class, b::build);
    }

    @Test
    void rejectsInvertedTimeRange() {
        ReplayQuery.Builder b = ReplayQuery.builder()
                .startedAfter(Instant.parse("2026-06-20T12:00:00Z"))
                .startedBefore(Instant.parse("2026-06-20T11:00:00Z"));
        assertThrows(IllegalArgumentException.class, b::build);
    }

    @Test
    void rejectsInvertedDurationRange() {
        ReplayQuery.Builder b = ReplayQuery.builder()
                .durationMinMs(5000)
                .durationMaxMs(1000);
        assertThrows(IllegalArgumentException.class, b::build);
    }

    @Test
    void rejectsDurationAboveMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> ReplayQuery.builder().durationMaxMs(ReplayQuery.MAX_DURATION_MS + 1));
    }

    @Test
    void rendersAllSetFilters() {
        ReplayQuery query = ReplayQuery.builder()
                .pageSize(50)
                .search("clutch")
                .serverId("server-1")
                .gameMode("duels")
                .flagged(true)
                .starred(false)
                .playerUuid("uuid-9")
                .startedAfter(Instant.parse("2026-06-01T00:00:00Z"))
                .durationMinMs(1000)
                .build();
        Map<String, String> params = query.toQueryParameters();
        assertEquals("50", params.get("page_size"));
        assertEquals("clutch", params.get("q"));
        assertEquals("server-1", params.get("server"));
        assertEquals("duels", params.get("game_mode"));
        assertEquals("true", params.get("flagged"));
        assertEquals("false", params.get("starred"));
        assertEquals("uuid-9", params.get("player_uuid"));
        assertEquals("2026-06-01T00:00:00Z", params.get("started_after"));
        assertEquals("1000", params.get("duration_min_ms"));
        assertFalse(params.containsKey("player"));
    }

    @Test
    void nextPageOfRequiresANextPage() {
        ReplayPage lastPage = new ReplayPage(java.util.Collections.<ReplayMetadata>emptyList(), null, 20);
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.nextPageOf(lastPage));
    }

    @Test
    void nextPageOfCarriesCursor() {
        ReplayPage page = new ReplayPage(java.util.Collections.<ReplayMetadata>emptyList(), "cursor-7", 20);
        ReplayQuery query = ReplayQuery.nextPageOf(page).build();
        assertEquals("cursor-7", query.toQueryParameters().get("page_token"));
    }
}
