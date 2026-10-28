/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A request to finalise a replay collection: to end the match's scope and hand
 * its footage over for processing.
 *
 * <p>Maps to {@code POST /v1/replay-collections/{id}/finalize}. The call needs an
 * API key with the {@link ApiScope#COLLECTIONS_WRITE} scope and an idempotency
 * key. Finalising moves the collection to {@link ProcessingState#FINALIZING}, not
 * {@link ProcessingState#READY}: the archive it was cut from still has to upload.
 * Sending the same key again returns the original {@link ScopeOperation}, with its
 * processing state refreshed from the collection.
 */
public final class CollectionFinalizeRequest {

    private static final int MAX_TEAMS = 64;

    private final String idempotencyKey;
    private final long endTick;
    private final List<Object> participants;
    private final List<Object> teams;
    private final Map<String, String> metadata;
    private final List<Object> segments;

    private CollectionFinalizeRequest(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.endTick = b.endTick;
        this.participants = new ArrayList<Object>(b.participants);
        this.teams = new ArrayList<Object>(b.teams);
        this.metadata = b.metadata;
        this.segments = new ArrayList<Object>(b.segments);
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
        body.put("end_tick", endTick);
        if (!participants.isEmpty()) {
            body.put("participants", new ArrayList<Object>(participants));
        }
        if (!teams.isEmpty()) {
            body.put("teams", new ArrayList<Object>(teams));
        }
        if (!metadata.isEmpty()) {
            body.put("metadata", new LinkedHashMap<String, String>(metadata));
        }
        if (!segments.isEmpty()) {
            body.put("segments", new ArrayList<Object>(segments));
        }
        return body;
    }

    /**
     * Returns a new builder for finalising a collection.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this finalisation
     * @param endTick        the tick at which the match ended
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if the key is blank, too long or contains CR, LF or NUL,
     *                                  or the tick is negative
     */
    public static Builder builder(String idempotencyKey, long endTick) {
        return new Builder(idempotencyKey, endTick);
    }

    /**
     * A fluent builder for {@link CollectionFinalizeRequest}. Not thread-safe.
     */
    public static final class Builder {
        private final String idempotencyKey;
        private final long endTick;
        private final List<Object> participants = new ArrayList<Object>();
        private final List<Object> teams = new ArrayList<Object>();
        private Map<String, String> metadata = Validation.requireMetadata(null);
        private final List<Object> segments = new ArrayList<Object>();

        private Builder(String idempotencyKey, long endTick) {
            this.idempotencyKey = Validation.requireIdempotencyKey(idempotencyKey);
            this.endTick = Validation.requireTick(endTick, "endTick");
        }

        /**
         * Adds the final state of a player on the roster.
         *
         * @param participant the roster entry; at most
         *                    {@value CollectionRequest#MAX_PARTICIPANTS} in all
         * @return this builder
         * @throws IllegalArgumentException if the roster is full
         */
        public Builder participant(ScopeParticipant participant) {
            if (participants.size() >= CollectionRequest.MAX_PARTICIPANTS) {
                throw new IllegalArgumentException("participants must not exceed "
                        + CollectionRequest.MAX_PARTICIPANTS + " entries");
            }
            participants.add(Objects.requireNonNull(participant, "participant").toBody());
            return this;
        }

        /**
         * Adds the final state of a team, including its result and placement.
         *
         * @param team the team; at most {@value CollectionRequest#MAX_TEAMS} in all
         * @return this builder
         * @throws IllegalArgumentException if there are too many teams
         */
        public Builder team(ScopeTeam team) {
            if (teams.size() >= MAX_TEAMS) {
                throw new IllegalArgumentException("teams must not exceed " + MAX_TEAMS + " entries");
            }
            teams.add(Objects.requireNonNull(team, "team").toBody());
            return this;
        }

        /**
         * Sets metadata to merge into the collection's own, replacing any set earlier on
         * this builder.
         *
         * @param metadata at most 32 entries; each key is namespaced (for example
         *                 {@code acme:arena}) and each value is at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if an entry is not acceptable
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = Validation.requireMetadata(metadata);
            return this;
        }

        /**
         * Adds a closed segment. Send the collection's full segment list, including
         * the one opened when the collection was created, now closed.
         *
         * @param segment the segment; at most {@value CollectionRequest#MAX_SEGMENTS} in all
         * @return this builder
         * @throws IllegalArgumentException if there are too many segments
         */
        public Builder segment(ScopeSegment segment) {
            if (segments.size() >= CollectionRequest.MAX_SEGMENTS) {
                throw new IllegalArgumentException("segments must not exceed "
                        + CollectionRequest.MAX_SEGMENTS + " entries");
            }
            segments.add(Objects.requireNonNull(segment, "segment").toBody());
            return this;
        }

        /**
         * Builds the immutable request.
         *
         * @return a new immutable request
         */
        public CollectionFinalizeRequest build() {
            return new CollectionFinalizeRequest(this);
        }
    }
}
