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
 * A request to open a replay collection: the logical window over a recording
 * that an integration begins at the start of a match.
 *
 * <p>Maps to {@code POST /v1/replay-collections}. The call needs an API key with
 * the {@link ApiScope#COLLECTIONS_WRITE} scope and an idempotency key. The key
 * identifies the collection: sending the same request again with the same key
 * returns the collection the first request created instead of opening a second one.
 * Keep the request object, or at least its key, until the call has succeeded.
 *
 * <p>The builder validates field bounds locally to match the server's own checks,
 * so a bad value fails in the caller's JVM rather than after a round trip.
 */
public final class CollectionRequest {

    /** The most participants one request may carry. */
    public static final int MAX_PARTICIPANTS = 256;

    /** The most teams one request may carry. */
    public static final int MAX_TEAMS = 64;

    /** The most spectators one request may carry. */
    public static final int MAX_SPECTATORS = 512;

    /** The most segments one request may carry. */
    public static final int MAX_SEGMENTS = 8192;

    private static final int MAX_WORLD_IDS = 16;

    private final String idempotencyKey;
    private final Map<String, Object> body;

    private CollectionRequest(Builder b) {
        this.idempotencyKey = b.idempotencyKey;
        this.body = b.body();
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
        return new LinkedHashMap<String, Object>(body);
    }

    /**
     * Returns a new builder for opening a collection.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this collection
     * @param serverId       the UUID of the server that is recording the match
     * @param category       the integration-defined category, at most 64 characters
     * @param mode           the integration-defined game mode, at most 64 characters
     * @param policyId       the id of the release policy, at most 64 characters
     * @param policy         the resolved release policy to store with the collection
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if a value is blank, too long or not a UUID
     */
    public static Builder builder(String idempotencyKey, String serverId, String category,
                                  String mode, String policyId, ReleasePolicy policy) {
        return new Builder(idempotencyKey, serverId, category, mode, policyId, policy);
    }

    /**
     * A fluent builder for {@link CollectionRequest}. Not thread-safe.
     */
    public static final class Builder {
        private final String idempotencyKey;
        private final String serverId;
        private final String category;
        private final String mode;
        private final String policyId;
        private final ReleasePolicy policy;
        private String externalMatchId;
        private long startTick;
        private List<String> worldIds = new ArrayList<String>();
        private final List<Object> participants = new ArrayList<Object>();
        private final List<Object> teams = new ArrayList<Object>();
        private final List<String> spectators = new ArrayList<String>();
        private String competitionId;
        private String seriesId;
        private String roundId;
        private String bracketMatchId;
        private Integer gameNumber;
        private Integer bestOf;
        private String partyEventId;
        private String region;
        private Integer teamSize;
        private Boolean ranked;
        private Boolean tournament;
        private Map<String, String> metadata;
        private final List<Object> segments = new ArrayList<Object>();

        private Builder(String idempotencyKey, String serverId, String category, String mode,
                        String policyId, ReleasePolicy policy) {
            this.idempotencyKey = Validation.requireIdempotencyKey(idempotencyKey);
            this.serverId = Validation.requireUuid(serverId, "serverId");
            this.category = Validation.requireText(category, "category", 64);
            this.mode = Validation.requireText(mode, "mode", 64);
            this.policyId = Validation.requireText(policyId, "policyId", 64);
            this.policy = Objects.requireNonNull(policy, "policy");
        }

        /**
         * Sets the integration's own identifier for the match, so the collection can
         * be found again with {@code CollectionQuery} or a bulk lookup.
         *
         * @param externalMatchId at most 200 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder externalMatchId(String externalMatchId) {
            this.externalMatchId = Validation.requireText(externalMatchId, "externalMatchId", 200);
            return this;
        }

        /**
         * Sets the tick at which the collection begins. The default is 0.
         *
         * @param startTick a non-negative tick
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder startTick(long startTick) {
            this.startTick = Validation.requireTick(startTick, "startTick");
            return this;
        }

        /**
         * Sets the ids of the worlds the match takes place in.
         *
         * @param worldIds at most 16 ids, each 1 to 64 characters
         * @return this builder
         * @throws IllegalArgumentException if there are too many ids or one is invalid
         */
        public Builder worldIds(List<String> worldIds) {
            Objects.requireNonNull(worldIds, "worldIds");
            if (worldIds.size() > MAX_WORLD_IDS) {
                throw new IllegalArgumentException("worldIds must not exceed " + MAX_WORLD_IDS + " entries");
            }
            List<String> copy = new ArrayList<String>();
            for (String id : worldIds) {
                copy.add(Validation.requireText(id, "worldId", 64));
            }
            this.worldIds = copy;
            return this;
        }

        /**
         * Adds a player to the roster.
         *
         * @param participant the roster entry; at most {@value CollectionRequest#MAX_PARTICIPANTS} in all
         * @return this builder
         * @throws IllegalArgumentException if the roster is full
         */
        public Builder participant(ScopeParticipant participant) {
            if (participants.size() >= MAX_PARTICIPANTS) {
                throw new IllegalArgumentException("participants must not exceed " + MAX_PARTICIPANTS + " entries");
            }
            participants.add(Objects.requireNonNull(participant, "participant").toBody());
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
         * Adds a spectator who is present when the match begins. The player joins
         * at the collection's start tick with the {@code spectator} role.
         *
         * @param playerUuid the spectator's UUID; at most {@value CollectionRequest#MAX_SPECTATORS} in all
         * @return this builder
         * @throws IllegalArgumentException if invalid or there are too many spectators
         */
        public Builder spectator(String playerUuid) {
            if (spectators.size() >= MAX_SPECTATORS) {
                throw new IllegalArgumentException("spectators must not exceed " + MAX_SPECTATORS + " entries");
            }
            spectators.add(Validation.requireUuid(playerUuid, "playerUuid"));
            return this;
        }

        /**
         * Sets the id of the competition the match belongs to.
         *
         * @param competitionId at most 128 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder competitionId(String competitionId) {
            this.competitionId = Validation.requireText(competitionId, "competitionId", 128);
            return this;
        }

        /**
         * Sets the id of the series the match belongs to.
         *
         * @param seriesId at most 128 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder seriesId(String seriesId) {
            this.seriesId = Validation.requireText(seriesId, "seriesId", 128);
            return this;
        }

        /**
         * Sets the id of the round the match belongs to.
         *
         * @param roundId at most 128 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder roundId(String roundId) {
            this.roundId = Validation.requireText(roundId, "roundId", 128);
            return this;
        }

        /**
         * Sets the id of the bracket match.
         *
         * @param bracketMatchId at most 128 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder bracketMatchId(String bracketMatchId) {
            this.bracketMatchId = Validation.requireText(bracketMatchId, "bracketMatchId", 128);
            return this;
        }

        /**
         * Sets the game number within its series.
         *
         * @param gameNumber 1 or more
         * @return this builder
         * @throws IllegalArgumentException if less than 1
         */
        public Builder gameNumber(int gameNumber) {
            this.gameNumber = positive(gameNumber, "gameNumber");
            return this;
        }

        /**
         * Sets the length of the series as a best-of count.
         *
         * @param bestOf 1 or more
         * @return this builder
         * @throws IllegalArgumentException if less than 1
         */
        public Builder bestOf(int bestOf) {
            this.bestOf = positive(bestOf, "bestOf");
            return this;
        }

        /**
         * Groups the match under a parent event, such as a tournament weekend.
         *
         * @param partyEventId at most 128 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder partyEventId(String partyEventId) {
            this.partyEventId = Validation.requireText(partyEventId, "partyEventId", 128);
            return this;
        }

        /**
         * Sets the region the match is played in.
         *
         * @param region at most 64 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder region(String region) {
            this.region = Validation.requireText(region, "region", 64);
            return this;
        }

        /**
         * Sets the number of players per team.
         *
         * @param teamSize 1 or more
         * @return this builder
         * @throws IllegalArgumentException if less than 1
         */
        public Builder teamSize(int teamSize) {
            this.teamSize = positive(teamSize, "teamSize");
            return this;
        }

        /**
         * Marks the match as ranked or unranked.
         *
         * @param ranked whether the match is ranked
         * @return this builder
         */
        public Builder ranked(boolean ranked) {
            this.ranked = ranked;
            return this;
        }

        /**
         * Marks the match as a tournament match or not.
         *
         * @param tournament whether the match is a tournament match
         * @return this builder
         */
        public Builder tournament(boolean tournament) {
            this.tournament = tournament;
            return this;
        }

        /**
         * Sets the collection's namespaced key/value metadata, replacing any set earlier.
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
         * Adds the first, still-open segment of the recording the match is being
         * recorded into, so that embargo coverage exists for the whole match.
         *
         * @param segment the segment; at most {@value CollectionRequest#MAX_SEGMENTS} in all
         * @return this builder
         * @throws IllegalArgumentException if there are too many segments
         */
        public Builder segment(ScopeSegment segment) {
            if (segments.size() >= MAX_SEGMENTS) {
                throw new IllegalArgumentException("segments must not exceed " + MAX_SEGMENTS + " entries");
            }
            segments.add(Objects.requireNonNull(segment, "segment").toBody());
            return this;
        }

        /**
         * Builds the immutable request.
         *
         * @return a new immutable request
         */
        public CollectionRequest build() {
            return new CollectionRequest(this);
        }

        private Map<String, Object> body() {
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            if (externalMatchId != null) {
                body.put("external_match_id", externalMatchId);
            }
            body.put("category", category);
            body.put("mode", mode);
            body.put("policy_id", policyId);
            body.put("server_id", serverId);
            body.put("start_tick", startTick);
            if (!worldIds.isEmpty()) {
                body.put("world_ids", new ArrayList<String>(worldIds));
            }
            if (!participants.isEmpty()) {
                body.put("participants", new ArrayList<Object>(participants));
            }
            if (!teams.isEmpty()) {
                body.put("teams", new ArrayList<Object>(teams));
            }
            if (!spectators.isEmpty()) {
                body.put("spectators", new ArrayList<String>(spectators));
            }
            putIfSet(body, "competition_id", competitionId);
            putIfSet(body, "series_id", seriesId);
            putIfSet(body, "round_id", roundId);
            putIfSet(body, "bracket_match_id", bracketMatchId);
            putIfSet(body, "game_number", gameNumber);
            putIfSet(body, "best_of", bestOf);
            putIfSet(body, "party_event_id", partyEventId);
            putIfSet(body, "region", region);
            putIfSet(body, "team_size", teamSize);
            putIfSet(body, "ranked", ranked);
            putIfSet(body, "tournament", tournament);
            if (metadata != null && !metadata.isEmpty()) {
                body.put("metadata", new LinkedHashMap<String, String>(metadata));
            }
            body.put("policy_snapshot", policy.toBody());
            if (!segments.isEmpty()) {
                body.put("segments", new ArrayList<Object>(segments));
            }
            return body;
        }

        private static void putIfSet(Map<String, Object> body, String key, Object value) {
            if (value != null) {
                body.put(key, value);
            }
        }

        private static int positive(int value, String field) {
            if (value < 1) {
                throw new IllegalArgumentException(field + " must be positive");
            }
            return value;
        }
    }
}
