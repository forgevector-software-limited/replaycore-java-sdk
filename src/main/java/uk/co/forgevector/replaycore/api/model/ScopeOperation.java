/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome of an idempotent collection or asset operation: updating or finalising a collection,
 * or creating or finalising an asset.
 *
 * Immutable. Resending a request with the same idempotency key returns the original operation.
 */
public final class ScopeOperation {

    private final String id;
    private final String collectionId;
    private final String assetId;
    private final OperationStatus status;
    private final ProcessingState processingState;
    private final String failureCode;
    private final boolean retryable;
    private final long attemptGeneration;
    private final Instant playableAt;
    private final String resourceUrl;
    private final String watchUrl;
    private final boolean watchTicketEligible;
    private final boolean linkReady;
    private final boolean playable;

    private ScopeOperation(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.collectionId = b.collectionId;
        this.assetId = b.assetId;
        this.status = b.status != null ? b.status : OperationStatus.UNKNOWN;
        this.processingState = b.processingState;
        this.failureCode = b.failureCode;
        this.retryable = b.retryable;
        this.attemptGeneration = b.attemptGeneration;
        this.playableAt = b.playableAt;
        this.resourceUrl = b.resourceUrl;
        this.watchUrl = b.watchUrl;
        this.watchTicketEligible = b.watchTicketEligible;
        this.linkReady = b.linkReady;
        this.playable = b.playable;
    }

    /**
     * Returns the operation id.
     *
     * @return the operation id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the collection the operation applied to.
     *
     * @return the id of the collection the operation applied to, or an empty optional
     */
    public Optional<String> getCollectionId() {
        return Optional.ofNullable(collectionId);
    }

    /**
     * Returns the id of the asset the operation produced or changed.
     *
     * @return the id of the asset the operation produced or changed, or an empty optional
     */
    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    /**
     * Returns the operation status.
     *
     * @return the operation status
     */
    public OperationStatus getStatus() {
        return status;
    }

    /**
     * Returns the processing state the operation settled the target in.
     *
     * @return the processing state the operation settled the target in, or an empty optional
     */
    public Optional<ProcessingState> getProcessingState() {
        return Optional.ofNullable(processingState);
    }

    /**
     * Returns the code explaining a failed operation.
     *
     * @return the code explaining a failed operation, or an empty optional
     */
    public Optional<String> getFailureCode() {
        return Optional.ofNullable(failureCode);
    }

    /**
     * Returns whether a failed operation may be retried.
     *
     * @return whether a failed operation may be retried
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Returns the number of times the operation was retried.
     *
     * @return the number of times the operation was retried
     */
    public long getAttemptGeneration() {
        return attemptGeneration;
    }

    /**
     * Returns when the target became playable.
     *
     * @return when the target became playable, or an empty optional
     */
    public Optional<Instant> getPlayableAt() {
        return Optional.ofNullable(playableAt);
    }

    /**
     * Returns the API path of the resource the operation produced or changed.
     *
     * @return the API path of the resource the operation produced or changed, or an empty optional
     */
    public Optional<String> getResourceUrl() {
        return Optional.ofNullable(resourceUrl);
    }

    /**
     * Returns the browser watch link, when one exists.
     *
     * @return the browser watch link, when one exists, or an empty optional
     */
    public Optional<String> getWatchUrl() {
        return Optional.ofNullable(watchUrl);
    }

    /**
     * Returns whether a watch ticket can be issued for the target.
     *
     * @return whether a watch ticket can be issued for the target
     */
    public boolean isWatchTicketEligible() {
        return watchTicketEligible;
    }

    /**
     * Returns whether the target's resource link can be followed.
     *
     * @return whether the target's resource link can be followed
     */
    public boolean isLinkReady() {
        return linkReady;
    }

    /**
     * Returns whether the target is playable now.
     *
     * @return whether the target is playable now
     */
    public boolean isPlayable() {
        return playable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScopeOperation)) {
            return false;
        }
        return id.equals(((ScopeOperation) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ScopeOperation{id=" + id
                + ", collectionId=" + collectionId
                + ", assetId=" + assetId
                + ", status=" + status + '}';
    }

    /**
     * Returns a new builder for assembling a {@link ScopeOperation}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param id the operation id; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link ScopeOperation}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String id;
        private String collectionId;
        private String assetId;
        private OperationStatus status;
        private ProcessingState processingState;
        private String failureCode;
        private boolean retryable;
        private long attemptGeneration;
        private Instant playableAt;
        private String resourceUrl;
        private String watchUrl;
        private boolean watchTicketEligible;
        private boolean linkReady;
        private boolean playable;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the id of the collection the operation applied to.
         *
         * @param v the id of the collection the operation applied to
         * @return this builder
         */
        public Builder collectionId(String v) { this.collectionId = v; return this; }

        /**
         * Sets the id of the asset the operation produced or changed.
         *
         * @param v the id of the asset the operation produced or changed
         * @return this builder
         */
        public Builder assetId(String v) { this.assetId = v; return this; }

        /**
         * Sets the operation status.
         *
         * @param v the operation status
         * @return this builder
         */
        public Builder status(OperationStatus v) { this.status = v; return this; }

        /**
         * Sets the processing state the operation settled the target in.
         *
         * @param v the processing state the operation settled the target in
         * @return this builder
         */
        public Builder processingState(ProcessingState v) { this.processingState = v; return this; }

        /**
         * Sets the code explaining a failed operation.
         *
         * @param v the code explaining a failed operation
         * @return this builder
         */
        public Builder failureCode(String v) { this.failureCode = v; return this; }

        /**
         * Sets whether a failed operation may be retried.
         *
         * @param v whether a failed operation may be retried
         * @return this builder
         */
        public Builder retryable(boolean v) { this.retryable = v; return this; }

        /**
         * Sets the number of times the operation was retried.
         *
         * @param v the number of times the operation was retried
         * @return this builder
         */
        public Builder attemptGeneration(long v) { this.attemptGeneration = v; return this; }

        /**
         * Sets when the target became playable.
         *
         * @param v when the target became playable
         * @return this builder
         */
        public Builder playableAt(Instant v) { this.playableAt = v; return this; }

        /**
         * Sets the API path of the resource the operation produced or changed.
         *
         * @param v the API path of the resource the operation produced or changed
         * @return this builder
         */
        public Builder resourceUrl(String v) { this.resourceUrl = v; return this; }

        /**
         * Sets the browser watch link, when one exists.
         *
         * @param v the browser watch link, when one exists
         * @return this builder
         */
        public Builder watchUrl(String v) { this.watchUrl = v; return this; }

        /**
         * Sets whether a watch ticket can be issued for the target.
         *
         * @param v whether a watch ticket can be issued for the target
         * @return this builder
         */
        public Builder watchTicketEligible(boolean v) { this.watchTicketEligible = v; return this; }

        /**
         * Sets whether the target's resource link can be followed.
         *
         * @param v whether the target's resource link can be followed
         * @return this builder
         */
        public Builder linkReady(boolean v) { this.linkReady = v; return this; }

        /**
         * Sets whether the target is playable now.
         *
         * @param v whether the target is playable now
         * @return this builder
         */
        public Builder playable(boolean v) { this.playable = v; return this; }

        /**
         * Builds the immutable {@link ScopeOperation}.
         *
         * @return a new immutable instance
         */
        public ScopeOperation build() {
            return new ScopeOperation(this);
        }
    }
}
