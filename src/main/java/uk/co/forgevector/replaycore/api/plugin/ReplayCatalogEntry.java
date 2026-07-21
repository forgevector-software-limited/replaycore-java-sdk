/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One row of a {@link ReplayCatalogPage}: a {@code ReplayAsset} the queried player relates to, exactly as
 * {@code GET /v1/players/{uuid}/replays} (RFC-0009 section 9) returns it.
 *
 * <h2>{@link #viewerRelation()} is resolved PER ROW, for the queried player only</h2>
 * <p>A combat moment produces exactly one asset (RFC-0009 section 2.4); per-player meaning is carried by a
 * relationship row, never by a second copy of the asset. Consequently the SAME asset id can appear in one
 * player's page as {@link AssetRelationship#KILL} and in another player's page as
 * {@link AssetRelationship#DEATH} - {@link #viewerRelation()} is never a property of the asset itself, only
 * of this one entry's answer to "how does the queried player relate to this asset".
 *
 * <h2>Held content never appears here</h2>
 * <p>Every entry has already passed the cloud's embargo gate (RFC-0009 section 7) for the caller's
 * credentials; a held, revoked or otherwise unauthorized asset is never represented by an entry with some
 * "hidden" marker, it is simply absent from the page.
 *
 * <p>Build one with the fluent {@link #builder(String, String, AssetKind)}; the three identity fields keep
 * a longer positional constructor error-prone, matching {@link ReplayOperationResult}'s own precedent for
 * this field count. {@link #processingState()}, {@link #releaseState()}, {@link #visibility()},
 * {@link #createdAt()} and {@link #updatedAt()} are always present on the wire and therefore required at
 * {@link #build()}, even though they are supplied through the builder rather than the required constructor
 * arguments.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ReplayCatalogEntry {

    private final String assetId;
    private final String collectionId;
    private final AssetKind kind;
    private final EventKind eventKind;
    private final AssetRelationship viewerRelation;
    private final ProcessingState processingState;
    private final ReleaseState releaseState;
    private final ReplayVisibility visibility;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Long durationTicks;
    private final boolean thumbnailReady;
    private final boolean playbackEligible;
    private final boolean expired;

    private ReplayCatalogEntry(Builder b) {
        this.assetId = requiredIdentifier(b.assetId, "assetId");
        this.collectionId = requiredIdentifier(b.collectionId, "collectionId");
        this.kind = Objects.requireNonNull(b.kind, "kind must not be null");
        this.eventKind = b.eventKind;
        this.viewerRelation = b.viewerRelation;
        this.processingState = Objects.requireNonNull(b.processingState, "processingState must not be null");
        this.releaseState = Objects.requireNonNull(b.releaseState, "releaseState must not be null");
        this.visibility = Objects.requireNonNull(b.visibility, "visibility must not be null");
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(b.updatedAt, "updatedAt must not be null");
        this.durationTicks = b.durationTicks;
        this.thumbnailReady = b.thumbnailReady;
        this.playbackEligible = b.playbackEligible;
        this.expired = b.expired;
    }

    /**
     * Starts building an entry with its three identity fields.
     *
     * @param assetId      the asset's id (required, non-blank)
     * @param collectionId the id of the collection this asset belongs to (required, non-blank)
     * @param kind         what kind of asset this is; must not be {@code null}
     * @return a new builder
     */
    public static Builder builder(String assetId, String collectionId, AssetKind kind) {
        return new Builder(assetId, collectionId, kind);
    }

    /** @return the asset's id; never {@code null} */
    public String assetId() { return assetId; }
    /** @return the id of the collection this asset belongs to; never {@code null} */
    public String collectionId() { return collectionId; }
    /** @return what kind of asset this is; never {@code null} */
    public AssetKind kind() { return kind; }
    /** @return the in-game moment kind, present only when {@link #kind()} is {@link AssetKind#EVENT_CLIP} */
    public Optional<EventKind> eventKind() { return Optional.ofNullable(eventKind); }
    /** @return how the queried player relates to this asset; never absent for a row returned by
     *          {@link ReplayCatalogApi#listForPlayer}, which selects only rows with a resolved relation */
    public Optional<AssetRelationship> viewerRelation() { return Optional.ofNullable(viewerRelation); }
    /** @return the asset's processing lifecycle state; never {@code null} */
    public ProcessingState processingState() { return processingState; }
    /** @return whether an operator has made this asset reachable; never {@code null} */
    public ReleaseState releaseState() { return releaseState; }
    /** @return who this asset is reachable to once released; never {@code null} */
    public ReplayVisibility visibility() { return visibility; }
    /** @return when this asset was created; never {@code null} */
    public Instant createdAt() { return createdAt; }
    /** @return when this asset was last updated; never {@code null} */
    public Instant updatedAt() { return updatedAt; }
    /** @return the asset's length in ticks, present when the asset carries its own window (every kind
     *          except {@link AssetKind#FULL_MATCH} while its collection is still open) */
    public Optional<Long> durationTicks() { return Optional.ofNullable(durationTicks); }
    /** @return a coarse display hint for whether a thumbnail is ready; never itself a playback grant */
    public boolean thumbnailReady() { return thumbnailReady; }
    /** @return a coarse display hint for whether this asset looks playable; the authoritative check is
     *          still {@link ReplayCatalogApi#createWatchTicket} */
    public boolean playbackEligible() { return playbackEligible; }
    /** @return whether retention has expired this asset before it could be watched */
    public boolean expired() { return expired; }

    private static String requiredIdentifier(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }

    /** A fluent builder for {@link ReplayCatalogEntry}. Not thread-safe; build one entry per builder. */
    public static final class Builder {
        private final String assetId;
        private final String collectionId;
        private final AssetKind kind;
        private EventKind eventKind;
        private AssetRelationship viewerRelation;
        private ProcessingState processingState;
        private ReleaseState releaseState;
        private ReplayVisibility visibility;
        private Instant createdAt;
        private Instant updatedAt;
        private Long durationTicks;
        private boolean thumbnailReady;
        private boolean playbackEligible;
        private boolean expired;

        private Builder(String assetId, String collectionId, AssetKind kind) {
            this.assetId = assetId;
            this.collectionId = collectionId;
            this.kind = kind;
        }

        /**
         * Sets the in-game moment kind (applies only when the entry's kind is {@link AssetKind#EVENT_CLIP}).
         *
         * @param eventKind the event kind, or {@code null} to clear
         * @return this builder
         */
        public Builder eventKind(EventKind eventKind) {
            this.eventKind = eventKind;
            return this;
        }

        /**
         * Sets how the queried player relates to this asset.
         *
         * @param viewerRelation the relation, or {@code null} to clear
         * @return this builder
         */
        public Builder viewerRelation(AssetRelationship viewerRelation) {
            this.viewerRelation = viewerRelation;
            return this;
        }

        /**
         * Sets the asset's processing lifecycle state (required at {@link #build()}).
         *
         * @param processingState the processing state; must not be {@code null}
         * @return this builder
         */
        public Builder processingState(ProcessingState processingState) {
            this.processingState = processingState;
            return this;
        }

        /**
         * Sets whether an operator has made this asset reachable (required at {@link #build()}).
         *
         * @param releaseState the release state; must not be {@code null}
         * @return this builder
         */
        public Builder releaseState(ReleaseState releaseState) {
            this.releaseState = releaseState;
            return this;
        }

        /**
         * Sets who this asset is reachable to once released (required at {@link #build()}).
         *
         * @param visibility the visibility; must not be {@code null}
         * @return this builder
         */
        public Builder visibility(ReplayVisibility visibility) {
            this.visibility = visibility;
            return this;
        }

        /**
         * Sets when this asset was created (required at {@link #build()}).
         *
         * @param createdAt the creation instant; must not be {@code null}
         * @return this builder
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets when this asset was last updated (required at {@link #build()}).
         *
         * @param updatedAt the last-updated instant; must not be {@code null}
         * @return this builder
         */
        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the asset's length in ticks.
         *
         * @param durationTicks the duration, or {@code null} to clear
         * @return this builder
         */
        public Builder durationTicks(Long durationTicks) {
            this.durationTicks = durationTicks;
            return this;
        }

        /**
         * Sets the thumbnail-readiness display hint.
         *
         * @param thumbnailReady {@code true} if a thumbnail is ready
         * @return this builder
         */
        public Builder thumbnailReady(boolean thumbnailReady) {
            this.thumbnailReady = thumbnailReady;
            return this;
        }

        /**
         * Sets the playback-eligibility display hint.
         *
         * @param playbackEligible {@code true} if this asset looks playable
         * @return this builder
         */
        public Builder playbackEligible(boolean playbackEligible) {
            this.playbackEligible = playbackEligible;
            return this;
        }

        /**
         * Sets whether retention has expired this asset.
         *
         * @param expired {@code true} if expired
         * @return this builder
         */
        public Builder expired(boolean expired) {
            this.expired = expired;
            return this;
        }

        /**
         * Builds the immutable entry, applying all field validation.
         *
         * @return a new {@link ReplayCatalogEntry}
         * @throws IllegalArgumentException if a required field is blank
         * @throws NullPointerException     if {@code kind}, {@code processingState}, {@code releaseState},
         *                                   {@code visibility}, {@code createdAt} or {@code updatedAt} is
         *                                   {@code null}
         */
        public ReplayCatalogEntry build() {
            return new ReplayCatalogEntry(this);
        }
    }
}
