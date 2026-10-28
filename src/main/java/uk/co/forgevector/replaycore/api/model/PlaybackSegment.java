/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * One recording segment a watch ticket plays.
 *
 * Immutable. A match that was rotated across several archives plays as several segments in
 * sequence.
 */
public final class PlaybackSegment {

    private final String replayId;
    private final int sequenceIndex;
    private final long startTick;
    private final long endTick;

    /**
     * Constructs a playback segment. Intended for internal deserialisation and tests.
     *
     * @param replayId the id of the replay archive the segment is read from
     * @param sequenceIndex the 0-based position of the segment in playback order
     * @param startTick the first tick played, in ticks of the archive
     * @param endTick the last tick played, in ticks of the archive
     */
    public PlaybackSegment(String replayId, int sequenceIndex, long startTick, long endTick) {
        this.replayId = replayId != null ? replayId : "";
        this.sequenceIndex = sequenceIndex;
        this.startTick = startTick;
        this.endTick = endTick;
    }

    /**
     * Returns the id of the replay archive the segment is read from.
     *
     * @return the id of the replay archive the segment is read from; never {@code null}
     */
    public String getReplayId() {
        return replayId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlaybackSegment)) {
            return false;
        }
        PlaybackSegment that = (PlaybackSegment) o;
        return Objects.equals(replayId, that.replayId)
                && sequenceIndex == that.sequenceIndex
                && startTick == that.startTick
                && endTick == that.endTick;
    }

    @Override
    public int hashCode() {
        return Objects.hash(replayId, sequenceIndex, startTick, endTick);
    }

    @Override
    public String toString() {
        return "PlaybackSegment{replayId=" + replayId
                + ", sequenceIndex=" + sequenceIndex + '}';
    }
}
