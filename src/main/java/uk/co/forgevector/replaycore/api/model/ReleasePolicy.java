/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The release policy a replay collection is opened under: when its full match
 * and its clips leave embargo, and who may watch them afterwards.
 *
 * <p>A collection stores the resolved policy it was opened with and always
 * evaluates that snapshot, so changing an integration's configuration later never
 * releases footage recorded under stricter rules. Send it with
 * {@link CollectionRequest.Builder#build()}.
 *
 * <p>{@link ReleaseTrigger#UNKNOWN} is not a valid trigger to send.
 */
public final class ReleasePolicy {

    private final ReleaseTrigger fullReplay;
    private final ReleaseTrigger eventClips;
    private final CollectionVisibility visibility;
    private final boolean staffBypass;

    private ReleasePolicy(Builder b) {
        this.fullReplay = b.fullReplay;
        this.eventClips = b.eventClips;
        this.visibility = b.visibility;
        this.staffBypass = b.staffBypass;
    }

    /**
     * Returns when the collection's full match is released.
     *
     * @return the full-match release trigger
     */
    public ReleaseTrigger getFullReplay() {
        return fullReplay;
    }

    /**
     * Returns when the collection's clips and highlights are released.
     *
     * @return the clip release trigger
     */
    public ReleaseTrigger getEventClips() {
        return eventClips;
    }

    /**
     * Returns the visibility the collection and its assets take once released.
     *
     * @return the visibility
     */
    public CollectionVisibility getVisibility() {
        return visibility;
    }

    /**
     * Returns whether a key holding the staff bypass scope may view held content
     * under this policy.
     *
     * @return {@code true} if staff may view held content
     */
    public boolean isStaffBypass() {
        return staffBypass;
    }

    /**
     * Renders this policy as the JSON object the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("full-replay", rule(fullReplay));
        body.put("event-clips", rule(eventClips));
        body.put("visibility", visibility.wireValue());
        if (staffBypass) {
            body.put("staff-bypass", Boolean.TRUE);
        }
        return body;
    }

    private static Map<String, Object> rule(ReleaseTrigger trigger) {
        Map<String, Object> rule = new LinkedHashMap<String, Object>();
        rule.put("release-trigger", trigger.wireValue());
        return rule;
    }

    /**
     * Returns a new builder for a release policy.
     *
     * @param fullReplay what releases the full match
     * @param eventClips what releases clips and highlights
     * @param visibility who may watch released content
     * @return a builder seeded with the three required settings
     * @throws IllegalArgumentException if any argument is {@code null} or unknown
     */
    public static Builder builder(ReleaseTrigger fullReplay, ReleaseTrigger eventClips,
                                  CollectionVisibility visibility) {
        return new Builder(fullReplay, eventClips, visibility);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReleasePolicy)) {
            return false;
        }
        ReleasePolicy that = (ReleasePolicy) o;
        return fullReplay == that.fullReplay
                && eventClips == that.eventClips
                && visibility == that.visibility
                && staffBypass == that.staffBypass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullReplay, eventClips, visibility, staffBypass);
    }

    @Override
    public String toString() {
        return "ReleasePolicy{fullReplay=" + fullReplay + ", eventClips=" + eventClips
                + ", visibility=" + visibility + ", staffBypass=" + staffBypass + '}';
    }

    /**
     * A fluent builder for {@link ReleasePolicy}. Not thread-safe.
     */
    public static final class Builder {
        private final ReleaseTrigger fullReplay;
        private final ReleaseTrigger eventClips;
        private final CollectionVisibility visibility;
        private boolean staffBypass;

        private Builder(ReleaseTrigger fullReplay, ReleaseTrigger eventClips, CollectionVisibility visibility) {
            if (fullReplay == null || fullReplay == ReleaseTrigger.UNKNOWN) {
                throw new IllegalArgumentException("fullReplay must be a known release trigger");
            }
            if (eventClips == null || eventClips == ReleaseTrigger.UNKNOWN) {
                throw new IllegalArgumentException("eventClips must be a known release trigger");
            }
            if (visibility == null || visibility == CollectionVisibility.UNKNOWN) {
                throw new IllegalArgumentException("visibility must be a known visibility");
            }
            this.fullReplay = fullReplay;
            this.eventClips = eventClips;
            this.visibility = visibility;
        }

        /**
         * Lets a key holding the staff bypass scope view held content.
         *
         * @param staffBypass {@code true} to permit staff viewing
         * @return this builder
         */
        public Builder staffBypass(boolean staffBypass) {
            this.staffBypass = staffBypass;
            return this;
        }

        /**
         * Builds the immutable policy.
         *
         * @return a new immutable policy
         */
        public ReleasePolicy build() {
            return new ReleasePolicy(this);
        }
    }
}
