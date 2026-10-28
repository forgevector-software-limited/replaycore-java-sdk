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
 * One event clip in a batch written with {@code createAssetsBatch}.
 *
 * <p>A batch clip is addressed by the replay collection and the archive segment it
 * is cut from. The client event id makes the entry idempotent: writing the same id
 * again for the same collection does not create a second clip. Every batch clip is
 * an {@link AssetKind#EVENT_CLIP}.
 */
public final class ScopeClipEntry {

    private static final int MAX_RELATIONSHIPS = 256;

    private final String collectionId;
    private final String archiveId;
    private final int sequenceIndex;
    private final String clientEventId;
    private final EventKind eventKind;
    private final long startTick;
    private final long endTick;
    private final List<Object> relationships;

    private ScopeClipEntry(Builder b) {
        this.collectionId = b.collectionId;
        this.archiveId = b.archiveId;
        this.sequenceIndex = b.sequenceIndex;
        this.clientEventId = b.clientEventId;
        this.eventKind = b.eventKind;
        this.startTick = b.startTick;
        this.endTick = b.endTick;
        this.relationships = new ArrayList<Object>(b.relationships);
    }

    /**
     * Renders this entry as the JSON object the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("collection_id", collectionId);
        body.put("archive_id", archiveId);
        body.put("segment_sequence_index", sequenceIndex);
        body.put("client_event_id", clientEventId);
        body.put("kind", AssetKind.EVENT_CLIP.wireValue());
        body.put("event_kind", eventKind.wireValue());
        body.put("start_tick", startTick);
        body.put("end_tick", endTick);
        if (!relationships.isEmpty()) {
            body.put("relationships", new ArrayList<Object>(relationships));
        }
        return body;
    }

    /**
     * Returns a new builder for a batch clip.
     *
     * @param collectionId  the UUID of the collection the clip belongs to
     * @param archiveId     the UUID of the archive the ticks are measured in
     * @param clientEventId a caller-chosen id, 1 to 200 characters, unique per clip in the collection
     * @param eventKind     the gameplay event the clip covers
     * @param startTick     the first tick of the clip, in ticks of the archive
     * @param endTick       the last tick of the clip, not before the start tick
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if a value is invalid
     */
    public static Builder builder(String collectionId, String archiveId, String clientEventId,
                                  EventKind eventKind, long startTick, long endTick) {
        return new Builder(collectionId, archiveId, clientEventId, eventKind, startTick, endTick);
    }

    /**
     * A fluent builder for {@link ScopeClipEntry}. Not thread-safe.
     */
    public static final class Builder {
        private final String collectionId;
        private final String archiveId;
        private final String clientEventId;
        private final EventKind eventKind;
        private final long startTick;
        private final long endTick;
        private int sequenceIndex;
        private final List<Object> relationships = new ArrayList<Object>();

        private Builder(String collectionId, String archiveId, String clientEventId,
                        EventKind eventKind, long startTick, long endTick) {
            this.collectionId = Validation.requireUuid(collectionId, "collectionId");
            this.archiveId = Validation.requireUuid(archiveId, "archiveId");
            this.clientEventId = Validation.requireText(clientEventId, "clientEventId", 200);
            if (eventKind == null || eventKind == EventKind.UNKNOWN) {
                throw new IllegalArgumentException("eventKind must be a known event kind");
            }
            this.eventKind = eventKind;
            this.startTick = Validation.requireTick(startTick, "startTick");
            this.endTick = Validation.requireTick(endTick, "endTick");
            if (this.endTick < this.startTick) {
                throw new IllegalArgumentException("endTick must not precede startTick");
            }
        }

        /**
         * Sets the 0-based sequence index of the archive segment the clip is cut from.
         * The default is 0.
         *
         * @param sequenceIndex a non-negative index
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder sequenceIndex(int sequenceIndex) {
            if (sequenceIndex < 0) {
                throw new IllegalArgumentException("sequenceIndex must not be negative");
            }
            this.sequenceIndex = sequenceIndex;
            return this;
        }

        /**
         * Relates a player to the clip, for example the killer and the victim.
         *
         * @param playerUuid the player's UUID
         * @param relation   how the player relates to the clip; at most 256 relationships in all
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
         * Builds the immutable entry.
         *
         * @return a new immutable entry
         */
        public ScopeClipEntry build() {
            return new ScopeClipEntry(this);
        }
    }
}
