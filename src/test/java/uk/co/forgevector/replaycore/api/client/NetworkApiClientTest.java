/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.co.forgevector.replaycore.api.Fixtures;
import uk.co.forgevector.replaycore.api.exception.AuthorizationException;
import uk.co.forgevector.replaycore.api.exception.NotFoundException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreApiException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreException;
import uk.co.forgevector.replaycore.api.internal.HttpRequest;
import uk.co.forgevector.replaycore.api.model.ApiResponse;
import uk.co.forgevector.replaycore.api.model.AssetFinalizeRequest;
import uk.co.forgevector.replaycore.api.model.AssetKind;
import uk.co.forgevector.replaycore.api.model.AssetQuery;
import uk.co.forgevector.replaycore.api.model.AssetRelation;
import uk.co.forgevector.replaycore.api.model.AssetRequest;
import uk.co.forgevector.replaycore.api.model.BatchEntryResult;
import uk.co.forgevector.replaycore.api.model.BatchEntryStatus;
import uk.co.forgevector.replaycore.api.model.CatalogFacets;
import uk.co.forgevector.replaycore.api.model.CatalogPage;
import uk.co.forgevector.replaycore.api.model.CollectionFinalizeRequest;
import uk.co.forgevector.replaycore.api.model.CollectionQuery;
import uk.co.forgevector.replaycore.api.model.CollectionRequest;
import uk.co.forgevector.replaycore.api.model.CollectionUpdateRequest;
import uk.co.forgevector.replaycore.api.model.CollectionVisibility;
import uk.co.forgevector.replaycore.api.model.EventKind;
import uk.co.forgevector.replaycore.api.model.OperationStatus;
import uk.co.forgevector.replaycore.api.model.PlayerReplayQuery;
import uk.co.forgevector.replaycore.api.model.ProcessingState;
import uk.co.forgevector.replaycore.api.model.ReleasePolicy;
import uk.co.forgevector.replaycore.api.model.ReleaseState;
import uk.co.forgevector.replaycore.api.model.ReleaseTrigger;
import uk.co.forgevector.replaycore.api.model.ReplayAsset;
import uk.co.forgevector.replaycore.api.model.ReplayCollection;
import uk.co.forgevector.replaycore.api.model.ScopeClipEntry;
import uk.co.forgevector.replaycore.api.model.ScopeMarker;
import uk.co.forgevector.replaycore.api.model.ScopeMarkerEntry;
import uk.co.forgevector.replaycore.api.model.ScopeOperation;
import uk.co.forgevector.replaycore.api.model.ScopeParticipant;
import uk.co.forgevector.replaycore.api.model.ScopeSegment;
import uk.co.forgevector.replaycore.api.model.ScopeTeam;
import uk.co.forgevector.replaycore.api.model.WatchLink;
import uk.co.forgevector.replaycore.api.model.WatchTicket;
import uk.co.forgevector.replaycore.api.model.WatchTicketRedemption;
import uk.co.forgevector.replaycore.api.model.WebhookDeliveryReplay;

class NetworkApiClientTest {

    private static final String KEY = "rc_live_AbCdEfGhIjKlMnOpQrStUvWxYz0123456789";
    private static final String BASE = "https://api.example.test";
    private static final String COLLECTION = "3f0c2a64-5d1e-4b7a-9c3e-1a2b3c4d5e6f";
    private static final String ASSET = "8a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";
    private static final String SEGMENT = "2b7c9d10-6e3f-4f4a-8b5c-0d1e2f3a4b5c";
    private static final String ARCHIVE = "5e55105e-0000-4000-8000-000000000001";
    private static final String SERVER = "11111111-1111-4111-8111-111111111111";
    private static final String PLAYER = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    private ReplayCoreClient clientWith(RecordingTransport transport) {
        return ReplayCoreClient.builder().apiKey(KEY).baseUrl(BASE).transport(transport).build();
    }

    private static ReleasePolicy policy() {
        return ReleasePolicy.builder(ReleaseTrigger.COLLECTION_END, ReleaseTrigger.ASSET_READY,
                CollectionVisibility.PUBLIC).staffBypass(true).build();
    }

    // ---------------------------------------------------------------- requestJson headers

    @Test
    void requestJsonSendsCallerHeadersAlongsideTheClientsOwn() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, "{\"ok\":true}");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Idempotency-Key", "order-17");
        headers.put("If-Match", "\"etag-3\"");

        ApiResponse response = clientWith(transport).requestJson("PUT", "/v1/api/setup/thing",
                Collections.singletonMap("a", "b"), headers);

        assertEquals(200, response.getStatusCode());
        HttpRequest sent = transport.lastRequest();
        assertEquals("order-17", sent.getHeaders().get("Idempotency-Key"));
        assertEquals("\"etag-3\"", sent.getHeaders().get("If-Match"));
        assertEquals("Bearer " + KEY, sent.getHeaders().get("Authorization"));
        assertEquals("application/json", sent.getHeaders().get("Content-Type"));
    }

    @Test
    void threeArgumentRequestJsonSendsNoExtraHeaders() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, "{}");
        clientWith(transport).requestJson("GET", "/v1/api/servers", null);
        assertEquals(3, transport.lastRequest().getHeaders().size());
    }

    @Test
    void requestJsonRefusesHeadersTheClientOwns() {
        ReplayCoreClient client = clientWith(new RecordingTransport());
        for (String name : Arrays.asList("Authorization", "authorization", "HOST", "User-Agent", "user-agent",
                "Accept", "aCcEpT", "Content-Type", "content-type")) {
            Map<String, String> headers = Collections.singletonMap(name, "x");
            assertThrows(IllegalArgumentException.class,
                    () -> client.requestJson("GET", "/v1/api/servers", null, headers), name);
        }
    }

    @Test
    void requestJsonRefusesControlCharactersAndMalformedNames() {
        ReplayCoreClient client = clientWith(new RecordingTransport());
        List<Map<String, String>> bad = Arrays.<Map<String, String>>asList(
                Collections.singletonMap("X-A\r\nX-B", "v"),
                Collections.singletonMap("X-A\n", "v"),
                Collections.singletonMap("X-A\0", "v"),
                Collections.singletonMap("X A", "v"),
                Collections.singletonMap("X-A:", "v"),
                Collections.singletonMap("", "v"),
                Collections.singletonMap("X-A", "v\r\nInjected: 1"),
                Collections.singletonMap("X-A", "v\nx"),
                Collections.singletonMap("X-A", "v\0"),
                Collections.singletonMap("X-A", ""),
                Collections.<String, String>singletonMap("X-A", null));
        for (Map<String, String> headers : bad) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.requestJson("GET", "/v1/api/servers", null, headers), headers.toString());
        }
    }

    @Test
    void requestJsonRefusesRepeatedHeaderNamesIgnoringCase() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("If-Match", "a");
        headers.put("if-match", "b");
        assertThrows(IllegalArgumentException.class,
                () -> clientWith(new RecordingTransport()).requestJson("GET", "/v1/api/servers", null, headers));
    }

    @Test
    void requestJsonKeepsItsPathSafetyWithHeaders() {
        ReplayCoreClient client = clientWith(new RecordingTransport());
        assertThrows(IllegalArgumentException.class,
                () -> client.requestJson("GET", "/v1/../admin", null, Collections.singletonMap("If-Match", "x")));
        assertThrows(IllegalArgumentException.class,
                () -> client.requestJson("GET", "https://evil.test/v1/x", null, null));
    }

    @Test
    void asyncRequestJsonForwardsHeaders() throws Exception {
        RecordingTransport transport = new RecordingTransport().enqueue(200, "{\"ok\":true}");
        ReplayCoreAsyncClient async = new ReplayCoreAsyncClient(clientWith(transport));

        async.requestJson("POST", "/v1/api/setup/x", null, Collections.singletonMap("Idempotency-Key", "k1")).get();

        assertEquals("k1", transport.lastRequest().getHeaders().get("Idempotency-Key"));
    }

    // ---------------------------------------------------------------- lifecycle

    @Test
    void createCollectionSendsIdempotencyKeyHeaderAndParsesTheCollection() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(201, Fixtures.text("collection.json"));
        CollectionRequest request = CollectionRequest
                .builder("begin-match-42", SERVER, "bedwars", "solo", "embargo-until-end", policy())
                .externalMatchId("match-20261028-0042")
                .worldIds(Arrays.asList("world"))
                .participant(ScopeParticipant.builder(PLAYER, "Notch").teamKey("red").joinedTick(5).build())
                .team(ScopeTeam.builder("red").name("Red Team").build())
                .spectator("853c80ef-3c37-49fd-aa49-938b674adae6")
                .segment(ScopeSegment.builder(ARCHIVE, 0, 0).build())
                .teamSize(4)
                .ranked(true)
                .metadata(Collections.singletonMap("acme:arena", "sky-fortress"))
                .build();

        ReplayCollection collection = clientWith(transport).createCollection(request);

        assertEquals(COLLECTION, collection.getId());
        assertEquals(ProcessingState.READY, collection.getProcessingState());
        assertEquals(ReleaseState.RELEASED, collection.getReleaseState());
        assertEquals(CollectionVisibility.PUBLIC, collection.getVisibility());
        assertEquals(AssetRelation.KILL, collection.getViewerRelation().get());
        assertEquals(1, collection.getParticipants().size());
        assertEquals("Notch", collection.getParticipants().get(0).getPlayerName());
        assertEquals(Integer.valueOf(1), collection.getTeams().get(0).getPlacement().get());
        assertEquals("sky-fortress", collection.getMetadata().get("acme:arena"));
        assertEquals(Instant.parse("2026-10-28T12:10:00Z"), collection.getEndedAt().get());
        assertTrue(collection.isPlayable());

        HttpRequest sent = transport.lastRequest();
        assertEquals("POST", sent.getMethod());
        assertEquals(BASE + "/v1/replay-collections", sent.getUrl());
        assertEquals("begin-match-42", sent.getHeaders().get("Idempotency-Key"));
        assertFalse(sent.getHeaders().containsKey("If-Match"));
        assertEquals("application/json", sent.getHeaders().get("Content-Type"));
        String body = sent.getBody();
        assertTrue(body.contains("\"server_id\":\"" + SERVER + "\""), body);
        assertTrue(body.contains("\"external_match_id\":\"match-20261028-0042\""), body);
        assertTrue(body.contains("\"policy_snapshot\":{\"full-replay\":{\"release-trigger\":\"collection_end\"},"
                + "\"event-clips\":{\"release-trigger\":\"asset_ready\"},\"visibility\":\"public\","
                + "\"staff-bypass\":true}"), body);
        assertTrue(body.contains("\"participants\":[{\"player_uuid\":\"" + PLAYER
                + "\",\"player_name\":\"Notch\",\"team_key\":\"red\",\"role\":\"participant\",\"joined_tick\":5}]"), body);
        assertTrue(body.contains("\"teams\":[{\"key\":\"red\",\"name\":\"Red Team\"}]"), body);
        assertTrue(body.contains("\"spectators\":[\"853c80ef-3c37-49fd-aa49-938b674adae6\"]"), body);
        assertTrue(body.contains("\"segments\":[{\"archive_id\":\"" + ARCHIVE
                + "\",\"sequence_index\":0,\"start_tick\":0}]"), body);
        assertTrue(body.contains("\"metadata\":{\"acme:arena\":\"sky-fortress\"}"), body);
    }

    @Test
    void createCollectionNeverGeneratesAnIdempotencyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> CollectionRequest.builder(null, SERVER, "a", "b", "c", policy()));
        assertThrows(IllegalArgumentException.class,
                () -> CollectionRequest.builder("  ", SERVER, "a", "b", "c", policy()));
        assertThrows(IllegalArgumentException.class,
                () -> CollectionRequest.builder("k\r\nX: y", SERVER, "a", "b", "c", policy()));
        char[] tooLong = new char[201];
        Arrays.fill(tooLong, 'k');
        assertThrows(IllegalArgumentException.class,
                () -> CollectionRequest.builder(new String(tooLong), SERVER, "a", "b", "c", policy()));
    }

    @Test
    void updateCollectionSendsKeyAndBody() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("operation.json"));
        CollectionUpdateRequest request = CollectionUpdateRequest.builder("update-1")
                .participantJoined(ScopeParticipant.builder(PLAYER, "Notch").role("spectator").build())
                .participantLeft("853c80ef-3c37-49fd-aa49-938b674adae6", 900)
                .metadata(Collections.singletonMap("acme:round", "2"))
                .build();

        ScopeOperation operation = clientWith(transport).updateCollection(COLLECTION, request);

        assertEquals(OperationStatus.COMPLETED, operation.getStatus());
        assertEquals(ProcessingState.READY, operation.getProcessingState().get());
        assertEquals(COLLECTION, operation.getCollectionId().get());
        assertEquals(ASSET, operation.getAssetId().get());
        assertEquals(2L, operation.getAttemptGeneration());
        assertEquals("/v1/replay-assets/" + ASSET, operation.getResourceUrl().get());
        HttpRequest sent = transport.lastRequest();
        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION, sent.getUrl());
        assertEquals("update-1", sent.getHeaders().get("Idempotency-Key"));
        assertTrue(sent.getBody().contains("\"participants_left\":[{\"player_uuid\":"
                + "\"853c80ef-3c37-49fd-aa49-938b674adae6\",\"left_tick\":900}]"), sent.getBody());
        assertTrue(sent.getBody().contains("\"metadata\":{\"acme:round\":\"2\"}"), sent.getBody());
    }

    @Test
    void finalizeCollectionPostsToFinalizeWithKey() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("operation.json"));
        CollectionFinalizeRequest request = CollectionFinalizeRequest.builder("end-1", 12000)
                .team(ScopeTeam.builder("red").result("winner").placement(1).build())
                .segment(ScopeSegment.builder(ARCHIVE, 0, 0).endTick(12000).segmentFirstTick(0).build())
                .build();

        clientWith(transport).finalizeCollection(COLLECTION, request);

        HttpRequest sent = transport.lastRequest();
        assertEquals("POST", sent.getMethod());
        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION + "/finalize", sent.getUrl());
        assertEquals("end-1", sent.getHeaders().get("Idempotency-Key"));
        assertTrue(sent.getBody().startsWith("{\"end_tick\":12000"), sent.getBody());
        assertTrue(sent.getBody().contains("\"segments\":[{\"archive_id\":\"" + ARCHIVE
                + "\",\"sequence_index\":0,\"start_tick\":0,\"end_tick\":12000,\"segment_first_tick\":0}]"),
                sent.getBody());
    }

    @Test
    void holdReleaseAndRevokeSendOnlyTheKey() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("collection.json"))
                .enqueue(200, Fixtures.text("collection.json"))
                .enqueue(200, Fixtures.text("collection.json"));
        ReplayCoreClient client = clientWith(transport);

        client.holdCollection(COLLECTION, "hold-1");
        client.releaseCollection(COLLECTION, "release-1");
        ReplayCollection revoked = client.revokeCollection(COLLECTION, "revoke-1");

        assertEquals(COLLECTION, revoked.getId());
        List<HttpRequest> sent = transport.allRequests();
        String[] actions = {"hold", "release", "revoke"};
        String[] keys = {"hold-1", "release-1", "revoke-1"};
        for (int i = 0; i < 3; i++) {
            assertEquals("POST", sent.get(i).getMethod());
            assertEquals(BASE + "/v1/replay-collections/" + COLLECTION + "/" + actions[i], sent.get(i).getUrl());
            assertEquals(keys[i], sent.get(i).getHeaders().get("Idempotency-Key"));
            assertNull(sent.get(i).getBody());
            assertFalse(sent.get(i).getHeaders().containsKey("Content-Type"));
        }
        assertThrows(IllegalArgumentException.class, () -> client.holdCollection(COLLECTION, " "));
        assertThrows(IllegalArgumentException.class, () -> client.holdCollection("not-a-uuid", "k"));
    }

    @Test
    void embargoErrorsMapToTheExceptionHierarchy() {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(403, "{\"code\":\"INSUFFICIENT_SCOPE\",\"detail\":\"needs release:write\"}")
                .enqueue(409, "{\"code\":\"OPERATION_IN_PROGRESS\",\"detail\":\"already being processed\"}")
                .enqueue(404, "{\"code\":\"NOT_FOUND\",\"detail\":\"collection not found\"}");
        ReplayCoreClient client = clientWith(transport);

        AuthorizationException forbidden = assertThrows(AuthorizationException.class,
                () -> client.holdCollection(COLLECTION, "k"));
        assertEquals("INSUFFICIENT_SCOPE", forbidden.getCode());
        ReplayCoreApiException conflict = assertThrows(ReplayCoreApiException.class,
                () -> client.holdCollection(COLLECTION, "k"));
        assertEquals(409, conflict.getStatusCode());
        assertEquals("OPERATION_IN_PROGRESS", conflict.getCode());
        assertThrows(NotFoundException.class, () -> client.revokeCollection(COLLECTION, "k"));
    }

    // ---------------------------------------------------------------- catalogue

    @Test
    void listCollectionsEncodesFiltersAndParsesThePage() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("collection-page.json"));
        CollectionQuery query = CollectionQuery.builder()
                .pageSize(20)
                .category("bedwars")
                .serverId(SERVER)
                .startedAfter(Instant.parse("2026-10-01T00:00:00Z"))
                .processingState(ProcessingState.READY)
                .assetKind(AssetKind.EVENT_CLIP)
                .playable(true)
                .customFacet("acme:region", "eu west")
                .build();

        CatalogPage<ReplayCollection> page = clientWith(transport).listCollections(query);

        assertEquals(1, page.getResults().size());
        assertTrue(page.hasNextPage());
        assertEquals("Y3Vyc29yLTI", page.getNextPageToken().get());
        assertEquals(20, page.getPageSize());
        HttpRequest sent = transport.lastRequest();
        assertEquals("GET", sent.getMethod());
        String url = sent.getUrl();
        assertTrue(url.startsWith(BASE + "/v1/replay-collections?page_size=20&category=bedwars"), url);
        assertTrue(url.contains("server_id=" + SERVER), url);
        assertTrue(url.contains("from=2026-10-01T00%3A00%3A00Z"), url);
        assertTrue(url.contains("processing_state=ready"), url);
        assertTrue(url.contains("asset_kind=EVENT_CLIP"), url);
        assertTrue(url.contains("playable=true"), url);
        assertTrue(url.contains("facet.acme%3Aregion=eu%20west"), url);
        assertFalse(sent.getHeaders().containsKey("Idempotency-Key"));
    }

    @Test
    void collectionQueryValidatesItsFilters() {
        assertThrows(IllegalArgumentException.class, () -> CollectionQuery.builder().serverId("nope"));
        assertThrows(IllegalArgumentException.class, () -> CollectionQuery.builder().pageSize(101));
        assertThrows(IllegalArgumentException.class, () -> CollectionQuery.builder().teamSize(0));
        assertThrows(IllegalArgumentException.class, () -> CollectionQuery.builder().category(" "));
        assertThrows(IllegalArgumentException.class,
                () -> CollectionQuery.builder().processingState(ProcessingState.UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> CollectionQuery.builder().customFacet("Bad Key", "v"));
    }

    @Test
    void getCollectionReadsOneWithOptionalPlayer() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("collection.json"))
                .enqueue(200, Fixtures.text("collection.json"))
                .enqueue(404, "{\"code\":\"NOT_FOUND\",\"detail\":\"collection not found\"}");
        ReplayCoreClient client = clientWith(transport);

        client.getCollection(COLLECTION);
        client.getCollection(COLLECTION, PLAYER);

        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION, transport.allRequests().get(0).getUrl());
        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION + "?player_uuid=" + PLAYER,
                transport.allRequests().get(1).getUrl());
        assertThrows(NotFoundException.class, () -> client.getCollection(COLLECTION));
    }

    @Test
    void listCollectionAssetsParsesAssets() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("player-replays.json"));

        CatalogPage<ReplayAsset> page = clientWith(transport).listCollectionAssets(COLLECTION,
                AssetQuery.builder().assetKind(AssetKind.EVENT_CLIP).eventKind(EventKind.KILL)
                        .playerUuid(PLAYER).build());

        ReplayAsset asset = page.getResults().get(0);
        assertEquals(ASSET, asset.getId());
        assertEquals(AssetKind.EVENT_CLIP, asset.getKind());
        assertEquals(EventKind.KILL, asset.getEventKind().get());
        assertEquals(Long.valueOf(3000), asset.getStartTick().get());
        assertEquals(2, asset.getParticipants().size());
        assertEquals(AssetRelation.DEATH, asset.getParticipants().get(1).getRelation());
        assertTrue(asset.getWatchUrl().get().startsWith("https://watch.replaycore.com/w/"));
        String url = transport.lastRequest().getUrl();
        assertTrue(url.startsWith(BASE + "/v1/replay-collections/" + COLLECTION + "/assets?"), url);
        assertTrue(url.contains("asset_kind=EVENT_CLIP&event_kind=KILL&player_uuid=" + PLAYER), url);
    }

    @Test
    void listCollectionTimelineEventsParsesMarkers() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("collection-markers.json"));

        List<ScopeMarker> markers = clientWith(transport).listCollectionTimelineEvents(COLLECTION);

        assertEquals(1, markers.size());
        assertEquals("Bed destroyed", markers.get(0).getLabel());
        assertEquals(1, markers.get(0).getSegmentSequenceIndex());
        assertEquals(ARCHIVE, markers.get(0).getArchiveId());
        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION + "/timeline-events",
                transport.lastRequest().getUrl());
    }

    @Test
    void getCollectionFacetsParsesCounts() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("collection-facets.json"));

        CatalogFacets facets = clientWith(transport).getCollectionFacets();

        assertEquals("bedwars", facets.getCategory().get(0).getValue());
        assertEquals(42L, facets.getCategory().get(0).getCount());
        assertEquals("4", facets.getTeamSize().get(0).getValue());
        assertEquals(BASE + "/v1/replay-collections/facets", transport.lastRequest().getUrl());
    }

    @Test
    void bulkLookupPostsBothListsAndEnforcesItsBounds() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("collection-bulk-lookup.json"));
        ReplayCoreClient client = clientWith(transport);

        List<ReplayCollection> found = client.bulkLookupCollections(
                Arrays.asList("match-1", "match-2"), Arrays.asList(COLLECTION));

        assertEquals(1, found.size());
        HttpRequest sent = transport.lastRequest();
        assertEquals("POST", sent.getMethod());
        assertEquals(BASE + "/v1/replay-collections/bulk-lookup", sent.getUrl());
        assertEquals("{\"external_match_ids\":[\"match-1\",\"match-2\"],\"collection_ids\":[\"" + COLLECTION + "\"]}",
                sent.getBody());
        assertFalse(sent.getHeaders().containsKey("Idempotency-Key"));
        assertThrows(IllegalArgumentException.class, () -> client.bulkLookupCollections(null, null));
        String[] many = new String[101];
        Arrays.fill(many, "m");
        assertThrows(IllegalArgumentException.class,
                () -> client.bulkLookupCollections(Arrays.asList(many), null));
    }

    // ---------------------------------------------------------------- assets

    @Test
    void createAssetSendsKeyAndBody() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(201, Fixtures.text("operation.json"));
        AssetRequest request = AssetRequest.builder("clip-1", AssetKind.EVENT_CLIP, SEGMENT, 3000, 3400)
                .eventKind(EventKind.KILL)
                .relationship(PLAYER, AssetRelation.KILL)
                .build();

        ScopeOperation operation = clientWith(transport).createAsset(COLLECTION, request);

        assertEquals(ASSET, operation.getAssetId().get());
        HttpRequest sent = transport.lastRequest();
        assertEquals(BASE + "/v1/replay-collections/" + COLLECTION + "/assets", sent.getUrl());
        assertEquals("clip-1", sent.getHeaders().get("Idempotency-Key"));
        assertEquals("{\"kind\":\"EVENT_CLIP\",\"event_kind\":\"KILL\",\"segment_id\":\"" + SEGMENT
                + "\",\"start_tick\":3000,\"end_tick\":3400,\"relationships\":[{\"player_uuid\":\"" + PLAYER
                + "\",\"relation\":\"KILL\"}]}", sent.getBody());
    }

    @Test
    void assetRequestRefusesTheFullMatchKindAndBadWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> AssetRequest.builder("k", AssetKind.FULL_MATCH, SEGMENT, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> AssetRequest.builder("k", AssetKind.EVENT_CLIP, SEGMENT, 10, 5));
        assertThrows(IllegalArgumentException.class,
                () -> AssetRequest.builder("k", AssetKind.EVENT_CLIP, "nope", 0, 5));
    }

    @Test
    void finalizeAssetReportsReadyOrFailed() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("operation.json"))
                .enqueue(200, Fixtures.text("operation.json"));
        ReplayCoreClient client = clientWith(transport);

        client.finalizeAsset(ASSET, AssetFinalizeRequest.ready("ready-1"));
        client.finalizeAsset(ASSET, AssetFinalizeRequest.failed("failed-1", "ENCODE_FAILED"));

        HttpRequest ready = transport.allRequests().get(0);
        assertEquals(BASE + "/v1/replay-assets/" + ASSET + "/finalize", ready.getUrl());
        assertEquals("ready-1", ready.getHeaders().get("Idempotency-Key"));
        assertEquals("{\"processing_state\":\"ready\"}", ready.getBody());
        HttpRequest failed = transport.allRequests().get(1);
        assertEquals("failed-1", failed.getHeaders().get("Idempotency-Key"));
        assertEquals("{\"processing_state\":\"failed\",\"failure_code\":\"ENCODE_FAILED\"}", failed.getBody());
        assertThrows(IllegalArgumentException.class, () -> AssetFinalizeRequest.failed("k", " "));
    }

    @Test
    void batchWritesSendOneKeyAndReturnPositionalResults() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("scope-batch.json"))
                .enqueue(200, Fixtures.text("scope-batch.json"));
        ReplayCoreClient client = clientWith(transport);

        List<BatchEntryResult> markers = client.createTimelineEventsBatch("batch-1", Arrays.asList(
                ScopeMarkerEntry.builder(COLLECTION, ARCHIVE, "evt-1", 4200, "Bed destroyed")
                        .sequenceIndex(1).category("objective").integration("bedwars").build()));
        List<BatchEntryResult> clips = client.createAssetsBatch("batch-2", Arrays.asList(
                ScopeClipEntry.builder(COLLECTION, ARCHIVE, "evt-2", EventKind.KILL, 10, 20)
                        .relationship(PLAYER, AssetRelation.KILL).build()));

        assertEquals(3, markers.size());
        assertEquals(BatchEntryStatus.REJECTED, markers.get(0).getStatus());
        assertEquals("INVALID_ENTRY", markers.get(0).getCode().get());
        assertEquals(BatchEntryStatus.ACCEPTED, markers.get(1).getStatus());
        assertFalse(markers.get(1).getCode().isPresent());
        assertEquals(BatchEntryStatus.RETRYABLE, markers.get(2).getStatus());
        assertEquals(3, clips.size());

        HttpRequest first = transport.allRequests().get(0);
        assertEquals(BASE + "/v1/timeline-events/batch", first.getUrl());
        assertEquals("batch-1", first.getHeaders().get("Idempotency-Key"));
        assertEquals("{\"entries\":[{\"collection_id\":\"" + COLLECTION + "\",\"archive_id\":\"" + ARCHIVE
                + "\",\"segment_sequence_index\":1,\"client_event_id\":\"evt-1\",\"tick\":4200,"
                + "\"label\":\"Bed destroyed\",\"category\":\"objective\",\"integration\":\"bedwars\"}]}",
                first.getBody());
        HttpRequest second = transport.allRequests().get(1);
        assertEquals(BASE + "/v1/replay-assets/batch", second.getUrl());
        assertEquals("batch-2", second.getHeaders().get("Idempotency-Key"));
        assertTrue(second.getBody().contains("\"kind\":\"EVENT_CLIP\",\"event_kind\":\"KILL\""), second.getBody());

        assertThrows(IllegalArgumentException.class,
                () -> client.createTimelineEventsBatch("k", Collections.<ScopeMarkerEntry>emptyList()));
        assertThrows(IllegalArgumentException.class, () -> client.createAssetsBatch(" ", Collections.<ScopeClipEntry>emptyList()));
    }

    @Test
    void getAssetParsesRelationships() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("asset.json"));

        ReplayAsset asset = clientWith(transport).getAsset(ASSET);

        assertEquals(1, asset.getRelationships().size());
        assertEquals(AssetRelation.KILL, asset.getRelationships().get(0).getRelation());
        assertEquals(AssetRelation.KILL, asset.getViewerRelation().get());
        assertTrue(asset.isPlaybackEligible());
        assertEquals(BASE + "/v1/replay-assets/" + ASSET, transport.lastRequest().getUrl());
    }

    @Test
    void listPlayerReplaysUsesThePlayerPath() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("player-replays.json"));

        CatalogPage<ReplayAsset> page = clientWith(transport).listPlayerReplays(PLAYER,
                PlayerReplayQuery.builder().collectionId(COLLECTION).mode("solo").playable(true).build());

        assertEquals(1, page.getResults().size());
        assertEquals(BASE + "/v1/players/" + PLAYER + "/replays?collection_id=" + COLLECTION
                + "&mode=solo&playable=true", transport.lastRequest().getUrl());
    }

    // ---------------------------------------------------------------- playback

    @Test
    void issueWatchTicketSendsViewerAndNoKey() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("watch-ticket.json"));

        WatchTicket ticket = clientWith(transport).issueWatchTicket(ASSET, PLAYER);

        assertFalse(ticket.isPreparing());
        assertEquals(ASSET, ticket.getAssetId());
        assertEquals(64, ticket.getTicket().get().length());
        assertEquals(1, ticket.getPlayback().size());
        assertEquals(ARCHIVE, ticket.getPlayback().get(0).getReplayId());
        assertEquals(Long.valueOf(300), ticket.getExpiresIn().get());
        assertTrue(ticket.isSingleUse());
        HttpRequest sent = transport.lastRequest();
        assertEquals("POST", sent.getMethod());
        assertEquals(BASE + "/v1/replay-assets/" + ASSET + "/watch-ticket", sent.getUrl());
        assertEquals("{\"viewer_player_uuid\":\"" + PLAYER + "\"}", sent.getBody());
        assertFalse(sent.getHeaders().containsKey("Idempotency-Key"));
    }

    @Test
    void issueWatchTicketReportsAnAssetThatIsStillPreparing() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(202, Fixtures.text("watch-ticket-preparing.json"));

        WatchTicket ticket = clientWith(transport).issueWatchTicket(ASSET, null);

        assertTrue(ticket.isPreparing());
        assertFalse(ticket.getTicket().isPresent());
        assertEquals(Integer.valueOf(5), ticket.getRetryAfterSeconds().get());
        assertEquals(ProcessingState.PROCESSING, ticket.getProcessingState().get());
        assertNull(transport.lastRequest().getBody());
    }

    @Test
    void redeemWatchTicketSendsTheTicketAndParsesSegments() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport().enqueue(200, Fixtures.text("watch-ticket-redeem.json"));

        WatchTicketRedemption redemption = clientWith(transport).redeemWatchTicket(ASSET, "abc123", PLAYER);

        assertEquals(400L, redemption.getTotalTicks());
        assertEquals(1, redemption.getSegments().size());
        assertEquals(120L, redemption.getSegments().get(0).getExpiresIn());
        HttpRequest sent = transport.lastRequest();
        assertEquals(BASE + "/v1/replay-assets/" + ASSET + "/watch-ticket/redeem", sent.getUrl());
        assertEquals("{\"ticket\":\"abc123\",\"viewer_player_uuid\":\"" + PLAYER + "\"}", sent.getBody());
        assertThrows(IllegalArgumentException.class, () -> clientWith(transport).redeemWatchTicket(ASSET, " ", null));
    }

    @Test
    void createWatchLinkParsesTheLinkAndMapsTierRefusal() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(201, Fixtures.text("watch-link.json"))
                .enqueue(403, "{\"code\":\"FORBIDDEN_TIER\",\"detail\":\"upgrade to unlock\"}");
        ReplayCoreClient client = clientWith(transport);

        WatchLink link = client.createWatchLink(ASSET);

        assertEquals("0f1e2d3c4b5a69788796a5b4c3d2e1f0", link.getToken());
        assertEquals(Instant.parse("2026-12-28T12:00:00Z"), link.getExpiresAt().get());
        assertEquals(BASE + "/v1/replay-assets/" + ASSET + "/watch-link", transport.lastRequest().getUrl());
        assertNull(transport.lastRequest().getBody());
        AuthorizationException refused = assertThrows(AuthorizationException.class, () -> client.createWatchLink(ASSET));
        assertEquals("FORBIDDEN_TIER", refused.getCode());
    }

    @Test
    void replayWebhookDeliveryPostsWithoutBody() throws ReplayCoreException {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("webhook-delivery-replay.json"))
                .enqueue(409, "{\"code\":\"DELIVERY_NOT_REPLAYABLE\",\"detail\":\"already succeeded\"}");
        ReplayCoreClient client = clientWith(transport);
        String id = "12345678-1234-4234-8234-123456789abc";

        WebhookDeliveryReplay replay = client.replayWebhookDelivery(id);

        assertEquals(id, replay.getDeliveryId());
        assertEquals("delivered", replay.getStatus());
        assertEquals(BASE + "/v1/webhooks/deliveries/" + id + "/replay", transport.lastRequest().getUrl());
        assertNull(transport.lastRequest().getBody());
        ReplayCoreApiException conflict = assertThrows(ReplayCoreApiException.class, () -> client.replayWebhookDelivery(id));
        assertEquals("DELIVERY_NOT_REPLAYABLE", conflict.getCode());
    }

    // ---------------------------------------------------------------- async twins

    @Test
    void asyncTwinsDelegateToTheSynchronousCalls() throws Exception {
        RecordingTransport transport = new RecordingTransport()
                .enqueue(200, Fixtures.text("collection.json"))
                .enqueue(200, Fixtures.text("collection-page.json"))
                .enqueue(200, Fixtures.text("watch-link.json"));
        ReplayCoreAsyncClient async = new ReplayCoreAsyncClient(clientWith(transport));

        ReplayCollection held = async.holdCollection(COLLECTION, "hold-async").get();
        CatalogPage<ReplayCollection> page = async.listCollections(CollectionQuery.builder().build()).get();
        WatchLink link = async.createWatchLink(ASSET).get();

        assertEquals(COLLECTION, held.getId());
        assertEquals(1, page.getResults().size());
        assertEquals("0f1e2d3c4b5a69788796a5b4c3d2e1f0", link.getToken());
        assertEquals("hold-async", transport.allRequests().get(0).getHeaders().get("Idempotency-Key"));
    }
}
