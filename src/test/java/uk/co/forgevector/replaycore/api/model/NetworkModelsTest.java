/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import uk.co.forgevector.replaycore.api.Fixtures;
import uk.co.forgevector.replaycore.api.internal.ModelMapper;

class NetworkModelsTest {

    private static final String SESSION = "5e55105e-0000-4000-8000-000000000001";

    @Test
    void replayQuerySendsSessionIdAndValidatesIt() {
        assertEquals(SESSION, ReplayQuery.builder().sessionId(SESSION.toUpperCase()).build()
                .toQueryParameters().get("session_id"));
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().sessionId("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> ReplayQuery.builder().sessionId(" "));
    }

    @Test
    void replayMetadataExposesSessionFieldsFromTheCloudFixture() {
        ReplayMetadata replay = ModelMapper.toReplayMetadata(Fixtures.object("replay.json"));

        assertTrue(replay.isSegmented());
        assertEquals(SESSION, replay.getSessionId().get());
        assertEquals(0, replay.getSegmentSeq().getAsInt());
        assertEquals(1_800_000L, replay.getSessionDurationMs().get().longValue());
        assertEquals("00000000-0000-4000-8000-0000000000b1", replay.getRecordingSessionId().get());
        assertTrue(replay.hasThumbnail());
        assertTrue(replay.hasPrivateThumbnail());
        assertEquals(Quality.HIGH, replay.getQuality());
    }

    @Test
    void anUnsegmentedReplayCarriesItsOwnIdAsSessionId() {
        ReplayMetadata replay = ModelMapper.toReplayMetadata(uk.co.forgevector.replaycore.api.internal.Json.parseObject(
                "{\"id\":\"a\",\"session_id\":\"a\"}"));

        assertFalse(replay.isSegmented());
        assertEquals("a", replay.getSessionId().get());
        assertFalse(replay.getSegmentSeq().isPresent());
        assertFalse(replay.getSessionDurationMs().isPresent());
    }

    @Test
    void timelineEventRequestSendsAValidatedIcon() {
        TimelineEventRequest request = TimelineEventRequest.forReplay(SESSION).tick(5).label("Bed")
                .icon(" Bed ").integration("bedwars").build();

        assertEquals("bed", request.toBody().get("icon"));
        assertEquals("bedwars", request.toBody().get("integration"));
        assertThrows(IllegalArgumentException.class, () -> TimelineEventRequest.forReplay(SESSION).icon("rocket"));
        assertThrows(IllegalArgumentException.class, () -> TimelineEventRequest.forReplay(SESSION).integration("Bad Name"));
        assertFalse(TimelineEventRequest.forReplay(SESSION).tick(5).label("x").build().toBody().containsKey("icon"));
        assertEquals(15, TimelineEventRequest.ICONS.size());
    }

    @Test
    void timelineMarkerExposesIconAndIntegration() {
        TimelineMarker marker = ModelMapper.toTimelineMarker(Fixtures.object("timeline-marker.json"));

        assertEquals("skull", marker.getIcon().get());
        assertEquals("vulcan", marker.getIntegration().get());
    }

    @Test
    void serverInstanceExposesCountsVersionAndUpdateAdvice() {
        List<ServerInstance> servers = ModelMapper.toServerInstances(Fixtures.object("servers.json"));

        ServerInstance server = servers.get(0);
        assertEquals(37, server.getPlayerCount().get().intValue());
        assertEquals(1284L, server.getReplayCount());
        assertEquals("1.21.4", server.getMinecraftVersion().get());
        assertEquals(PluginUpdateStatus.CURRENT, server.getUpdate().get().getStatus());
        assertTrue(server.getUpdate().get().isLoaderAvailable());
    }

    @Test
    void enumsMapKnownAndFutureValues() {
        assertEquals(ProcessingState.FINALIZING, ProcessingState.fromWire("finalizing"));
        assertEquals(ProcessingState.UNKNOWN, ProcessingState.fromWire("teleporting"));
        assertEquals(ReleaseState.REVOKED, ReleaseState.fromWire("revoked"));
        assertEquals(CollectionVisibility.STAFF_ONLY, CollectionVisibility.fromWire("staff_only"));
        assertEquals(AssetKind.FULL_MATCH, AssetKind.fromWire("FULL_MATCH"));
        assertEquals(AssetKind.UNKNOWN, AssetKind.fromWire("full_match"));
        assertEquals(EventKind.ROUND_END, EventKind.fromWire("ROUND_END"));
        assertEquals(AssetRelation.PARTICIPANT, AssetRelation.fromWire("PARTICIPANT"));
        assertEquals(OperationStatus.PENDING, OperationStatus.fromWire("pending"));
        assertEquals(BatchEntryStatus.RETRYABLE, BatchEntryStatus.fromWire("retryable"));
        assertEquals(BatchEntryStatus.UNKNOWN, BatchEntryStatus.fromWire(null));
        assertEquals(ReleaseTrigger.ASSET_READY, ReleaseTrigger.fromWire("asset_ready"));
        assertEquals(PluginUpdateStatus.MIGRATE_TO_LOADER, PluginUpdateStatus.fromWire("migrate_to_loader"));
        assertEquals(Quality.ULTRA, Quality.fromWire("ultra"));
    }

    @Test
    void releasePolicyRefusesUnknownSettings() {
        assertThrows(IllegalArgumentException.class, () -> ReleasePolicy.builder(
                ReleaseTrigger.UNKNOWN, ReleaseTrigger.EXPLICIT, CollectionVisibility.PUBLIC));
        assertThrows(IllegalArgumentException.class, () -> ReleasePolicy.builder(
                ReleaseTrigger.EXPLICIT, ReleaseTrigger.EXPLICIT, null));
        assertEquals("{\"full-replay\":{\"release-trigger\":\"explicit\"},\"event-clips\":"
                + "{\"release-trigger\":\"explicit\"},\"visibility\":\"private\"}",
                uk.co.forgevector.replaycore.api.internal.Json.write(ReleasePolicy.builder(
                        ReleaseTrigger.EXPLICIT, ReleaseTrigger.EXPLICIT, CollectionVisibility.PRIVATE)
                        .build().toBody()));
    }

    @Test
    void rosterBuildersValidateLocally() {
        String uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5";
        assertThrows(IllegalArgumentException.class, () -> ScopeParticipant.builder("x", "n"));
        assertThrows(IllegalArgumentException.class,
                () -> ScopeParticipant.builder(uuid, "n").joinedTick(10).leftTick(5).build());
        assertThrows(IllegalArgumentException.class, () -> ScopeTeam.builder("k").placement(0));
        assertThrows(IllegalArgumentException.class,
                () -> ScopeSegment.builder(uuid, 0, 10).endTick(5).build());
        assertThrows(IllegalArgumentException.class, () -> CollectionRequest
                .builder("k", uuid, "a", "b", "c", ReleasePolicy.builder(ReleaseTrigger.EXPLICIT,
                        ReleaseTrigger.EXPLICIT, CollectionVisibility.PUBLIC).build())
                .metadata(Collections.singletonMap("Bad Key", "v")));
        assertThrows(IllegalArgumentException.class, () -> ScopeMarkerEntry.builder(uuid, uuid, "e", -1, "l"));
        assertThrows(IllegalArgumentException.class,
                () -> ScopeClipEntry.builder(uuid, uuid, "e", EventKind.UNKNOWN, 0, 1));
        assertEquals(Arrays.asList("a"), Arrays.asList("a"));
    }
}
