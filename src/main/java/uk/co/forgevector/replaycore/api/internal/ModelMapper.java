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
import uk.co.forgevector.replaycore.api.model.Participant;
import uk.co.forgevector.replaycore.api.model.Quality;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.StorageTier;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;
import uk.co.forgevector.replaycore.api.model.Visibility;

/**
 * Converts parsed JSON trees (the {@link Map}/{@link List}/scalar shape that
 * {@link Json} produces) into the SDK's immutable model types.
 *
 * <p>This is an internal helper. It is deliberately lenient about field presence
 * &mdash; missing optional fields map to absent {@link java.util.Optional}s, and
 * unknown enum values map to the relevant {@code UNKNOWN} constant &mdash; so a
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
                .sessionId(str(obj, "session_id"));

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
                str(obj, "actor"),
                instant(obj, "createdAt"));
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
            // response, keeping the SDK robust against future format tweaks.
            return null;
        }
    }
}
