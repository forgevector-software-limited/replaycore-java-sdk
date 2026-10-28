/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A timeline marker attached to a replay collection rather than to one replay.
 *
 * Immutable. Collection markers are written with a batch call and addressed by collection and
 * segment, so they do not carry a replay id.
 */
public final class ScopeMarker {

    private final String id;
    private final String collectionId;
    private final String segmentId;
    private final String clientEventId;
    private final long tick;
    private final String label;
    private final String category;
    private final String actor;
    private final String integration;
    private final Instant createdAt;
    private final int segmentSequenceIndex;
    private final String archiveId;

    private ScopeMarker(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.collectionId = b.collectionId != null ? b.collectionId : "";
        this.segmentId = b.segmentId != null ? b.segmentId : "";
        this.clientEventId = b.clientEventId;
        this.tick = b.tick;
        this.label = b.label != null ? b.label : "";
        this.category = b.category;
        this.actor = b.actor;
        this.integration = b.integration;
        this.createdAt = b.createdAt;
        this.segmentSequenceIndex = b.segmentSequenceIndex;
        this.archiveId = b.archiveId != null ? b.archiveId : "";
    }

    /**
     * Returns the marker id.
     *
     * @return the marker id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the collection the marker belongs to.
     *
     * @return the id of the collection the marker belongs to; never {@code null}
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the id of the segment the marker's tick belongs to.
     *
     * @return the id of the segment the marker's tick belongs to; never {@code null}
     */
    public String getSegmentId() {
        return segmentId;
    }

    /**
     * Returns the caller-supplied event id the marker was written under.
     *
     * @return the caller-supplied event id the marker was written under, or an empty optional
     */
    public Optional<String> getClientEventId() {
        return Optional.ofNullable(clientEventId);
    }

    /**
     * Returns the marker's position, in ticks of its segment.
     *
     * @return the marker's position, in ticks of its segment
     */
    public long getTick() {
        return tick;
    }

    /**
     * Returns the marker's visible label.
     *
     * @return the marker's visible label; never {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the marker's category.
     *
     * @return the marker's category, or an empty optional
     */
    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the marker's actor label.
     *
     * @return the marker's actor label, or an empty optional
     */
    public Optional<String> getActor() {
        return Optional.ofNullable(actor);
    }

    /**
     * Returns the integration that raised the marker.
     *
     * @return the integration that raised the marker, or an empty optional
     */
    public Optional<String> getIntegration() {
        return Optional.ofNullable(integration);
    }

    /**
     * Returns when the marker was created.
     *
     * @return when the marker was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns the 0-based sequence index of the marker's segment.
     *
     * @return the 0-based sequence index of the marker's segment
     */
    public int getSegmentSequenceIndex() {
        return segmentSequenceIndex;
    }

    /**
     * Returns the id of the archive the marker's segment was recorded into.
     *
     * @return the id of the archive the marker's segment was recorded into; never {@code null}
     */
    public String getArchiveId() {
        return archiveId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScopeMarker)) {
            return false;
        }
        return id.equals(((ScopeMarker) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ScopeMarker{id=" + id
                + ", collectionId=" + collectionId
                + ", tick=" + tick
                + ", label=" + label + '}';
    }

    /**
     * Returns a new builder for assembling a {@link ScopeMarker}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param id the marker id; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link ScopeMarker}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String id;
        private String collectionId;
        private String segmentId;
        private String clientEventId;
        private long tick;
        private String label;
        private String category;
        private String actor;
        private String integration;
        private Instant createdAt;
        private int segmentSequenceIndex;
        private String archiveId;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the id of the collection the marker belongs to.
         *
         * @param v the id of the collection the marker belongs to
         * @return this builder
         */
        public Builder collectionId(String v) { this.collectionId = v; return this; }

        /**
         * Sets the id of the segment the marker's tick belongs to.
         *
         * @param v the id of the segment the marker's tick belongs to
         * @return this builder
         */
        public Builder segmentId(String v) { this.segmentId = v; return this; }

        /**
         * Sets the caller-supplied event id the marker was written under.
         *
         * @param v the caller-supplied event id the marker was written under
         * @return this builder
         */
        public Builder clientEventId(String v) { this.clientEventId = v; return this; }

        /**
         * Sets the marker's position, in ticks of its segment.
         *
         * @param v the marker's position, in ticks of its segment
         * @return this builder
         */
        public Builder tick(long v) { this.tick = v; return this; }

        /**
         * Sets the marker's visible label.
         *
         * @param v the marker's visible label
         * @return this builder
         */
        public Builder label(String v) { this.label = v; return this; }

        /**
         * Sets the marker's category.
         *
         * @param v the marker's category
         * @return this builder
         */
        public Builder category(String v) { this.category = v; return this; }

        /**
         * Sets the marker's actor label.
         *
         * @param v the marker's actor label
         * @return this builder
         */
        public Builder actor(String v) { this.actor = v; return this; }

        /**
         * Sets the integration that raised the marker.
         *
         * @param v the integration that raised the marker
         * @return this builder
         */
        public Builder integration(String v) { this.integration = v; return this; }

        /**
         * Sets when the marker was created.
         *
         * @param v when the marker was created
         * @return this builder
         */
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        /**
         * Sets the 0-based sequence index of the marker's segment.
         *
         * @param v the 0-based sequence index of the marker's segment
         * @return this builder
         */
        public Builder segmentSequenceIndex(int v) { this.segmentSequenceIndex = v; return this; }

        /**
         * Sets the id of the archive the marker's segment was recorded into.
         *
         * @param v the id of the archive the marker's segment was recorded into
         * @return this builder
         */
        public Builder archiveId(String v) { this.archiveId = v; return this; }

        /**
         * Builds the immutable {@link ScopeMarker}.
         *
         * @return a new immutable instance
         */
        public ScopeMarker build() {
            return new ScopeMarker(this);
        }
    }
}
