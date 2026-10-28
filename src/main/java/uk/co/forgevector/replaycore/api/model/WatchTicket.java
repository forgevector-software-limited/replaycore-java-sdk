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
 * A single-use ticket for watching a replay asset, or the notice that the asset is not ready yet.
 *
 * Immutable. When {@link #isPreparing()} is {@code true} the asset's media is still being
 * produced: no ticket was issued, and the call can be repeated after {@link
 * #getRetryAfterSeconds()}. Otherwise the ticket is redeemed with {@code redeemWatchTicket}.
 */
public final class WatchTicket {

    private final String assetId;
    private final String id;
    private final String ticket;
    private final String redemptionUrl;
    private final List<PlaybackSegment> playback;
    private final Instant expiresAt;
    private final Long expiresIn;
    private final boolean singleUse;
    private final ProcessingState processingState;
    private final Integer retryAfterSeconds;

    private WatchTicket(Builder b) {
        this.assetId = Objects.requireNonNull(b.assetId, "assetId");
        this.id = b.id;
        this.ticket = b.ticket;
        this.redemptionUrl = b.redemptionUrl;
        this.playback = b.playback != null
                ? Collections.unmodifiableList(new ArrayList<PlaybackSegment>(b.playback))
                : Collections.<PlaybackSegment>emptyList();
        this.expiresAt = b.expiresAt;
        this.expiresIn = b.expiresIn;
        this.singleUse = b.singleUse;
        this.processingState = b.processingState;
        this.retryAfterSeconds = b.retryAfterSeconds;
    }

    /**
     * Returns the id of the asset the ticket plays.
     *
     * @return the id of the asset the ticket plays; never {@code null}
     */
    public String getAssetId() {
        return assetId;
    }

    /**
     * Returns the ticket id.
     *
     * @return the ticket id, or an empty optional
     */
    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the ticket secret to present when redeeming.
     *
     * Treat it as a credential: it is shown once and is never logged by the SDK.
     *
     * @return the ticket secret to present when redeeming, or an empty optional
     */
    public Optional<String> getTicket() {
        return Optional.ofNullable(ticket);
    }

    /**
     * Returns the URL the ticket is redeemed at.
     *
     * @return the URL the ticket is redeemed at, or an empty optional
     */
    public Optional<String> getRedemptionUrl() {
        return Optional.ofNullable(redemptionUrl);
    }

    /**
     * Returns the segments the ticket plays.
     *
     * Names the segments only; it carries no storage URL.
     *
     * @return the segments the ticket plays; never {@code null}
     */
    public List<PlaybackSegment> getPlayback() {
        return playback;
    }

    /**
     * Returns when the ticket expires.
     *
     * @return when the ticket expires, or an empty optional
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * Returns the ticket's remaining lifetime, in seconds.
     *
     * @return the ticket's remaining lifetime, in seconds, or an empty optional
     */
    public Optional<Long> getExpiresIn() {
        return Optional.ofNullable(expiresIn);
    }

    /**
     * Returns whether the ticket can be redeemed only once.
     *
     * @return whether the ticket can be redeemed only once
     */
    public boolean isSingleUse() {
        return singleUse;
    }

    /**
     * Returns the asset's processing state while it is not ready.
     *
     * @return the asset's processing state while it is not ready, or an empty optional
     */
    public Optional<ProcessingState> getProcessingState() {
        return Optional.ofNullable(processingState);
    }

    /**
     * Returns how long to wait before asking again, in seconds, while the asset is not ready.
     *
     * @return how long to wait before asking again, in seconds, while the asset is not ready, or
     *         an empty optional
     */
    public Optional<Integer> getRetryAfterSeconds() {
        return Optional.ofNullable(retryAfterSeconds);
    }

    /**
     * Returns whether the asset is still being prepared. No ticket is issued
     * until it is ready.
     *
     * @return {@code true} if the response is a not-ready notice
     */
    public boolean isPreparing() {
        return retryAfterSeconds != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WatchTicket)) {
            return false;
        }
        WatchTicket that = (WatchTicket) o;
        return Objects.equals(assetId, that.assetId)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, id);
    }

    @Override
    public String toString() {
        return "WatchTicket{assetId=" + assetId
                + ", id=" + id + '}';
    }

    /**
     * Returns a new builder for assembling a {@link WatchTicket}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param assetId the id of the asset the ticket plays; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String assetId) {
        return new Builder(assetId);
    }

    /**
     * A mutable builder for {@link WatchTicket}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String assetId;
        private String id;
        private String ticket;
        private String redemptionUrl;
        private List<PlaybackSegment> playback;
        private Instant expiresAt;
        private Long expiresIn;
        private boolean singleUse;
        private ProcessingState processingState;
        private Integer retryAfterSeconds;

        private Builder(String assetId) {
            this.assetId = Objects.requireNonNull(assetId, "assetId");
        }

        /**
         * Sets the ticket id.
         *
         * @param v the ticket id
         * @return this builder
         */
        public Builder id(String v) { this.id = v; return this; }

        /**
         * Sets the ticket secret to present when redeeming.
         *
         * @param v the ticket secret to present when redeeming
         * @return this builder
         */
        public Builder ticket(String v) { this.ticket = v; return this; }

        /**
         * Sets the URL the ticket is redeemed at.
         *
         * @param v the URL the ticket is redeemed at
         * @return this builder
         */
        public Builder redemptionUrl(String v) { this.redemptionUrl = v; return this; }

        /**
         * Sets the segments the ticket plays.
         *
         * @param v the segments the ticket plays
         * @return this builder
         */
        public Builder playback(List<PlaybackSegment> v) { this.playback = v; return this; }

        /**
         * Sets when the ticket expires.
         *
         * @param v when the ticket expires
         * @return this builder
         */
        public Builder expiresAt(Instant v) { this.expiresAt = v; return this; }

        /**
         * Sets the ticket's remaining lifetime, in seconds.
         *
         * @param v the ticket's remaining lifetime, in seconds
         * @return this builder
         */
        public Builder expiresIn(Long v) { this.expiresIn = v; return this; }

        /**
         * Sets whether the ticket can be redeemed only once.
         *
         * @param v whether the ticket can be redeemed only once
         * @return this builder
         */
        public Builder singleUse(boolean v) { this.singleUse = v; return this; }

        /**
         * Sets the asset's processing state while it is not ready.
         *
         * @param v the asset's processing state while it is not ready
         * @return this builder
         */
        public Builder processingState(ProcessingState v) { this.processingState = v; return this; }

        /**
         * Sets how long to wait before asking again, in seconds, while the asset is not ready.
         *
         * @param v how long to wait before asking again, in seconds, while the asset is not ready
         * @return this builder
         */
        public Builder retryAfterSeconds(Integer v) { this.retryAfterSeconds = v; return this; }

        /**
         * Builds the immutable {@link WatchTicket}.
         *
         * @return a new immutable instance
         */
        public WatchTicket build() {
            return new WatchTicket(this);
        }
    }
}
