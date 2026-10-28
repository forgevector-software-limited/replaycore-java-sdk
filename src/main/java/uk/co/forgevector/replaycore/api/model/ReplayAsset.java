/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A playable asset cut from a replay collection: its full match, or a clip or highlight of it.
 *
 * Immutable. The single-asset read also returns the asset's relationships; a player's replay
 * listing returns the full participant set instead. Where a call does not return a field, the
 * getter reads as empty, an empty list or {@code false}.
 */
public final class ReplayAsset {

    private final String id;
    private final String collectionId;
    private final AssetKind kind;
    private final EventKind eventKind;
    private final String segmentId;
    private final Long startTick;
    private final Long endTick;
    private final ProcessingState processingState;
    private final ReleaseState releaseState;
    private final CollectionVisibility visibility;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String externalMatchId;
    private final Instant collectionEndedAt;
    private final boolean gameplayBoundariesComplete;
    private final boolean authoritativeMatchComplete;
    private final List<AssetRelationship> relationships;
    private final AssetRelation viewerRelation;
    private final List<AssetParticipant> participants;
    private final Long durationTicks;
    private final boolean thumbnailReady;
    private final boolean playbackEligible;
    private final boolean playable;
    private final boolean mediaReady;
    private final boolean expired;
    private final String watchUrl;

    private ReplayAsset(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.collectionId = b.collectionId != null ? b.collectionId : "";
        this.kind = b.kind != null ? b.kind : AssetKind.UNKNOWN;
        this.eventKind = b.eventKind;
        this.segmentId = b.segmentId;
        this.startTick = b.startTick;
        this.endTick = b.endTick;
        this.processingState = b.processingState != null ? b.processingState : ProcessingState.UNKNOWN;
        this.releaseState = b.releaseState != null ? b.releaseState : ReleaseState.UNKNOWN;
        this.visibility = b.visibility != null ? b.visibility : CollectionVisibility.UNKNOWN;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.externalMatchId = b.externalMatchId;
        this.collectionEndedAt = b.collectionEndedAt;
        this.gameplayBoundariesComplete = b.gameplayBoundariesComplete;
        this.authoritativeMatchComplete = b.authoritativeMatchComplete;
        this.relationships = b.relationships != null
                ? Collections.unmodifiableList(new ArrayList<AssetRelationship>(b.relationships))
                : Collections.<AssetRelationship>emptyList();
        this.viewerRelation = b.viewerRelation;
        this.participants = b.participants != null
                ? Collections.unmodifiableList(new ArrayList<AssetParticipant>(b.participants))
                : Collections.<AssetParticipant>emptyList();
        this.durationTicks = b.durationTicks;
        this.thumbnailReady = b.thumbnailReady;
        this.playbackEligible = b.playbackEligible;
        this.playable = b.playable;
        this.mediaReady = b.mediaReady;
        this.expired = b.expired;
        this.watchUrl = b.watchUrl;
    }

    /**
     * Returns the asset id.
     *
     * @return the asset id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the collection the asset belongs to.
     *
     * @return the id of the collection the asset belongs to; never {@code null}
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the kind of asset.
     *
     * @return the kind of asset
     */
    public AssetKind getKind() {
        return kind;
    }

    /**
     * Returns the gameplay event the asset covers.
     *
     * @return the gameplay event the asset covers, or an empty optional
     */
    public Optional<EventKind> getEventKind() {
        return Optional.ofNullable(eventKind);
    }

    /**
     * Returns the id of the recording segment the asset is cut from.
     *
     * @return the id of the recording segment the asset is cut from, or an empty optional
     */
    public Optional<String> getSegmentId() {
        return Optional.ofNullable(segmentId);
    }

    /**
     * Returns the first tick of the asset's window; absent for a full match.
     *
     * @return the first tick of the asset's window; absent for a full match, or an empty optional
     */
    public Optional<Long> getStartTick() {
        return Optional.ofNullable(startTick);
    }

    /**
     * Returns the last tick of the asset's window; absent for a full match.
     *
     * @return the last tick of the asset's window; absent for a full match, or an empty optional
     */
    public Optional<Long> getEndTick() {
        return Optional.ofNullable(endTick);
    }

    /**
     * Returns the asset's processing state.
     *
     * @return the asset's processing state
     */
    public ProcessingState getProcessingState() {
        return processingState;
    }

    /**
     * Returns the asset's embargo state.
     *
     * @return the asset's embargo state
     */
    public ReleaseState getReleaseState() {
        return releaseState;
    }

    /**
     * Returns who may watch the asset once released.
     *
     * @return who may watch the asset once released
     */
    public CollectionVisibility getVisibility() {
        return visibility;
    }

    /**
     * Returns when the asset was created.
     *
     * @return when the asset was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns when the asset was last updated.
     *
     * @return when the asset was last updated, or an empty optional
     */
    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
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
     * Returns when the parent collection's scope ended.
     *
     * @return when the parent collection's scope ended, or an empty optional
     */
    public Optional<Instant> getCollectionEndedAt() {
        return Optional.ofNullable(collectionEndedAt);
    }

    /**
     * Returns whether the gameplay boundaries of the footage are complete.
     *
     * @return whether the gameplay boundaries of the footage are complete
     */
    public boolean isGameplayBoundariesComplete() {
        return gameplayBoundariesComplete;
    }

    /**
     * Returns whether the whole match is covered by authoritative footage.
     *
     * @return whether the whole match is covered by authoritative footage
     */
    public boolean isAuthoritativeMatchComplete() {
        return authoritativeMatchComplete;
    }

    /**
     * Returns the player relationships of the asset.
     *
     * Populated only by the single-asset read.
     *
     * @return the player relationships of the asset; never {@code null}
     */
    public List<AssetRelationship> getRelationships() {
        return relationships;
    }

    /**
     * Returns the requested player's relation to the asset.
     *
     * Present only when the request named a player.
     *
     * @return the requested player's relation to the asset, or an empty optional
     */
    public Optional<AssetRelation> getViewerRelation() {
        return Optional.ofNullable(viewerRelation);
    }

    /**
     * Returns the full set of players related to the asset.
     *
     * Populated only by a player's replay listing.
     *
     * @return the full set of players related to the asset; never {@code null}
     */
    public List<AssetParticipant> getParticipants() {
        return participants;
    }

    /**
     * Returns the length of the asset in ticks.
     *
     * @return the length of the asset in ticks, or an empty optional
     */
    public Optional<Long> getDurationTicks() {
        return Optional.ofNullable(durationTicks);
    }

    /**
     * Returns whether a thumbnail can be shown for the asset.
     *
     * @return whether a thumbnail can be shown for the asset
     */
    public boolean isThumbnailReady() {
        return thumbnailReady;
    }

    /**
     * Returns whether the asset's footage is ready to play.
     *
     * This is a hint, not a grant: a watch ticket re-checks authorisation.
     *
     * @return whether the asset's footage is ready to play
     */
    public boolean isPlaybackEligible() {
        return playbackEligible;
    }

    /**
     * Returns whether the asset's footage is ready to play.
     *
     * @return whether the asset's footage is ready to play
     */
    public boolean isPlayable() {
        return playable;
    }

    /**
     * Returns whether the asset's media is ready.
     *
     * @return whether the asset's media is ready
     */
    public boolean isMediaReady() {
        return mediaReady;
    }

    /**
     * Returns whether the asset's footage has passed its retention period.
     *
     * @return whether the asset's footage has passed its retention period
     */
    public boolean isExpired() {
        return expired;
    }

    /**
     * Returns the durable browser watch link.
     *
     * Present only for a released, public or unlisted asset on a plan that includes hosted watch
     * links.
     *
     * @return the durable browser watch link, or an empty optional
     */
    public Optional<String> getWatchUrl() {
        return Optional.ofNullable(watchUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayAsset)) {
            return false;
        }
        return id.equals(((ReplayAsset) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ReplayAsset{id=" + id
                + ", collectionId=" + collectionId
                + ", kind=" + kind
                + ", processingState=" + processingState
                + ", releaseState=" + releaseState + '}';
    }

    /**
     * Returns a new builder for assembling a {@link ReplayAsset}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param id the asset id; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link ReplayAsset}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String id;
        private String collectionId;
        private AssetKind kind;
        private EventKind eventKind;
        private String segmentId;
        private Long startTick;
        private Long endTick;
        private ProcessingState processingState;
        private ReleaseState releaseState;
        private CollectionVisibility visibility;
        private Instant createdAt;
        private Instant updatedAt;
        private String externalMatchId;
        private Instant collectionEndedAt;
        private boolean gameplayBoundariesComplete;
        private boolean authoritativeMatchComplete;
        private List<AssetRelationship> relationships;
        private AssetRelation viewerRelation;
        private List<AssetParticipant> participants;
        private Long durationTicks;
        private boolean thumbnailReady;
        private boolean playbackEligible;
        private boolean playable;
        private boolean mediaReady;
        private boolean expired;
        private String watchUrl;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the id of the collection the asset belongs to.
         *
         * @param v the id of the collection the asset belongs to
         * @return this builder
         */
        public Builder collectionId(String v) { this.collectionId = v; return this; }

        /**
         * Sets the kind of asset.
         *
         * @param v the kind of asset
         * @return this builder
         */
        public Builder kind(AssetKind v) { this.kind = v; return this; }

        /**
         * Sets the gameplay event the asset covers.
         *
         * @param v the gameplay event the asset covers
         * @return this builder
         */
        public Builder eventKind(EventKind v) { this.eventKind = v; return this; }

        /**
         * Sets the id of the recording segment the asset is cut from.
         *
         * @param v the id of the recording segment the asset is cut from
         * @return this builder
         */
        public Builder segmentId(String v) { this.segmentId = v; return this; }

        /**
         * Sets the first tick of the asset's window; absent for a full match.
         *
         * @param v the first tick of the asset's window; absent for a full match
         * @return this builder
         */
        public Builder startTick(Long v) { this.startTick = v; return this; }

        /**
         * Sets the last tick of the asset's window; absent for a full match.
         *
         * @param v the last tick of the asset's window; absent for a full match
         * @return this builder
         */
        public Builder endTick(Long v) { this.endTick = v; return this; }

        /**
         * Sets the asset's processing state.
         *
         * @param v the asset's processing state
         * @return this builder
         */
        public Builder processingState(ProcessingState v) { this.processingState = v; return this; }

        /**
         * Sets the asset's embargo state.
         *
         * @param v the asset's embargo state
         * @return this builder
         */
        public Builder releaseState(ReleaseState v) { this.releaseState = v; return this; }

        /**
         * Sets who may watch the asset once released.
         *
         * @param v who may watch the asset once released
         * @return this builder
         */
        public Builder visibility(CollectionVisibility v) { this.visibility = v; return this; }

        /**
         * Sets when the asset was created.
         *
         * @param v when the asset was created
         * @return this builder
         */
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        /**
         * Sets when the asset was last updated.
         *
         * @param v when the asset was last updated
         * @return this builder
         */
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        /**
         * Sets the integration's own match identifier.
         *
         * @param v the integration's own match identifier
         * @return this builder
         */
        public Builder externalMatchId(String v) { this.externalMatchId = v; return this; }

        /**
         * Sets when the parent collection's scope ended.
         *
         * @param v when the parent collection's scope ended
         * @return this builder
         */
        public Builder collectionEndedAt(Instant v) { this.collectionEndedAt = v; return this; }

        /**
         * Sets whether the gameplay boundaries of the footage are complete.
         *
         * @param v whether the gameplay boundaries of the footage are complete
         * @return this builder
         */
        public Builder gameplayBoundariesComplete(boolean v) { this.gameplayBoundariesComplete = v; return this; }

        /**
         * Sets whether the whole match is covered by authoritative footage.
         *
         * @param v whether the whole match is covered by authoritative footage
         * @return this builder
         */
        public Builder authoritativeMatchComplete(boolean v) { this.authoritativeMatchComplete = v; return this; }

        /**
         * Sets the player relationships of the asset.
         *
         * @param v the player relationships of the asset
         * @return this builder
         */
        public Builder relationships(List<AssetRelationship> v) { this.relationships = v; return this; }

        /**
         * Sets the requested player's relation to the asset.
         *
         * @param v the requested player's relation to the asset
         * @return this builder
         */
        public Builder viewerRelation(AssetRelation v) { this.viewerRelation = v; return this; }

        /**
         * Sets the full set of players related to the asset.
         *
         * @param v the full set of players related to the asset
         * @return this builder
         */
        public Builder participants(List<AssetParticipant> v) { this.participants = v; return this; }

        /**
         * Sets the length of the asset in ticks.
         *
         * @param v the length of the asset in ticks
         * @return this builder
         */
        public Builder durationTicks(Long v) { this.durationTicks = v; return this; }

        /**
         * Sets whether a thumbnail can be shown for the asset.
         *
         * @param v whether a thumbnail can be shown for the asset
         * @return this builder
         */
        public Builder thumbnailReady(boolean v) { this.thumbnailReady = v; return this; }

        /**
         * Sets whether the asset's footage is ready to play.
         *
         * @param v whether the asset's footage is ready to play
         * @return this builder
         */
        public Builder playbackEligible(boolean v) { this.playbackEligible = v; return this; }

        /**
         * Sets whether the asset's footage is ready to play.
         *
         * @param v whether the asset's footage is ready to play
         * @return this builder
         */
        public Builder playable(boolean v) { this.playable = v; return this; }

        /**
         * Sets whether the asset's media is ready.
         *
         * @param v whether the asset's media is ready
         * @return this builder
         */
        public Builder mediaReady(boolean v) { this.mediaReady = v; return this; }

        /**
         * Sets whether the asset's footage has passed its retention period.
         *
         * @param v whether the asset's footage has passed its retention period
         * @return this builder
         */
        public Builder expired(boolean v) { this.expired = v; return this; }

        /**
         * Sets the durable browser watch link.
         *
         * @param v the durable browser watch link
         * @return this builder
         */
        public Builder watchUrl(String v) { this.watchUrl = v; return this; }

        /**
         * Builds the immutable {@link ReplayAsset}.
         *
         * @return a new immutable instance
         */
        public ReplayAsset build() {
            return new ReplayAsset(this);
        }
    }
}
