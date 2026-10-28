/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * One timeline marker in a batch written with {@code createTimelineEventsBatch}.
 *
 * <p>Unlike {@link TimelineEventRequest}, a batch marker is addressed by the
 * replay collection and the archive segment it belongs to, not by a replay. The
 * client event id makes the entry idempotent: writing the same id again for the
 * same collection does not create a second marker.
 *
 * <p>The builder validates field bounds locally (label 1&ndash;120 characters,
 * category &le; 60, actor &le; 80, integration &le; 64, client event id
 * &le; 200) to match the server's own checks.
 */
public final class ScopeMarkerEntry {

    private final String collectionId;
    private final String archiveId;
    private final int sequenceIndex;
    private final String clientEventId;
    private final long tick;
    private final String label;
    private final String category;
    private final String actor;
    private final String integration;

    private ScopeMarkerEntry(Builder b) {
        this.collectionId = b.collectionId;
        this.archiveId = b.archiveId;
        this.sequenceIndex = b.sequenceIndex;
        this.clientEventId = b.clientEventId;
        this.tick = b.tick;
        this.label = b.label;
        this.category = b.category;
        this.actor = b.actor;
        this.integration = b.integration;
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
        body.put("tick", tick);
        body.put("label", label);
        if (category != null) {
            body.put("category", category);
        }
        if (actor != null) {
            body.put("actor", actor);
        }
        if (integration != null) {
            body.put("integration", integration);
        }
        return body;
    }

    /**
     * Returns a new builder for a batch marker.
     *
     * @param collectionId  the UUID of the collection the marker belongs to
     * @param archiveId     the UUID of the archive the tick is measured in
     * @param clientEventId a caller-chosen id, 1 to 200 characters, unique per marker in the collection
     * @param tick          the marker's tick, in ticks of the archive
     * @param label         the marker's visible label, 1 to 120 characters
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if a value is invalid
     */
    public static Builder builder(String collectionId, String archiveId, String clientEventId,
                                  long tick, String label) {
        return new Builder(collectionId, archiveId, clientEventId, tick, label);
    }

    /**
     * A fluent builder for {@link ScopeMarkerEntry}. Not thread-safe.
     */
    public static final class Builder {
        private final String collectionId;
        private final String archiveId;
        private final String clientEventId;
        private final long tick;
        private final String label;
        private int sequenceIndex;
        private String category;
        private String actor;
        private String integration;

        private Builder(String collectionId, String archiveId, String clientEventId, long tick, String label) {
            this.collectionId = Validation.requireUuid(collectionId, "collectionId");
            this.archiveId = Validation.requireUuid(archiveId, "archiveId");
            this.clientEventId = Validation.requireText(clientEventId, "clientEventId", 200);
            this.tick = Validation.requireTick(tick, "tick");
            this.label = Validation.requireText(label, "label", 120);
        }

        /**
         * Sets the 0-based sequence index of the archive segment the marker belongs to.
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
         * Sets an optional category label for grouping markers.
         *
         * @param category at most 60 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder category(String category) {
            this.category = Validation.requireText(category, "category", 60);
            return this;
        }

        /**
         * Sets an optional actor label (who or what raised the marker).
         *
         * @param actor at most 80 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder actor(String actor) {
            this.actor = Validation.requireText(actor, "actor", 80);
            return this;
        }

        /**
         * Names the integration raising the marker, so its visibility can follow the
         * integration's team-role rules.
         *
         * @param integration at most 64 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder integration(String integration) {
            this.integration = Validation.requireText(integration, "integration", 64);
            return this;
        }

        /**
         * Builds the immutable entry.
         *
         * @return a new immutable entry
         */
        public ScopeMarkerEntry build() {
            return new ScopeMarkerEntry(this);
        }
    }
}
