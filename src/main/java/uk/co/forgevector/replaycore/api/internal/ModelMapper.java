/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.internal;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.forgevector.replaycore.api.model.ArchiveStatus;
import uk.co.forgevector.replaycore.api.model.AssetKind;
import uk.co.forgevector.replaycore.api.model.AssetParticipant;
import uk.co.forgevector.replaycore.api.model.AssetRelation;
import uk.co.forgevector.replaycore.api.model.AssetRelationship;
import uk.co.forgevector.replaycore.api.model.BatchEntryResult;
import uk.co.forgevector.replaycore.api.model.BatchEntryStatus;
import uk.co.forgevector.replaycore.api.model.CatalogFacets;
import uk.co.forgevector.replaycore.api.model.CatalogPage;
import uk.co.forgevector.replaycore.api.model.CollectionParticipant;
import uk.co.forgevector.replaycore.api.model.CollectionTeam;
import uk.co.forgevector.replaycore.api.model.CollectionVisibility;
import uk.co.forgevector.replaycore.api.model.EventKind;
import uk.co.forgevector.replaycore.api.model.FacetCount;
import uk.co.forgevector.replaycore.api.model.OperationStatus;
import uk.co.forgevector.replaycore.api.model.Participant;
import uk.co.forgevector.replaycore.api.model.PlaybackSegment;
import uk.co.forgevector.replaycore.api.model.PluginUpdate;
import uk.co.forgevector.replaycore.api.model.PluginUpdateStatus;
import uk.co.forgevector.replaycore.api.model.ProcessingState;
import uk.co.forgevector.replaycore.api.model.Quality;
import uk.co.forgevector.replaycore.api.model.RedeemedSegment;
import uk.co.forgevector.replaycore.api.model.ReleaseState;
import uk.co.forgevector.replaycore.api.model.ReplayAsset;
import uk.co.forgevector.replaycore.api.model.ReplayCollection;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ScopeMarker;
import uk.co.forgevector.replaycore.api.model.ScopeOperation;
import uk.co.forgevector.replaycore.api.model.ServerInstance;
import uk.co.forgevector.replaycore.api.model.ServerStatus;
import uk.co.forgevector.replaycore.api.model.StorageTier;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;
import uk.co.forgevector.replaycore.api.model.Visibility;
import uk.co.forgevector.replaycore.api.model.WatchLink;
import uk.co.forgevector.replaycore.api.model.WatchTicket;
import uk.co.forgevector.replaycore.api.model.WatchTicketRedemption;
import uk.co.forgevector.replaycore.api.model.WebhookDeliveryReplay;

/**
 * Converts parsed JSON trees (the {@link Map}/{@link List}/scalar shape that
 * {@link Json} produces) into the SDK's immutable model types.
 *
 * <p>This is an internal helper. It is deliberately lenient about field presence:
 * missing optional fields map to absent {@link java.util.Optional}s, and
 * unknown enum values map to the relevant {@code UNKNOWN} constant, so a
 * forward-compatible server response never breaks an older SDK build.
 */
public final class ModelMapper {

    private ModelMapper() {
    }

    /**
     * Maps a replay metadata object.
     *
     * @param obj the parsed JSON object for one replay
     * @return the immutable {@link ReplayMetadata}
     */
    public static ReplayMetadata toReplayMetadata(Map<String, Object> obj) {
        String id = str(obj, "id");
        if (id == null) {
            throw new JsonParseException("replay metadata is missing the 'id' field");
        }
        ReplayMetadata.Builder b = ReplayMetadata.builder(id)
                .tenantId(str(obj, "tenant_id"))
                .serverId(str(obj, "server_id"))
                .serverName(str(obj, "server_name"))
                .displayName(str(obj, "display_name"))
                .integration(str(obj, "integration"))
                .quality(Quality.fromWire(str(obj, "quality")))
                .startedAt(instant(obj, "started_at"))
                .endedAt(instant(obj, "ended_at"))
                .durationMs(longOrNull(obj, "duration_ms"))
                .sizeBytes(longOrNull(obj, "size_bytes"))
                .storageTier(StorageTier.fromWire(str(obj, "storage_tier")))
                .retentionUntil(instant(obj, "retention_until"))
                .visibility(Visibility.fromWire(str(obj, "visibility")))
                .signatureKid(str(obj, "signature_kid"))
                .manifestHash(str(obj, "manifest_hash"))
                .hasStaffArchive(bool(obj, "has_staff_archive"))
                .redactedFrom(str(obj, "redacted_from"))
                .archiveStatus(ArchiveStatus.fromWire(str(obj, "archive_status")))
                .crashFinalised(bool(obj, "crash_finalised"))
                .starred(bool(obj, "starred"))
                .recoveryStatus(str(obj, "recovery_status"))
                .formatVersion((int) longValue(obj, "format_version", 0L))
                .storageMode(str(obj, "storage_mode"))
                .sessionId(str(obj, "session_id"))
                .segmentSeq(intOrNull(obj, "segment_seq"))
                .sessionDurationMs(longOrNull(obj, "session_duration_ms"))
                .recordingSessionId(str(obj, "recording_session_id"))
                .hasThumbnail(bool(obj, "has_thumbnail"))
                .hasPrivateThumbnail(bool(obj, "has_private_thumbnail"));

        Object participants = obj.get("participants");
        if (participants instanceof List) {
            List<Participant> roster = new ArrayList<Participant>();
            for (Object element : (List<?>) participants) {
                if (element instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> p = (Map<String, Object>) element;
                    String uuid = str(p, "uuid");
                    if (uuid != null) {
                        roster.add(new Participant(uuid, str(p, "name"), str(p, "role")));
                    }
                }
            }
            b.participants(roster);
        }
        return b.build();
    }

    /**
     * Maps a paginated list-replays response.
     *
     * @param obj the parsed JSON object containing {@code results},
     *            {@code next_page_token} and {@code page_size}
     * @return the immutable {@link ReplayPage}
     */
    public static ReplayPage toReplayPage(Map<String, Object> obj) {
        List<ReplayMetadata> results = new ArrayList<ReplayMetadata>();
        Object raw = obj.get("results");
        if (raw instanceof List) {
            for (Object element : (List<?>) raw) {
                if (element instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) element;
                    results.add(toReplayMetadata(m));
                }
            }
        }
        String token = str(obj, "next_page_token");
        int pageSize = (int) longValue(obj, "page_size", results.size());
        return new ReplayPage(results, token, pageSize);
    }

    /**
     * Maps a connected-servers response.
     *
     * @param obj the parsed JSON object containing a {@code servers} array
     * @return the server instances in response order
     */
    public static List<ServerInstance> toServerInstances(Map<String, Object> obj) {
        Object raw = obj.get("servers");
        if (!(raw instanceof List)) {
            throw new JsonParseException("server list response is missing the 'servers' array");
        }
        List<ServerInstance> servers = new ArrayList<ServerInstance>();
        for (Object element : (List<?>) raw) {
            if (!(element instanceof Map)) {
                throw new JsonParseException("server list contains a non-object entry");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) element;
            servers.add(toServerInstance(server));
        }
        return java.util.Collections.unmodifiableList(servers);
    }

    /**
     * Maps one connected-server response.
     *
     * @param server the parsed server object
     * @return one immutable server instance
     */
    public static ServerInstance toServerInstance(Map<String, Object> server) {
        String id = str(server, "id");
        String name = str(server, "name");
        if (id == null || name == null) {
            throw new JsonParseException("server response is missing 'id' or 'name'");
        }
        return new ServerInstance(
                id,
                name,
                ServerStatus.fromWire(str(server, "status")),
                instant(server, "lastSeenAt"),
                str(server, "pluginVersion"),
                str(server, "minecraftVersion"),
                toPluginUpdate(server.get("update")),
                intOrNull(server, "playerCount"),
                longValue(server, "replayCount", 0L));
    }

    /**
     * Maps a timeline-marker creation response.
     *
     * @param obj the parsed JSON object for the created marker
     * @return the immutable {@link TimelineMarker}
     */
    public static TimelineMarker toTimelineMarker(Map<String, Object> obj) {
        String id = str(obj, "id");
        String replayId = str(obj, "replayId");
        if (id == null || replayId == null) {
            throw new JsonParseException("timeline marker response is missing 'id' or 'replayId'");
        }
        return new TimelineMarker(
                id,
                replayId,
                longValue(obj, "tick", 0L),
                strOrEmpty(obj, "label"),
                str(obj, "category"),
                str(obj, "colour"),
                str(obj, "icon"),
                str(obj, "actor"),
                str(obj, "integration"),
                instant(obj, "createdAt"));
    }

    /**
     * Maps a page of collections.
     *
     * @param obj the parsed JSON object containing {@code results},
     *            {@code next_page_token} and {@code page_size}
     * @return the immutable page
     */
    public static CatalogPage<ReplayCollection> toCollectionPage(Map<String, Object> obj) {
        List<ReplayCollection> results = new ArrayList<ReplayCollection>();
        for (Map<String, Object> m : objects(obj, "results")) {
            results.add(toReplayCollection(m));
        }
        return new CatalogPage<ReplayCollection>(results, str(obj, "next_page_token"),
                (int) longValue(obj, "page_size", results.size()));
    }

    /**
     * Maps a page of assets.
     *
     * @param obj the parsed JSON object containing {@code results},
     *            {@code next_page_token} and {@code page_size}
     * @return the immutable page
     */
    public static CatalogPage<ReplayAsset> toAssetPage(Map<String, Object> obj) {
        List<ReplayAsset> results = new ArrayList<ReplayAsset>();
        for (Map<String, Object> m : objects(obj, "results")) {
            results.add(toReplayAsset(m));
        }
        return new CatalogPage<ReplayAsset>(results, str(obj, "next_page_token"),
                (int) longValue(obj, "page_size", results.size()));
    }

    /**
     * Maps the bulk-lookup response.
     *
     * @param obj the parsed JSON object containing {@code results}
     * @return the collections in response order
     */
    public static List<ReplayCollection> toReplayCollections(Map<String, Object> obj) {
        List<ReplayCollection> results = new ArrayList<ReplayCollection>();
        for (Map<String, Object> m : objects(obj, "results")) {
            results.add(toReplayCollection(m));
        }
        return java.util.Collections.unmodifiableList(results);
    }

    /**
     * Maps one collection, either as the lifecycle calls return it or as the
     * catalogue returns it with rosters and playback flags.
     *
     * @param obj the parsed JSON object for one collection
     * @return the immutable {@link ReplayCollection}
     */
    public static ReplayCollection toReplayCollection(Map<String, Object> obj) {
        String id = str(obj, "id");
        if (id == null) {
            throw new JsonParseException("collection is missing the 'id' field");
        }
        List<CollectionParticipant> participants = new ArrayList<CollectionParticipant>();
        for (Map<String, Object> p : objects(obj, "participants")) {
            participants.add(toCollectionParticipant(p));
        }
        List<CollectionTeam> teams = new ArrayList<CollectionTeam>();
        for (Map<String, Object> t : objects(obj, "teams")) {
            teams.add(new CollectionTeam(
                    strOrEmpty(t, "id"),
                    strOrEmpty(t, "collection_id"),
                    strOrEmpty(t, "key"),
                    str(t, "name"),
                    str(t, "result"),
                    intOrNull(t, "placement"),
                    instant(t, "created_at"),
                    instant(t, "updated_at")));
        }
        return ReplayCollection.builder(id)
                .externalMatchId(str(obj, "external_match_id"))
                .category(strOrEmpty(obj, "category"))
                .mode(strOrEmpty(obj, "mode"))
                .policyId(strOrEmpty(obj, "policy_id"))
                .serverId(strOrEmpty(obj, "server_id"))
                .worldIds(strings(obj, "world_ids"))
                .startTick(longValue(obj, "start_tick", 0L))
                .endTick(longOrNull(obj, "end_tick"))
                .startedAt(instant(obj, "started_at"))
                .endedAt(instant(obj, "ended_at"))
                .metadata(stringMap(obj, "metadata"))
                .competitionId(str(obj, "competition_id"))
                .seriesId(str(obj, "series_id"))
                .roundId(str(obj, "round_id"))
                .bracketMatchId(str(obj, "bracket_match_id"))
                .gameNumber(intOrNull(obj, "game_number"))
                .bestOf(intOrNull(obj, "best_of"))
                .partyEventId(str(obj, "party_event_id"))
                .region(str(obj, "region"))
                .teamSize(intOrNull(obj, "team_size"))
                .ranked(boolOrNull(obj, "ranked"))
                .tournament(boolOrNull(obj, "tournament"))
                .processingState(ProcessingState.fromWire(str(obj, "processing_state")))
                .releaseState(ReleaseState.fromWire(str(obj, "release_state")))
                .visibility(CollectionVisibility.fromWire(str(obj, "visibility")))
                .createdAt(instant(obj, "created_at"))
                .updatedAt(instant(obj, "updated_at"))
                .participants(participants)
                .teams(teams)
                .viewerRelation(obj.containsKey("viewer_relation") && str(obj, "viewer_relation") != null
                        ? AssetRelation.fromWire(str(obj, "viewer_relation")) : null)
                .durationTicks(longOrNull(obj, "duration_ticks"))
                .thumbnailReady(bool(obj, "thumbnail_ready"))
                .playbackEligible(bool(obj, "playback_eligible"))
                .playable(bool(obj, "playable"))
                .mediaReady(bool(obj, "media_ready"))
                .expired(bool(obj, "expired"))
                .build();
    }

    private static CollectionParticipant toCollectionParticipant(Map<String, Object> p) {
        String id = str(p, "id");
        if (id == null) {
            throw new JsonParseException("collection participant is missing the 'id' field");
        }
        return CollectionParticipant.builder(id)
                .collectionId(strOrEmpty(p, "collection_id"))
                .playerUuid(strOrEmpty(p, "player_uuid"))
                .playerName(strOrEmpty(p, "player_name"))
                .teamKey(str(p, "team_key"))
                .role(strOrEmpty(p, "role"))
                .joinedTick(longValue(p, "joined_tick", 0L))
                .leftTick(longOrNull(p, "left_tick"))
                .result(str(p, "result"))
                .createdAt(instant(p, "created_at"))
                .updatedAt(instant(p, "updated_at"))
                .build();
    }

    /**
     * Maps one asset, either as the catalogue lists it or as a single read
     * returns it with its relationships.
     *
     * @param obj the parsed JSON object for one asset
     * @return the immutable {@link ReplayAsset}
     */
    public static ReplayAsset toReplayAsset(Map<String, Object> obj) {
        String id = str(obj, "id");
        if (id == null) {
            throw new JsonParseException("asset is missing the 'id' field");
        }
        List<AssetRelationship> relationships = new ArrayList<AssetRelationship>();
        for (Map<String, Object> r : objects(obj, "relationships")) {
            relationships.add(new AssetRelationship(
                    strOrEmpty(r, "id"),
                    strOrEmpty(r, "asset_id"),
                    strOrEmpty(r, "player_uuid"),
                    AssetRelation.fromWire(str(r, "relation")),
                    instant(r, "created_at")));
        }
        List<AssetParticipant> participants = new ArrayList<AssetParticipant>();
        for (Map<String, Object> p : objects(obj, "participants")) {
            participants.add(new AssetParticipant(
                    strOrEmpty(p, "player_uuid"), AssetRelation.fromWire(str(p, "relation"))));
        }
        return ReplayAsset.builder(id)
                .collectionId(strOrEmpty(obj, "collection_id"))
                .kind(AssetKind.fromWire(str(obj, "kind")))
                .eventKind(optionalWire(obj, "event_kind") != null
                        ? EventKind.fromWire(str(obj, "event_kind")) : null)
                .segmentId(str(obj, "segment_id"))
                .startTick(longOrNull(obj, "start_tick"))
                .endTick(longOrNull(obj, "end_tick"))
                .processingState(ProcessingState.fromWire(str(obj, "processing_state")))
                .releaseState(ReleaseState.fromWire(str(obj, "release_state")))
                .visibility(CollectionVisibility.fromWire(str(obj, "visibility")))
                .createdAt(instant(obj, "created_at"))
                .updatedAt(instant(obj, "updated_at"))
                .externalMatchId(str(obj, "external_match_id"))
                .collectionEndedAt(instant(obj, "collection_ended_at"))
                .gameplayBoundariesComplete(bool(obj, "gameplay_boundaries_complete"))
                .authoritativeMatchComplete(bool(obj, "authoritative_match_complete"))
                .relationships(relationships)
                .viewerRelation(optionalWire(obj, "viewer_relation") != null
                        ? AssetRelation.fromWire(str(obj, "viewer_relation")) : null)
                .participants(participants)
                .durationTicks(longOrNull(obj, "duration_ticks"))
                .thumbnailReady(bool(obj, "thumbnail_ready"))
                .playbackEligible(bool(obj, "playback_eligible"))
                .playable(bool(obj, "playable"))
                .mediaReady(bool(obj, "media_ready"))
                .expired(bool(obj, "expired"))
                .watchUrl(str(obj, "watch_url"))
                .build();
    }

    /**
     * Maps the response of an idempotent collection or asset operation.
     *
     * @param obj the parsed JSON object for one operation
     * @return the immutable {@link ScopeOperation}
     */
    public static ScopeOperation toScopeOperation(Map<String, Object> obj) {
        String id = str(obj, "id");
        if (id == null) {
            throw new JsonParseException("operation response is missing the 'id' field");
        }
        return ScopeOperation.builder(id)
                .collectionId(str(obj, "collection_id"))
                .assetId(str(obj, "asset_id"))
                .status(OperationStatus.fromWire(str(obj, "status")))
                .processingState(optionalWire(obj, "processing_state") != null
                        ? ProcessingState.fromWire(str(obj, "processing_state")) : null)
                .failureCode(str(obj, "failure_code"))
                .retryable(bool(obj, "retryable"))
                .attemptGeneration(longValue(obj, "attempt_generation", 0L))
                .playableAt(instant(obj, "playable_at"))
                .resourceUrl(str(obj, "resource_url"))
                .watchUrl(str(obj, "watch_url"))
                .watchTicketEligible(bool(obj, "watch_ticket_eligible"))
                .linkReady(bool(obj, "link_ready"))
                .playable(bool(obj, "playable"))
                .build();
    }

    /**
     * Maps a collection-markers response.
     *
     * @param obj the parsed JSON object containing {@code results}
     * @return the markers in response order
     */
    public static List<ScopeMarker> toScopeMarkers(Map<String, Object> obj) {
        List<ScopeMarker> markers = new ArrayList<ScopeMarker>();
        for (Map<String, Object> m : objects(obj, "results")) {
            String id = str(m, "id");
            if (id == null) {
                throw new JsonParseException("collection marker is missing the 'id' field");
            }
            markers.add(ScopeMarker.builder(id)
                    .collectionId(strOrEmpty(m, "collection_id"))
                    .segmentId(strOrEmpty(m, "segment_id"))
                    .clientEventId(str(m, "client_event_id"))
                    .tick(longValue(m, "tick", 0L))
                    .label(strOrEmpty(m, "label"))
                    .category(str(m, "category"))
                    .actor(str(m, "actor"))
                    .integration(str(m, "integration"))
                    .createdAt(instant(m, "created_at"))
                    .segmentSequenceIndex((int) longValue(m, "segment_sequence_index", 0L))
                    .archiveId(strOrEmpty(m, "archive_id"))
                    .build());
        }
        return java.util.Collections.unmodifiableList(markers);
    }

    /**
     * Maps the catalogue facets response.
     *
     * @param obj the parsed JSON object with one count list per dimension
     * @return the immutable {@link CatalogFacets}
     */
    public static CatalogFacets toCatalogFacets(Map<String, Object> obj) {
        return CatalogFacets.builder()
                .category(facetCounts(obj, "category"))
                .mode(facetCounts(obj, "mode"))
                .assetKind(facetCounts(obj, "asset_kind"))
                .eventKind(facetCounts(obj, "event_kind"))
                .viewerRelation(facetCounts(obj, "viewer_relation"))
                .serverId(facetCounts(obj, "server_id"))
                .region(facetCounts(obj, "region"))
                .teamSize(facetCounts(obj, "team_size"))
                .ranked(facetCounts(obj, "ranked"))
                .tournament(facetCounts(obj, "tournament"))
                .competitionId(facetCounts(obj, "competition_id"))
                .build();
    }

    private static List<FacetCount> facetCounts(Map<String, Object> obj, String key) {
        List<FacetCount> counts = new ArrayList<FacetCount>();
        for (Map<String, Object> c : objects(obj, key)) {
            counts.add(new FacetCount(strOrEmpty(c, "value"), longValue(c, "count", 0L)));
        }
        return counts;
    }

    /**
     * Maps a batch-write response.
     *
     * @param obj the parsed JSON object containing {@code results}
     * @return one result per request entry, in request order
     */
    public static List<BatchEntryResult> toBatchResults(Map<String, Object> obj) {
        if (!(obj.get("results") instanceof List)) {
            throw new JsonParseException("batch response is missing the 'results' array");
        }
        List<BatchEntryResult> results = new ArrayList<BatchEntryResult>();
        for (Map<String, Object> r : objects(obj, "results")) {
            results.add(new BatchEntryResult(
                    BatchEntryStatus.fromWire(str(r, "status")), str(r, "code"), str(r, "detail")));
        }
        return java.util.Collections.unmodifiableList(results);
    }

    /**
     * Maps a watch-ticket response, which is either an issued ticket or a notice
     * that the asset is still being prepared.
     *
     * @param obj the parsed JSON object
     * @return the immutable {@link WatchTicket}
     */
    public static WatchTicket toWatchTicket(Map<String, Object> obj) {
        String assetId = str(obj, "asset_id");
        if (assetId == null) {
            throw new JsonParseException("watch ticket response is missing the 'asset_id' field");
        }
        List<PlaybackSegment> playback = new ArrayList<PlaybackSegment>();
        for (Map<String, Object> s : objects(obj, "playback")) {
            playback.add(new PlaybackSegment(
                    strOrEmpty(s, "replay_id"),
                    (int) longValue(s, "sequence_index", 0L),
                    longValue(s, "start_tick", 0L),
                    longValue(s, "end_tick", 0L)));
        }
        return WatchTicket.builder(assetId)
                .id(str(obj, "id"))
                .ticket(str(obj, "ticket"))
                .redemptionUrl(str(obj, "redemption_url"))
                .playback(playback)
                .expiresAt(instant(obj, "expires_at"))
                .expiresIn(longOrNull(obj, "expires_in"))
                .singleUse(bool(obj, "single_use"))
                .processingState(optionalWire(obj, "processing_state") != null
                        ? ProcessingState.fromWire(str(obj, "processing_state")) : null)
                .retryAfterSeconds(intOrNull(obj, "retry_after_seconds"))
                .build();
    }

    /**
     * Maps a redeemed watch ticket.
     *
     * @param obj the parsed JSON object with {@code session} and {@code segments}
     * @return the immutable {@link WatchTicketRedemption}
     */
    public static WatchTicketRedemption toWatchTicketRedemption(Map<String, Object> obj) {
        Object session = obj.get("session");
        long totalTicks = 0L;
        if (session instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) session;
            totalTicks = longValue(s, "total_ticks", 0L);
        }
        List<RedeemedSegment> segments = new ArrayList<RedeemedSegment>();
        for (Map<String, Object> s : objects(obj, "segments")) {
            segments.add(new RedeemedSegment(
                    (int) longValue(s, "sequence_index", 0L),
                    longValue(s, "start_tick", 0L),
                    longValue(s, "end_tick", 0L),
                    strOrEmpty(s, "url"),
                    longValue(s, "expires_in", 0L)));
        }
        return new WatchTicketRedemption(totalTicks, segments);
    }

    /**
     * Maps a minted watch link.
     *
     * @param obj the parsed JSON object with {@code token}, {@code expires_at}
     *            and {@code watch_url}
     * @return the immutable {@link WatchLink}
     */
    public static WatchLink toWatchLink(Map<String, Object> obj) {
        String token = str(obj, "token");
        String watchUrl = str(obj, "watch_url");
        if (token == null || watchUrl == null) {
            throw new JsonParseException("watch link response is missing 'token' or 'watch_url'");
        }
        return new WatchLink(token, instant(obj, "expires_at"), watchUrl);
    }

    /**
     * Maps the response to a webhook delivery replay.
     *
     * @param obj the parsed JSON object with {@code delivery_id} and {@code status}
     * @return the immutable {@link WebhookDeliveryReplay}
     */
    public static WebhookDeliveryReplay toWebhookDeliveryReplay(Map<String, Object> obj) {
        String deliveryId = str(obj, "delivery_id");
        if (deliveryId == null) {
            throw new JsonParseException("webhook replay response is missing the 'delivery_id' field");
        }
        return new WebhookDeliveryReplay(deliveryId, strOrEmpty(obj, "status"));
    }

    private static PluginUpdate toPluginUpdate(Object raw) {
        if (!(raw instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> u = (Map<String, Object>) raw;
        return new PluginUpdate(
                PluginUpdateStatus.fromWire(str(u, "status")),
                strOrEmpty(u, "minimumSupportedVersion"),
                strOrEmpty(u, "latestClassicVersion"),
                bool(u, "loaderAvailable"),
                str(u, "loaderDownloadUrl"));
    }

    private static List<Map<String, Object>> objects(Map<String, Object> obj, String key) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        Object raw = obj.get(key);
        if (raw instanceof List) {
            for (Object element : (List<?>) raw) {
                if (element instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) element;
                    out.add(m);
                }
            }
        }
        return out;
    }

    private static List<String> strings(Map<String, Object> obj, String key) {
        List<String> out = new ArrayList<String>();
        Object raw = obj.get(key);
        if (raw instanceof List) {
            for (Object element : (List<?>) raw) {
                if (element instanceof String) {
                    out.add((String) element);
                }
            }
        }
        return out;
    }

    private static Map<String, String> stringMap(Map<String, Object> obj, String key) {
        Map<String, String> out = new java.util.LinkedHashMap<String, String>();
        Object raw = obj.get(key);
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                    out.put((String) entry.getKey(), (String) entry.getValue());
                }
            }
        }
        return out;
    }

    private static String optionalWire(Map<String, Object> obj, String key) {
        return str(obj, key);
    }

    private static Boolean boolOrNull(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    private static String str(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v instanceof String) {
            String s = (String) v;
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    private static String strOrEmpty(Map<String, Object> obj, String key) {
        String v = str(obj, key);
        return v != null ? v : "";
    }

    private static boolean bool(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        return v instanceof Boolean && (Boolean) v;
    }

    private static Long longOrNull(Map<String, Object> obj, String key) {
        Object v = obj.get(key);
        if (v instanceof Long) {
            return (Long) v;
        }
        if (v instanceof Integer) {
            return ((Integer) v).longValue();
        }
        if (v instanceof Double) {
            return ((Double) v).longValue();
        }
        return null;
    }

    private static Integer intOrNull(Map<String, Object> obj, String key) {
        Long value = longOrNull(obj, key);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private static long longValue(Map<String, Object> obj, String key, long fallback) {
        Long v = longOrNull(obj, key);
        return v != null ? v : fallback;
    }

    private static Instant instant(Map<String, Object> obj, String key) {
        String v = str(obj, key);
        if (v == null) {
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException e) {
            // The cloud emits RFC3339, which Instant.parse accepts. A value it
            // cannot parse is treated as absent rather than failing the whole
            // response, so a future change to the timestamp format degrades to
            // a missing value instead of an exception.
            return null;
        }
    }
}
