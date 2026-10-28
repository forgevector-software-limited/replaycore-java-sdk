/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A player to add to a replay collection's roster.
 *
 * <p>Used by {@link CollectionRequest}, {@link CollectionUpdateRequest} and
 * {@link CollectionFinalizeRequest}. Ticks are positions on the collection's own
 * timeline, never wall-clock times. A player is identified by UUID, so sending the
 * same player again updates the existing entry.
 *
 * <p>The builder validates field bounds locally (name &le; 64 characters, role
 * &le; 32, team key &le; 64, result &le; 64) to match the server's own checks.
 */
public final class ScopeParticipant {

    /** The role a participant takes when none is set. */
    public static final String DEFAULT_ROLE = "participant";

    private final String playerUuid;
    private final String playerName;
    private final String teamKey;
    private final String role;
    private final long joinedTick;
    private final Long leftTick;
    private final String result;

    private ScopeParticipant(Builder b) {
        this.playerUuid = b.playerUuid;
        this.playerName = b.playerName;
        this.teamKey = b.teamKey;
        this.role = b.role;
        this.joinedTick = b.joinedTick;
        this.leftTick = b.leftTick;
        this.result = b.result;
    }

    /**
     * Renders this participant as the JSON object the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("player_uuid", playerUuid);
        body.put("player_name", playerName);
        if (teamKey != null) {
            body.put("team_key", teamKey);
        }
        body.put("role", role);
        body.put("joined_tick", joinedTick);
        if (leftTick != null) {
            body.put("left_tick", leftTick);
        }
        if (result != null) {
            body.put("result", result);
        }
        return body;
    }

    /**
     * Returns a new builder for a roster entry.
     *
     * @param playerUuid the player's UUID
     * @param playerName the player's name, 1 to 64 characters
     * @return a builder seeded with the player
     * @throws IllegalArgumentException if the UUID is invalid or the name is blank or too long
     */
    public static Builder builder(String playerUuid, String playerName) {
        return new Builder(playerUuid, playerName);
    }

    /**
     * A fluent builder for {@link ScopeParticipant}. Not thread-safe.
     */
    public static final class Builder {
        private final String playerUuid;
        private final String playerName;
        private String teamKey;
        private String role = DEFAULT_ROLE;
        private long joinedTick;
        private Long leftTick;
        private String result;

        private Builder(String playerUuid, String playerName) {
            this.playerUuid = Validation.requireUuid(playerUuid, "playerUuid");
            this.playerName = Validation.requireText(playerName, "playerName", 64);
        }

        /**
         * Puts the player on a team.
         *
         * @param teamKey the key of a team sent with the same request or an earlier one,
         *                at most 64 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder teamKey(String teamKey) {
            this.teamKey = Validation.requireText(teamKey, "teamKey", 64);
            return this;
        }

        /**
         * Sets the player's role. The default is {@value #DEFAULT_ROLE}; other
         * values the cloud treats specially are {@code spectator},
         * {@code staff_observer}, {@code coach} and {@code broadcaster}.
         *
         * @param role between 1 and 32 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder role(String role) {
            this.role = Validation.requireText(role, "role", 32);
            return this;
        }

        /**
         * Sets the tick at which the player joined. The default is 0.
         *
         * @param joinedTick a non-negative tick
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder joinedTick(long joinedTick) {
            this.joinedTick = Validation.requireTick(joinedTick, "joinedTick");
            return this;
        }

        /**
         * Sets the tick at which the player left.
         *
         * @param leftTick a non-negative tick, not before the join tick
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder leftTick(long leftTick) {
            this.leftTick = Validation.requireTick(leftTick, "leftTick");
            return this;
        }

        /**
         * Sets the player's result label (for example {@code "winner"}).
         *
         * @param result at most 64 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder result(String result) {
            this.result = Validation.requireText(result, "result", 64);
            return this;
        }

        /**
         * Builds the immutable participant.
         *
         * @return a new immutable participant
         * @throws IllegalArgumentException if the leave tick precedes the join tick
         */
        public ScopeParticipant build() {
            if (leftTick != null && leftTick < joinedTick) {
                throw new IllegalArgumentException("leftTick must not precede joinedTick");
            }
            return new ScopeParticipant(this);
        }
    }
}
