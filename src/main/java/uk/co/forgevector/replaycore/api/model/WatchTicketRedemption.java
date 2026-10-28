/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The playback a redeemed watch ticket grants.
 *
 * Immutable. Redeeming re-runs the full authorisation decision against fresh state, so a revoked
 * or newly held asset is refused even with a ticket that has not expired.
 */
public final class WatchTicketRedemption {

    private final long totalTicks;
    private final List<RedeemedSegment> segments;

    /**
     * Constructs a watch ticket redemption. Intended for internal deserialisation and tests.
     *
     * @param totalTicks the length of the whole playback, in ticks
     * @param segments the segments to play, in order
     */
    public WatchTicketRedemption(long totalTicks, List<RedeemedSegment> segments) {
        this.totalTicks = totalTicks;
        this.segments = segments != null
                ? Collections.unmodifiableList(new ArrayList<RedeemedSegment>(segments))
                : Collections.<RedeemedSegment>emptyList();
    }

    /**
     * Returns the length of the whole playback, in ticks.
     *
     * @return the length of the whole playback, in ticks
     */
    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Returns the segments to play, in order.
     *
     * @return the segments to play, in order; never {@code null}
     */
    public List<RedeemedSegment> getSegments() {
        return segments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WatchTicketRedemption)) {
            return false;
        }
        WatchTicketRedemption that = (WatchTicketRedemption) o;
        return totalTicks == that.totalTicks
                && Objects.equals(segments, that.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalTicks, segments);
    }

    @Override
    public String toString() {
        return "WatchTicketRedemption{totalTicks=" + totalTicks + '}';
    }
}
