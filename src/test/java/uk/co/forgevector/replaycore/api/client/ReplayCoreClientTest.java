/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import uk.co.forgevector.replaycore.api.exception.AuthenticationException;
import uk.co.forgevector.replaycore.api.exception.AuthorizationException;
import uk.co.forgevector.replaycore.api.exception.NotFoundException;
import uk.co.forgevector.replaycore.api.exception.RateLimitException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreApiException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreTransportException;
import uk.co.forgevector.replaycore.api.internal.HttpRequest;
import uk.co.forgevector.replaycore.api.model.Quality;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;
import uk.co.forgevector.replaycore.api.model.Visibility;

class ReplayCoreClientTest {

    private static final String KEY = "rc_live_AbCdEfGhIjKlMnOpQrStUvWxYz0123456789";
    private static final String BASE = "https://api.example.test";

    private ReplayCoreClient clientWith(RecordingTransport transport) {
        return ReplayCoreClient.builder()
                .apiKey(KEY)
                .baseUrl(BASE)
                .transport(transport)
                .build();
    }

    @Test
    void listReplaysParsesPageAndSendsBearerAuth() throws ReplayCoreException {
        String body = "{"
                + "\"results\":[{"
                + "\"id\":\"11111111-1111-1111-1111-111111111111\","
                + "\"server_id\":\"22222222-2222-2222-2222-222222222222\","
                + "\"display_name\":\"Final Game\","
                + "\"integration\":\"bedwars\","
                + "\"quality\":\"hd\","
                + "\"started_at\":\"2026-06-20T10:15:30Z\","
                + "\"ended_at\":\"2026-06-20T10:45:30Z\","
                + "\"duration_ms\":1800000,"
                + "\"size_bytes\":52428800,"
                + "\"storage_tier\":\"hot\","
                + "\"visibility\":\"private\","
                + "\"signature_kid\":\"prod-2026\","
                + "\"manifest_hash\":\"abc123\","
                + "\"starred\":true,"
                + "\"format_version\":3,"
                + "\"participants\":[{\"uuid\":\"p-1\",\"name\":\"Steve\",\"role\":\"player\"}]"
                + "}],"
                + "\"next_page_token\":\"cursor-2\","
                + "\"page_size\":20}";
        RecordingTransport transport = new RecordingTransport().enqueue(200, body);
        ReplayCoreClient client = clientWith(transport);

        ReplayPage page = client.listReplays(ReplayQuery.builder().pageSize(20).build());

        assertEquals(1, page.getResults().size());
        assertTrue(page.hasNextPage());
        assertEquals("cursor-2", page.getNextPageToken().get());

        ReplayMetadata replay = page.getResults().get(0);
        assertEquals("11111111-1111-1111-1111-111111111111", replay.getId());
        assertEquals("Final Game", replay.getDisplayName().get());
        assertEquals(Quality.HD, replay.getQuality());
        assertEquals(Visibility.PRIVATE, replay.getVisibility());
        assertEquals(1_800_000L, replay.getDurationMs().get().longValue());
        assertEquals(Duration.ofMinutes(30), replay.getDuration().get());
        assertEquals(Instant.parse("2026-06-20T10:15:30Z"), replay.getStartedAt().get());
        assertTrue(replay.isStarred());
        assertTrue(replay.isReady());
        assertEquals(1, replay.getParticipants().size());
        assertEquals("Steve", replay.getParticipants().get(0).getName().get());

        HttpRequest sent = transport.lastRequest();
        assertEquals("GET", sent.getMethod());
        assertEquals("Bearer " + KEY, sent.getHeaders().get("Authorization"));
        assertTrue(sent.getUrl().startsWith(BASE + "/v1/api/replays?"));
        assertTrue(sent.getUrl().contains("page_size=20"));
    }

    @Test
    void listReplaysEncodesFiltersIntoQueryString() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, "{\"results\":[],\"next_page_token\":\"\",\"page_size\":10}");
        ReplayCoreClient client = clientWith(transport);

        ReplayQuery query = ReplayQuery.builder()
                .pageSize(10)
                .search("clutch moment")
                .gameMode("bedwars")
                .starred(true)
                .build();
        ReplayPage page = client.listReplays(query);

        assertFalse(page.hasNextPage());
        String url = transport.lastRequest().getUrl();
        assertTrue(url.contains("q=clutch%20moment"), url);
        assertTrue(url.contains("game_mode=bedwars"), url);
        assertTrue(url.contains("starred=true"), url);
    }

    @Test
    void getReplayParsesSingleObject() throws ReplayCoreException {
        String body = "{"
                + "\"id\":\"33333333-3333-3333-3333-333333333333\","
                + "\"quality\":\"standard\","
                + "\"storage_tier\":\"r2_ia\","
                + "\"visibility\":\"public\","
                + "\"signature_kid\":\"staging-unsigned\","
                + "\"archive_status\":\"original\","
                + "\"format_version\":2}";
        RecordingTransport transport = new RecordingTransport().enqueue(200, body);
        ReplayCoreClient client = clientWith(transport);

        ReplayMetadata replay = client.getReplay("33333333-3333-3333-3333-333333333333");

        assertEquals("33333333-3333-3333-3333-333333333333", replay.getId());
        assertEquals(Quality.STANDARD, replay.getQuality());
        // staging-unsigned + no manifest hash means not ready.
        assertFalse(replay.isReady());
        assertEquals(BASE + "/v1/api/replays/33333333-3333-3333-3333-333333333333",
                transport.lastRequest().getUrl());
    }

    @Test
    void getReplayMapsNotFoundToTypedException() {
        RecordingTransport transport = new RecordingTransport().enqueue(404,
                "{\"type\":\"https://replaycore.com/problems/REPLAY_NOT_FOUND\","
                        + "\"title\":\"Not Found\",\"status\":404,"
                        + "\"code\":\"REPLAY_NOT_FOUND\",\"detail\":\"no such replay\"}");
        ReplayCoreClient client = clientWith(transport);

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> client.getReplay("00000000-0000-0000-0000-000000000000"));
        assertEquals(404, ex.getStatusCode());
        assertEquals("REPLAY_NOT_FOUND", ex.getCode());
        assertEquals("no such replay", ex.getDetail());
    }

    @Test
    void unauthorisedMapsToAuthenticationException() {
        RecordingTransport transport = new RecordingTransport().enqueue(401,
                "{\"code\":\"INVALID_API_KEY\",\"detail\":\"the API key is invalid\"}");
        ReplayCoreClient client = clientWith(transport);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> client.listReplays(ReplayQuery.builder().build()));
        assertEquals("INVALID_API_KEY", ex.getCode());
    }

    @Test
    void forbiddenMapsToAuthorizationException() {
        RecordingTransport transport = new RecordingTransport().enqueue(403,
                "{\"code\":\"INSUFFICIENT_SCOPE\",\"detail\":\"missing scope\"}");
        ReplayCoreClient client = clientWith(transport);

        assertThrows(AuthorizationException.class,
                () -> client.listReplays(ReplayQuery.builder().build()));
    }

    @Test
    void rateLimitedCarriesRetryAfter() {
        RecordingTransport transport = new RecordingTransport().enqueue(429,
                "{\"code\":\"RATE_LIMITED\",\"detail\":\"slow down\"}",
                Collections.singletonMap("Retry-After", "12"));
        ReplayCoreClient client = clientWith(transport);

        RateLimitException ex = assertThrows(RateLimitException.class,
                () -> client.listReplays(ReplayQuery.builder().build()));
        assertEquals(Duration.ofSeconds(12), ex.getRetryAfter());
    }

    @Test
    void serverErrorMapsToGenericApiException() {
        RecordingTransport transport = new RecordingTransport().enqueue(500,
                "{\"code\":\"INTERNAL_ERROR\",\"detail\":\"boom\"}");
        ReplayCoreClient client = clientWith(transport);

        ReplayCoreApiException ex = assertThrows(ReplayCoreApiException.class,
                () -> client.getReplay("33333333-3333-3333-3333-333333333333"));
        assertEquals(500, ex.getStatusCode());
        assertEquals("INTERNAL_ERROR", ex.getCode());
    }

    @Test
    void transportFailureMapsToTransportException() {
        RecordingTransport transport = new RecordingTransport()
                .enqueueFailure(new IOException("connection refused"));
        ReplayCoreClient client = clientWith(transport);

        ReplayCoreTransportException ex = assertThrows(ReplayCoreTransportException.class,
                () -> client.listReplays(ReplayQuery.builder().build()));
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void nonProblemJsonErrorBodyStillMapsByStatus() {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(502, "<html>bad gateway</html>");
        ReplayCoreClient client = clientWith(transport);

        ReplayCoreApiException ex = assertThrows(ReplayCoreApiException.class,
                () -> client.listReplays(ReplayQuery.builder().build()));
        assertEquals(502, ex.getStatusCode());
        assertNull(ex.getCode());
    }

    @Test
    void createTimelineMarkerSendsCamelCaseBodyAndParsesResponse() throws ReplayCoreException {
        String response = "{"
                + "\"id\":\"marker-1\","
                + "\"replayId\":\"44444444-4444-4444-4444-444444444444\","
                + "\"tick\":1200,"
                + "\"label\":\"Final Death\","
                + "\"category\":\"combat\","
                + "\"createdAt\":\"2026-06-20T10:30:00Z\"}";
        RecordingTransport transport = new RecordingTransport().enqueue(201, response);
        ReplayCoreClient client = clientWith(transport);

        TimelineEventRequest request = TimelineEventRequest
                .forReplay("44444444-4444-4444-4444-444444444444")
                .tick(1200)
                .label("Final Death")
                .category("combat")
                .build();
        TimelineMarker marker = client.createTimelineMarker(request);

        assertEquals("marker-1", marker.getId());
        assertEquals("44444444-4444-4444-4444-444444444444", marker.getReplayId());
        assertEquals(1200, marker.getTick());
        assertEquals("combat", marker.getCategory().get());
        assertEquals(Instant.parse("2026-06-20T10:30:00Z"), marker.getCreatedAt().get());

        HttpRequest sent = transport.lastRequest();
        assertEquals("POST", sent.getMethod());
        assertEquals(BASE + "/v1/api/timeline-events", sent.getUrl());
        assertEquals("application/json", sent.getHeaders().get("Content-Type"));
        String body = sent.getBody();
        assertTrue(body.contains("\"replayId\":\"44444444-4444-4444-4444-444444444444\""), body);
        assertTrue(body.contains("\"tick\":1200"), body);
        assertTrue(body.contains("\"label\":\"Final Death\""), body);
    }

    @Test
    void asyncClientCompletesAndShareConfiguration() throws Exception {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, "{\"id\":\"55555555-5555-5555-5555-555555555555\","
                        + "\"quality\":\"hd\",\"storage_tier\":\"hot\",\"visibility\":\"private\","
                        + "\"signature_kid\":\"prod\",\"manifest_hash\":\"hash\",\"format_version\":3}");
        ReplayCoreClient sync = clientWith(transport);
        ReplayCoreAsyncClient async = new ReplayCoreAsyncClient(sync);

        ReplayMetadata replay = async.getReplay("55555555-5555-5555-5555-555555555555")
                .get();

        assertEquals("55555555-5555-5555-5555-555555555555", replay.getId());
        assertTrue(replay.isReady());
        assertSame(sync, async.blocking());
    }

    @Test
    void asyncClientPropagatesApiExceptionAsCause() {
        RecordingTransport transport = new RecordingTransport().enqueue(401,
                "{\"code\":\"INVALID_API_KEY\",\"detail\":\"nope\"}");
        ReplayCoreAsyncClient async = new ReplayCoreAsyncClient(clientWith(transport));

        java.util.concurrent.ExecutionException ex = assertThrows(
                java.util.concurrent.ExecutionException.class,
                () -> async.listReplays(ReplayQuery.builder().build()).get());
        assertTrue(ex.getCause() instanceof AuthenticationException);
    }
}
