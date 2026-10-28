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
 * A request to update a replay collection that is still open: players who joined
 * or left, new teams, and extra metadata.
 *
 * <p>Maps to {@code POST /v1/replay-collections/{id}}. The call needs an API key
 * with the {@link ApiScope#COLLECTIONS_WRITE} scope and an idempotency key. The
 * writes are idempotent, and sending the same key again returns the original
 * {@link ScopeOperation} without applying the update twice. A key may not be
 * reused for a different update.
 *
 * <p>Metadata sent here is merged into what the collection already holds.
 */
public final class CollectionUpdateRequest {

    private static final int MAX_TEAMS = 64;

    private final String idempotencyKey;
    private final List<Object> participantsJoined;
    private final List<Object> participantsLeft;
    private final List<Object> teams;
    private final Map<String, String> metadata;

    private CollectionUpdateRequest(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.participantsJoined = new ArrayList<Object>(b.participantsJoined);
        this.participantsLeft = new ArrayList<Object>(b.participantsLeft);
        this.teams = new ArrayList<Object>(b.teams);
        this.metadata = b.metadata;
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
        if (!participantsJoined.isEmpty()) {
            body.put("participants_joined", new ArrayList<Object>(participantsJoined));
        }
        if (!participantsLeft.isEmpty()) {
            body.put("participants_left", new ArrayList<Object>(participantsLeft));
        }
        if (!teams.isEmpty()) {
            body.put("teams", new ArrayList<Object>(teams));
        }
        if (!metadata.isEmpty()) {
            body.put("metadata", new LinkedHashMap<String, String>(metadata));
        }
        return body;
    }

    /**
     * Returns a new builder for a collection update.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this update
     * @return a builder seeded with the key
     * @throws IllegalArgumentException if the key is blank, too long or contains CR, LF or NUL
     */
    public static Builder builder(String idempotencyKey) {
        return new Builder(idempotencyKey);
    }

    /**
     * A fluent builder for {@link CollectionUpdateRequest}. Not thread-safe.
     */
    public static final class Builder {
        private final String idempotencyKey;
        private final List<Object> participantsJoined = new ArrayList<Object>();
        private final List<Object> participantsLeft = new ArrayList<Object>();
        private final List<Object> teams = new ArrayList<Object>();
        private Map<String, String> metadata = Validation.requireMetadata(null);

        private Builder(String idempotencyKey) {
            this.idempotencyKey = Validation.requireIdempotencyKey(idempotencyKey);
        }

        /**
         * Adds a player who joined.
         *
         * @param participant the roster entry
         * @return this builder
         */
        public Builder participantJoined(ScopeParticipant participant) {
            participantsJoined.add(Objects.requireNonNull(participant, "participant").toBody());
            return this;
        }

        /**
         * Records that a player left at a tick. The player must already be on the roster.
         *
         * @param playerUuid the player's UUID
         * @param leftTick   the tick at which the player left, not before they joined
         * @return this builder
         * @throws IllegalArgumentException if the UUID is invalid or the tick is negative
         */
        public Builder participantLeft(String playerUuid, long leftTick) {
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("player_uuid", Validation.requireUuid(playerUuid, "playerUuid"));
            entry.put("left_tick", Validation.requireTick(leftTick, "leftTick"));
            participantsLeft.add(entry);
            return this;
        }

        /**
         * Adds a team.
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
         * Builds the immutable request.
         *
         * @return a new immutable request
         */
        public CollectionUpdateRequest build() {
            return new CollectionUpdateRequest(this);
        }
    }
}
