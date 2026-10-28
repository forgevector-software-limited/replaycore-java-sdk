/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * One segment of redeemed playback, with a short-lived URL to read its bytes from.
 *
 * Immutable.
 */
public final class RedeemedSegment {

    private final int sequenceIndex;
    private final long startTick;
    private final long endTick;
    private final String url;
    private final long expiresIn;

    /**
     * Constructs a redeemed segment. Intended for internal deserialisation and tests.
     *
     * @param sequenceIndex the 0-based position of the segment in playback order
     * @param startTick the first tick played, in ticks of the archive
     * @param endTick the last tick played, in ticks of the archive
     * @param url the short-lived URL the segment's bytes are read from
     * @param expiresIn the URL's remaining lifetime, in seconds
     */
    public RedeemedSegment(
            int sequenceIndex,
            long startTick,
            long endTick,
            String url,
            long expiresIn) {
        this.sequenceIndex = sequenceIndex;
        this.startTick = startTick;
        this.endTick = endTick;
        this.url = url != null ? url : "";
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the 0-based position of the segment in playback order.
     *
     * @return the 0-based position of the segment in playback order
     */
    public int getSequenceIndex() {
        return sequenceIndex;
    }

    /**
     * Returns the first tick played, in ticks of the archive.
     *
     * @return the first tick played, in ticks of the archive
     */
    public long getStartTick() {
        return startTick;
    }

    /**
     * Returns the last tick played, in ticks of the archive.
     *
     * @return the last tick played, in ticks of the archive
     */
    public long getEndTick() {
        return endTick;
    }

    /**
     * Returns the short-lived URL the segment's bytes are read from.
     *
     * Treat it as a credential.
     *
     * @return the short-lived URL the segment's bytes are read from; never {@code null}
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the URL's remaining lifetime, in seconds.
     *
     * @return the URL's remaining lifetime, in seconds
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedeemedSegment)) {
            return false;
        }
        RedeemedSegment that = (RedeemedSegment) o;
        return sequenceIndex == that.sequenceIndex
                && startTick == that.startTick
                && endTick == that.endTick
                && Objects.equals(url, that.url)
                && expiresIn == that.expiresIn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceIndex, startTick, endTick, url, expiresIn);
    }

    @Override
    public String toString() {
        return "RedeemedSegment{sequenceIndex=" + sequenceIndex
                + ", startTick=" + startTick
                + ", endTick=" + endTick + '}';
    }
}
