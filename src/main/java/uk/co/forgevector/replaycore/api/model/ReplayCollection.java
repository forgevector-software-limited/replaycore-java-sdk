/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A logical match collection: the window of one or more recordings that an integration opens at
 * the start of a match and finalises at its end.
 *
 * Immutable. The create and embargo calls return the stored collection. The catalogue calls (list,
 * get and bulk lookup) also return the rosters and the derived playback flags; where a call does
 * not return a field, the getter reads as empty, an empty list or {@code false}.
 */
public final class ReplayCollection {

    private final String id;
    private final String externalMatchId;
    private final String category;
    private final String mode;
    private final String policyId;
    private final String serverId;
    private final List<String> worldIds;
    private final long startTick;
    private final Long endTick;
    private final Instant startedAt;
    private final Instant endedAt;
    private final Map<String, String> metadata;
    private final String competitionId;
    private final String seriesId;
    private final String roundId;
    private final String bracketMatchId;
    private final Integer gameNumber;
    private final Integer bestOf;
    private final String partyEventId;
    private final String region;
    private final Integer teamSize;
    private final Boolean ranked;
    private final Boolean tournament;
    private final ProcessingState processingState;
    private final ReleaseState releaseState;
    private final CollectionVisibility visibility;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<CollectionParticipant> participants;
    private final List<CollectionTeam> teams;
    private final AssetRelation viewerRelation;
    private final Long durationTicks;
    private final boolean thumbnailReady;
    private final boolean playbackEligible;
    private final boolean playable;
    private final boolean mediaReady;
    private final boolean expired;

    private ReplayCollection(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.externalMatchId = b.externalMatchId;
        this.category = b.category != null ? b.category : "";
        this.mode = b.mode != null ? b.mode : "";
        this.policyId = b.policyId != null ? b.policyId : "";
        this.serverId = b.serverId != null ? b.serverId : "";
        this.worldIds = b.worldIds != null
                ? Collections.unmodifiableList(new ArrayList<String>(b.worldIds))
                : Collections.<String>emptyList();
        this.startTick = b.startTick;
        this.endTick = b.endTick;
        this.startedAt = b.startedAt;
        this.endedAt = b.endedAt;
        this.metadata = b.metadata != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, String>(b.metadata))
                : Collections.<String, String>emptyMap();
        this.competitionId = b.competitionId;
        this.seriesId = b.seriesId;
        this.roundId = b.roundId;
        this.bracketMatchId = b.bracketMatchId;
        this.gameNumber = b.gameNumber;
        this.bestOf = b.bestOf;
        this.partyEventId = b.partyEventId;
        this.region = b.region;
        this.teamSize = b.teamSize;
        this.ranked = b.ranked;
        this.tournament = b.tournament;
        this.processingState = b.processingState != null ? b.processingState : ProcessingState.UNKNOWN;
        this.releaseState = b.releaseState != null ? b.releaseState : ReleaseState.UNKNOWN;
        this.visibility = b.visibility != null ? b.visibility : CollectionVisibility.UNKNOWN;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.participants = b.participants != null
                ? Collections.unmodifiableList(new ArrayList<CollectionParticipant>(b.participants))
                : Collections.<CollectionParticipant>emptyList();
        this.teams = b.teams != null
                ? Collections.unmodifiableList(new ArrayList<CollectionTeam>(b.teams))
                : Collections.<CollectionTeam>emptyList();
        this.viewerRelation = b.viewerRelation;
        this.durationTicks = b.durationTicks;
        this.thumbnailReady = b.thumbnailReady;
        this.playbackEligible = b.playbackEligible;
        this.playable = b.playable;
        this.mediaReady = b.mediaReady;
        this.expired = b.expired;
    }

    /**
     * Returns the collection id.
     *
     * @return the collection id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the integration's own match identifier.
     *
     * @return the integration's own match identifier, or an empty optional
     */
    public Optional<String> getExternalMatchId() {
        return Optional.ofNullable(externalMatchId);
    }

    /**
     * Returns the integration-defined category.
     *
     * @return the integration-defined category; never {@code null}
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the integration-defined game mode.
     *
     * @return the integration-defined game mode; never {@code null}
     */
    public String getMode() {
        return mode;
    }

    /**
     * Returns the id of the release policy the collection was opened under.
     *
     * @return the id of the release policy the collection was opened under; never {@code null}
     */
    public String getPolicyId() {
        return policyId;
    }

    /**
     * Returns the id of the server that recorded the match.
     *
     * @return the id of the server that recorded the match; never {@code null}
     */
    public String getServerId() {
        return serverId;
    }

    /**
     * Returns the ids of the worlds the match took place in.
     *
     * @return the ids of the worlds the match took place in; never {@code null}
     */
    public List<String> getWorldIds() {
        return worldIds;
    }

    /**
     * Returns the tick at which the collection's scope began.
     *
     * @return the tick at which the collection's scope began
     */
    public long getStartTick() {
        return startTick;
    }

    /**
     * Returns the tick at which the scope ended; absent while the match is open.
     *
     * @return the tick at which the scope ended; absent while the match is open, or an empty
     *         optional
     */
    public Optional<Long> getEndTick() {
        return Optional.ofNullable(endTick);
    }

    /**
     * Returns when the scope began.
     *
     * @return when the scope began, or an empty optional
     */
    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    /**
     * Returns when the scope ended; absent while the match is open.
     *
     * @return when the scope ended; absent while the match is open, or an empty optional
     */
    public Optional<Instant> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    /**
     * Returns the collection's namespaced key/value metadata.
     *
     * @return the collection's namespaced key/value metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * Returns the id of the competition the match belongs to.
     *
     * @return the id of the competition the match belongs to, or an empty optional
     */
    public Optional<String> getCompetitionId() {
        return Optional.ofNullable(competitionId);
    }

    /**
     * Returns the id of the series the match belongs to.
     *
     * @return the id of the series the match belongs to, or an empty optional
     */
    public Optional<String> getSeriesId() {
        return Optional.ofNullable(seriesId);
    }

    /**
     * Returns the id of the round the match belongs to.
     *
     * @return the id of the round the match belongs to, or an empty optional
     */
    public Optional<String> getRoundId() {
        return Optional.ofNullable(roundId);
    }

    /**
     * Returns the id of the bracket match.
     *
     * @return the id of the bracket match, or an empty optional
     */
    public Optional<String> getBracketMatchId() {
        return Optional.ofNullable(bracketMatchId);
    }

    /**
     * Returns the game number within its series.
     *
     * @return the game number within its series, or an empty optional
     */
    public Optional<Integer> getGameNumber() {
        return Optional.ofNullable(gameNumber);
    }

    /**
     * Returns the length of the series, as a best-of count.
     *
     * @return the length of the series, as a best-of count, or an empty optional
     */
    public Optional<Integer> getBestOf() {
        return Optional.ofNullable(bestOf);
    }

    /**
     * Returns the id of the parent event that groups several matches.
     *
     * @return the id of the parent event that groups several matches, or an empty optional
     */
    public Optional<String> getPartyEventId() {
        return Optional.ofNullable(partyEventId);
    }

    /**
     * Returns the region the match was played in.
     *
     * @return the region the match was played in, or an empty optional
     */
    public Optional<String> getRegion() {
        return Optional.ofNullable(region);
    }

    /**
     * Returns the number of players per team.
     *
     * @return the number of players per team, or an empty optional
     */
    public Optional<Integer> getTeamSize() {
        return Optional.ofNullable(teamSize);
    }

    /**
     * Returns whether the match was ranked.
     *
     * @return whether the match was ranked, or an empty optional
     */
    public Optional<Boolean> getRanked() {
        return Optional.ofNullable(ranked);
    }

    /**
     * Returns whether the match was a tournament match.
     *
     * @return whether the match was a tournament match, or an empty optional
     */
    public Optional<Boolean> getTournament() {
        return Optional.ofNullable(tournament);
    }

    /**
     * Returns the collection's processing state.
     *
     * @return the collection's processing state
     */
    public ProcessingState getProcessingState() {
        return processingState;
    }

    /**
     * Returns the collection's embargo state.
     *
     * @return the collection's embargo state
     */
    public ReleaseState getReleaseState() {
        return releaseState;
    }

    /**
     * Returns who may watch the collection once released.
     *
     * @return who may watch the collection once released
     */
    public CollectionVisibility getVisibility() {
        return visibility;
    }

    /**
     * Returns when the collection was created.
     *
     * @return when the collection was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns when the collection was last updated.
     *
     * @return when the collection was last updated, or an empty optional
     */
    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    /**
     * Returns the roster of players in the match.
     *
     * Empty when the call does not return rosters.
     *
     * @return the roster of players in the match; never {@code null}
     */
    public List<CollectionParticipant> getParticipants() {
        return participants;
    }

    /**
     * Returns the teams in the match.
     *
     * Empty when the call does not return rosters.
     *
     * @return the teams in the match; never {@code null}
     */
    public List<CollectionTeam> getTeams() {
        return teams;
    }

    /**
     * Returns the requested player's relation to some asset of the collection.
     *
     * Present only when the request named a player.
     *
     * @return the requested player's relation to some asset of the collection, or an empty
     *         optional
     */
    public Optional<AssetRelation> getViewerRelation() {
        return Optional.ofNullable(viewerRelation);
    }

    /**
     * Returns the length of the match in ticks; absent while the match is open.
     *
     * @return the length of the match in ticks; absent while the match is open, or an empty
     *         optional
     */
    public Optional<Long> getDurationTicks() {
        return Optional.ofNullable(durationTicks);
    }

    /**
     * Returns whether a thumbnail can be shown for the collection.
     *
     * @return whether a thumbnail can be shown for the collection
     */
    public boolean isThumbnailReady() {
        return thumbnailReady;
    }

    /**
     * Returns whether the collection's footage is ready to play.
     *
     * This is a hint, not a grant: a watch ticket re-checks authorisation.
     *
     * @return whether the collection's footage is ready to play
     */
    public boolean isPlaybackEligible() {
        return playbackEligible;
    }

    /**
     * Returns whether the collection's footage is ready to play.
     *
     * @return whether the collection's footage is ready to play
     */
    public boolean isPlayable() {
        return playable;
    }

    /**
     * Returns whether the collection's media is ready.
     *
     * @return whether the collection's media is ready
     */
    public boolean isMediaReady() {
        return mediaReady;
    }

    /**
     * Returns whether the collection's footage has passed its retention period.
     *
     * @return whether the collection's footage has passed its retention period
     */
    public boolean isExpired() {
        return expired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayCollection)) {
            return false;
        }
        return id.equals(((ReplayCollection) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ReplayCollection{id=" + id
                + ", category=" + category
                + ", mode=" + mode
                + ", processingState=" + processingState
                + ", releaseState=" + releaseState + '}';
    }

    /**
     * Returns a new builder for assembling a {@link ReplayCollection}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param id the collection id; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link ReplayCollection}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String id;
        private String externalMatchId;
        private String category;
        private String mode;
        private String policyId;
        private String serverId;
        private List<String> worldIds;
        private long startTick;
        private Long endTick;
        private Instant startedAt;
        private Instant endedAt;
        private Map<String, String> metadata;
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
        private ProcessingState processingState;
        private ReleaseState releaseState;
        private CollectionVisibility visibility;
        private Instant createdAt;
        private Instant updatedAt;
        private List<CollectionParticipant> participants;
        private List<CollectionTeam> teams;
        private AssetRelation viewerRelation;
        private Long durationTicks;
        private boolean thumbnailReady;
        private boolean playbackEligible;
        private boolean playable;
        private boolean mediaReady;
        private boolean expired;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the integration's own match identifier.
         *
         * @param v the integration's own match identifier
         * @return this builder
         */
        public Builder externalMatchId(String v) { this.externalMatchId = v; return this; }

        /**
         * Sets the integration-defined category.
         *
         * @param v the integration-defined category
         * @return this builder
         */
        public Builder category(String v) { this.category = v; return this; }

        /**
         * Sets the integration-defined game mode.
         *
         * @param v the integration-defined game mode
         * @return this builder
         */
        public Builder mode(String v) { this.mode = v; return this; }

        /**
         * Sets the id of the release policy the collection was opened under.
         *
         * @param v the id of the release policy the collection was opened under
         * @return this builder
         */
        public Builder policyId(String v) { this.policyId = v; return this; }

        /**
         * Sets the id of the server that recorded the match.
         *
         * @param v the id of the server that recorded the match
         * @return this builder
         */
        public Builder serverId(String v) { this.serverId = v; return this; }

        /**
         * Sets the ids of the worlds the match took place in.
         *
         * @param v the ids of the worlds the match took place in
         * @return this builder
         */
        public Builder worldIds(List<String> v) { this.worldIds = v; return this; }

        /**
         * Sets the tick at which the collection's scope began.
         *
         * @param v the tick at which the collection's scope began
         * @return this builder
         */
        public Builder startTick(long v) { this.startTick = v; return this; }

        /**
         * Sets the tick at which the scope ended; absent while the match is open.
         *
         * @param v the tick at which the scope ended; absent while the match is open
         * @return this builder
         */
        public Builder endTick(Long v) { this.endTick = v; return this; }

        /**
         * Sets when the scope began.
         *
         * @param v when the scope began
         * @return this builder
         */
        public Builder startedAt(Instant v) { this.startedAt = v; return this; }

        /**
         * Sets when the scope ended; absent while the match is open.
         *
         * @param v when the scope ended; absent while the match is open
         * @return this builder
         */
        public Builder endedAt(Instant v) { this.endedAt = v; return this; }

        /**
         * Sets the collection's namespaced key/value metadata.
         *
         * @param v the collection's namespaced key/value metadata
         * @return this builder
         */
        public Builder metadata(Map<String, String> v) { this.metadata = v; return this; }

        /**
         * Sets the id of the competition the match belongs to.
         *
         * @param v the id of the competition the match belongs to
         * @return this builder
         */
        public Builder competitionId(String v) { this.competitionId = v; return this; }

        /**
         * Sets the id of the series the match belongs to.
         *
         * @param v the id of the series the match belongs to
         * @return this builder
         */
        public Builder seriesId(String v) { this.seriesId = v; return this; }

        /**
         * Sets the id of the round the match belongs to.
         *
         * @param v the id of the round the match belongs to
         * @return this builder
         */
        public Builder roundId(String v) { this.roundId = v; return this; }

        /**
         * Sets the id of the bracket match.
         *
         * @param v the id of the bracket match
         * @return this builder
         */
        public Builder bracketMatchId(String v) { this.bracketMatchId = v; return this; }

        /**
         * Sets the game number within its series.
         *
         * @param v the game number within its series
         * @return this builder
         */
        public Builder gameNumber(Integer v) { this.gameNumber = v; return this; }

        /**
         * Sets the length of the series, as a best-of count.
         *
         * @param v the length of the series, as a best-of count
         * @return this builder
         */
        public Builder bestOf(Integer v) { this.bestOf = v; return this; }

        /**
         * Sets the id of the parent event that groups several matches.
         *
         * @param v the id of the parent event that groups several matches
         * @return this builder
         */
        public Builder partyEventId(String v) { this.partyEventId = v; return this; }

        /**
         * Sets the region the match was played in.
         *
         * @param v the region the match was played in
         * @return this builder
         */
        public Builder region(String v) { this.region = v; return this; }

        /**
         * Sets the number of players per team.
         *
         * @param v the number of players per team
         * @return this builder
         */
        public Builder teamSize(Integer v) { this.teamSize = v; return this; }

        /**
         * Sets whether the match was ranked.
         *
         * @param v whether the match was ranked
         * @return this builder
         */
        public Builder ranked(Boolean v) { this.ranked = v; return this; }

        /**
         * Sets whether the match was a tournament match.
         *
         * @param v whether the match was a tournament match
         * @return this builder
         */
        public Builder tournament(Boolean v) { this.tournament = v; return this; }

        /**
         * Sets the collection's processing state.
         *
         * @param v the collection's processing state
         * @return this builder
         */
        public Builder processingState(ProcessingState v) { this.processingState = v; return this; }

        /**
         * Sets the collection's embargo state.
         *
         * @param v the collection's embargo state
         * @return this builder
         */
        public Builder releaseState(ReleaseState v) { this.releaseState = v; return this; }

        /**
         * Sets who may watch the collection once released.
         *
         * @param v who may watch the collection once released
         * @return this builder
         */
        public Builder visibility(CollectionVisibility v) { this.visibility = v; return this; }

        /**
         * Sets when the collection was created.
         *
         * @param v when the collection was created
         * @return this builder
         */
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        /**
         * Sets when the collection was last updated.
         *
         * @param v when the collection was last updated
         * @return this builder
         */
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        /**
         * Sets the roster of players in the match.
         *
         * @param v the roster of players in the match
         * @return this builder
         */
        public Builder participants(List<CollectionParticipant> v) { this.participants = v; return this; }

        /**
         * Sets the teams in the match.
         *
         * @param v the teams in the match
         * @return this builder
         */
        public Builder teams(List<CollectionTeam> v) { this.teams = v; return this; }

        /**
         * Sets the requested player's relation to some asset of the collection.
         *
         * @param v the requested player's relation to some asset of the collection
         * @return this builder
         */
        public Builder viewerRelation(AssetRelation v) { this.viewerRelation = v; return this; }

        /**
         * Sets the length of the match in ticks; absent while the match is open.
         *
         * @param v the length of the match in ticks; absent while the match is open
         * @return this builder
         */
        public Builder durationTicks(Long v) { this.durationTicks = v; return this; }

        /**
         * Sets whether a thumbnail can be shown for the collection.
         *
         * @param v whether a thumbnail can be shown for the collection
         * @return this builder
         */
        public Builder thumbnailReady(boolean v) { this.thumbnailReady = v; return this; }

        /**
         * Sets whether the collection's footage is ready to play.
         *
         * @param v whether the collection's footage is ready to play
         * @return this builder
         */
        public Builder playbackEligible(boolean v) { this.playbackEligible = v; return this; }

        /**
         * Sets whether the collection's footage is ready to play.
         *
         * @param v whether the collection's footage is ready to play
         * @return this builder
         */
        public Builder playable(boolean v) { this.playable = v; return this; }

        /**
         * Sets whether the collection's media is ready.
         *
         * @param v whether the collection's media is ready
         * @return this builder
         */
        public Builder mediaReady(boolean v) { this.mediaReady = v; return this; }

        /**
         * Sets whether the collection's footage has passed its retention period.
         *
         * @param v whether the collection's footage has passed its retention period
         * @return this builder
         */
        public Builder expired(boolean v) { this.expired = v; return this; }

        /**
         * Builds the immutable {@link ReplayCollection}.
         *
         * @return a new immutable instance
         */
        public ReplayCollection build() {
            return new ReplayCollection(this);
        }
    }
}
