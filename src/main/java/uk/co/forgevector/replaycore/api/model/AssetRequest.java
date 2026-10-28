/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A request to create a clip or highlight from a replay collection.
 *
 * <p>Maps to {@code POST /v1/replay-collections/{id}/assets}. The call needs an
 * API key with the {@link ApiScope#CLIPS_WRITE} scope and an idempotency key. The
 * key is what stops a resend creating a second asset for the same moment: sending
 * the same key again returns the original {@link ScopeOperation}.
 *
 * <p>A new asset is queued and embargoed. Report its media as finished with
 * {@link AssetFinalizeRequest}. The full-match asset is produced when the
 * collection is finalised, so {@link AssetKind#FULL_MATCH} cannot be requested.
 */
public final class AssetRequest {

    private static final int MAX_RELATIONSHIPS = 256;

    private final String idempotencyKey;
    private final AssetKind kind;
    private final EventKind eventKind;
    private final String segmentId;
    private final long startTick;
    private final long endTick;
    private final List<Object> relationships;

    private AssetRequest(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.kind = b.kind;
        this.eventKind = b.eventKind;
        this.segmentId = b.segmentId;
        this.startTick = b.startTick;
        this.endTick = b.endTick;
        this.relationships = new ArrayList<Object>(b.relationships);
    }

    /**
     * Returns the idempotency key the client sends in the {@code Idempotency-Key}
     * header.
     *
     * @return the key; never {@code null}
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Renders this request as the JSON body map the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map of body fields (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("kind", kind.wireValue());
        if (eventKind != null) {
            body.put("event_kind", eventKind.wireValue());
        }
        body.put("segment_id", segmentId);
        body.put("start_tick", startTick);
        body.put("end_tick", endTick);
        if (!relationships.isEmpty()) {
            body.put("relationships", new ArrayList<Object>(relationships));
        }
        return body;
    }

    /**
     * Returns a new builder for creating an asset.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this asset
     * @param kind           {@link AssetKind#EVENT_CLIP}, {@link AssetKind#CUSTOM_CLIP}
     *                       or {@link AssetKind#HIGHLIGHT}
     * @param segmentId      the UUID of the collection segment the asset is cut from
     * @param startTick      the first tick of the asset's window
     * @param endTick        the last tick of the asset's window, not before the start tick
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if a value is invalid
     */
    public static Builder builder(String idempotencyKey, AssetKind kind, String segmentId,
                                  long startTick, long endTick) {
        return new Builder(idempotencyKey, kind, segmentId, startTick, endTick);
    }

    /**
     * A fluent builder for {@link AssetRequest}. Not thread-safe.
     */
    public static final class Builder {
        private final String idempotencyKey;
        private final AssetKind kind;
        private final String segmentId;
        private final long startTick;
        private final long endTick;
        private EventKind eventKind;
        private final List<Object> relationships = new ArrayList<Object>();

        private Builder(String idempotencyKey, AssetKind kind, String segmentId, long startTick, long endTick) {
            this.idempotencyKey = Validation.requireIdempotencyKey(idempotencyKey);
            if (kind == null || kind == AssetKind.UNKNOWN || kind == AssetKind.FULL_MATCH) {
                throw new IllegalArgumentException(
                        "kind must be EVENT_CLIP, CUSTOM_CLIP or HIGHLIGHT; FULL_MATCH is produced by finalising the collection");
            }
            this.kind = kind;
            this.segmentId = Validation.requireUuid(segmentId, "segmentId");
            this.startTick = Validation.requireTick(startTick, "startTick");
            this.endTick = Validation.requireTick(endTick, "endTick");
            if (this.endTick < this.startTick) {
                throw new IllegalArgumentException("endTick must not precede startTick");
            }
        }

        /**
         * Sets the gameplay event the asset covers.
         *
         * @param eventKind the event kind
         * @return this builder
         * @throws IllegalArgumentException if {@code null} or {@link EventKind#UNKNOWN}
         */
        public Builder eventKind(EventKind eventKind) {
            if (eventKind == null || eventKind == EventKind.UNKNOWN) {
                throw new IllegalArgumentException("eventKind must be a known event kind");
            }
            this.eventKind = eventKind;
            return this;
        }

        /**
         * Relates a player to the asset, for example the killer and the victim of a kill clip.
         *
         * @param playerUuid the player's UUID
         * @param relation   how the player relates to the asset; at most 256 relationships in all
         * @return this builder
         * @throws IllegalArgumentException if the UUID is invalid, the relation is unknown,
         *                                  or there are too many relationships
         */
        public Builder relationship(String playerUuid, AssetRelation relation) {
            if (relationships.size() >= MAX_RELATIONSHIPS) {
                throw new IllegalArgumentException("relationships must not exceed " + MAX_RELATIONSHIPS + " entries");
            }
            if (relation == null || relation == AssetRelation.UNKNOWN) {
                throw new IllegalArgumentException("relation must be a known relation");
            }
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("player_uuid", Validation.requireUuid(playerUuid, "playerUuid"));
            entry.put("relation", relation.wireValue());
            relationships.add(entry);
            return this;
        }

        /**
         * Builds the immutable request.
         *
         * @return a new immutable request
         */
        public AssetRequest build() {
            return new AssetRequest(this);
        }
    }
}
