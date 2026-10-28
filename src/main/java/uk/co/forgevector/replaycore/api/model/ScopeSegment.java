/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * The part of one recording archive that a replay collection covers.
 *
 * <p>A match can span several archives when the recorder rotates a recording, so
 * a collection names each archive it draws on. Segments are ordered by their
 * sequence index, which is sent explicitly so that a resend can never reorder a
 * segment that is already stored. Ticks are local to the archive.
 *
 * <p>Send the still-open first segment, with no end tick, when the collection is
 * created. Send the full closed list when it is finalised. Both calls are safe to
 * repeat.
 */
public final class ScopeSegment {

    private final String archiveId;
    private final int sequenceIndex;
    private final long startTick;
    private final Long endTick;
    private final Long segmentFirstTick;

    private ScopeSegment(Builder b) {
        this.archiveId = b.archiveId;
        this.sequenceIndex = b.sequenceIndex;
        this.startTick = b.startTick;
        this.endTick = b.endTick;
        this.segmentFirstTick = b.segmentFirstTick;
    }

    /**
     * Renders this segment as the JSON object the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("archive_id", archiveId);
        body.put("sequence_index", sequenceIndex);
        body.put("start_tick", startTick);
        if (endTick != null) {
            body.put("end_tick", endTick);
        }
        if (segmentFirstTick != null) {
            body.put("segment_first_tick", segmentFirstTick);
        }
        return body;
    }

    /**
     * Returns a new builder for a segment.
     *
     * @param archiveId     the UUID of the archive the segment is recorded into
     * @param sequenceIndex the 0-based position of the segment in the collection
     * @param startTick     the first tick the collection covers, in ticks of the archive
     * @return a builder seeded with the required fields
     * @throws IllegalArgumentException if the UUID is invalid or a number is negative
     */
    public static Builder builder(String archiveId, int sequenceIndex, long startTick) {
        return new Builder(archiveId, sequenceIndex, startTick);
    }

    /**
     * A fluent builder for {@link ScopeSegment}. Not thread-safe.
     */
    public static final class Builder {
        private final String archiveId;
        private final int sequenceIndex;
        private final long startTick;
        private Long endTick;
        private Long segmentFirstTick;

        private Builder(String archiveId, int sequenceIndex, long startTick) {
            this.archiveId = Validation.requireUuid(archiveId, "archiveId");
            if (sequenceIndex < 0) {
                throw new IllegalArgumentException("sequenceIndex must not be negative");
            }
            this.sequenceIndex = sequenceIndex;
            this.startTick = Validation.requireTick(startTick, "startTick");
        }

        /**
         * Closes the segment. Leave it unset while the segment is still being recorded.
         *
         * @param endTick the last tick the collection covers, not before the start tick
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder endTick(long endTick) {
            this.endTick = Validation.requireTick(endTick, "endTick");
            return this;
        }

        /**
         * Sets the cumulative session tick at which the archive begins.
         *
         * @param segmentFirstTick a non-negative tick
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder segmentFirstTick(long segmentFirstTick) {
            this.segmentFirstTick = Validation.requireTick(segmentFirstTick, "segmentFirstTick");
            return this;
        }

        /**
         * Builds the immutable segment.
         *
         * @return a new immutable segment
         * @throws IllegalArgumentException if the end tick precedes the start tick
         */
        public ScopeSegment build() {
            if (endTick != null && endTick < startTick) {
                throw new IllegalArgumentException("endTick must not precede startTick");
            }
            return new ScopeSegment(this);
        }
    }
}
