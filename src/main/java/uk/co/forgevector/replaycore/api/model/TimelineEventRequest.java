/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A request to add a custom marker to a replay's timeline.
 *
 * <p>Maps to {@code POST /v1/api/timeline-events}. A marker pins a named moment
 * (for example {@code "Final Death"}) at a given tick. Exactly one target must be
 * supplied:
 * <ul>
 *   <li>{@link Builder#replayId(String)} &mdash; pin the marker to an existing,
 *       already-finalised replay; or</li>
 *   <li>{@link Builder#serverId(String)} &mdash; mark the server's
 *       <em>currently active</em> recording (useful for live tagging from a
 *       gameplay plugin).</li>
 * </ul>
 *
 * <p>The builder validates field bounds locally (label 1&ndash;120 chars,
 * category &le; 60, actor &le; 80, colour as {@code #rrggbb}) to match the
 * server's own checks. Writing a marker requires an API key with the
 * {@link ApiScope#REPLAYS_WRITE} scope.
 */
public final class TimelineEventRequest {

    private static final Pattern HEX_COLOUR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final String replayId;
    private final String serverId;
    private final long tick;
    private final String label;
    private final String category;
    private final String colour;
    private final String actor;

    private TimelineEventRequest(Builder b) {
        this.replayId = b.replayId;
        this.serverId = b.serverId;
        this.tick = b.tick;
        this.label = b.label;
        this.category = b.category;
        this.colour = b.colour;
        this.actor = b.actor;
    }

    /**
     * Renders this request as the JSON body map the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map of body fields (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        if (replayId != null) {
            body.put("replayId", replayId);
        }
        if (serverId != null) {
            body.put("serverId", serverId);
        }
        body.put("tick", tick);
        body.put("label", label);
        if (category != null) {
            body.put("category", category);
        }
        if (colour != null) {
            body.put("colour", colour);
        }
        if (actor != null) {
            body.put("actor", actor);
        }
        return body;
    }

    /**
     * Returns a new builder for a marker against an existing replay.
     *
     * @param replayId the target replay UUID; must be non-blank
     * @return a builder seeded with the replay target
     * @throws IllegalArgumentException if blank
     */
    public static Builder forReplay(String replayId) {
        return new Builder().replayId(replayId);
    }

    /**
     * Returns a new builder for a marker against a server's active recording.
     *
     * @param serverId the target server UUID; must be non-blank
     * @return a builder seeded with the server target
     * @throws IllegalArgumentException if blank
     */
    public static Builder forActiveRecording(String serverId) {
        return new Builder().serverId(serverId);
    }

    /**
     * A fluent builder for {@link TimelineEventRequest}. Not thread-safe.
     */
    public static final class Builder {
        private String replayId;
        private String serverId;
        private long tick;
        private boolean tickSet;
        private String label;
        private String category;
        private String colour;
        private String actor;

        private Builder() {
        }

        /**
         * Targets an existing replay by id. Clears any server target.
         *
         * @param replayId the replay UUID; must be non-blank
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder replayId(String replayId) {
            this.replayId = requireNonBlank(replayId, "replayId");
            this.serverId = null;
            return this;
        }

        /**
         * Targets a server's active recording by server id. Clears any replay
         * target.
         *
         * @param serverId the server UUID; must be non-blank
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder serverId(String serverId) {
            this.serverId = requireNonBlank(serverId, "serverId");
            this.replayId = null;
            return this;
        }

        /**
         * Sets the timeline position, in ticks, at which to place the marker.
         *
         * @param tick a non-negative tick index
         * @return this builder
         * @throws IllegalArgumentException if negative
         */
        public Builder tick(long tick) {
            if (tick < 0) {
                throw new IllegalArgumentException("tick must be non-negative");
            }
            this.tick = tick;
            this.tickSet = true;
            return this;
        }

        /**
         * Sets the marker's label (its visible name).
         *
         * @param label between 1 and 120 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or longer than 120 characters
         */
        public Builder label(String label) {
            String trimmed = requireNonBlank(label, "label");
            if (trimmed.length() > 120) {
                throw new IllegalArgumentException("label must be at most 120 characters");
            }
            this.label = label;
            return this;
        }

        /**
         * Sets an optional category label for grouping markers.
         *
         * @param category at most 60 characters, or {@code null} to clear
         * @return this builder
         * @throws IllegalArgumentException if longer than 60 characters
         */
        public Builder category(String category) {
            if (category != null && category.length() > 60) {
                throw new IllegalArgumentException("category must be at most 60 characters");
            }
            this.category = category;
            return this;
        }

        /**
         * Sets an optional marker colour as a six-digit hex string.
         *
         * @param colour a {@code #rrggbb} colour, or {@code null} to clear
         * @return this builder
         * @throws IllegalArgumentException if not a valid {@code #rrggbb} value
         */
        public Builder colour(String colour) {
            if (colour != null && !HEX_COLOUR.matcher(colour).matches()) {
                throw new IllegalArgumentException("colour must be a #rrggbb hex value");
            }
            this.colour = colour;
            return this;
        }

        /**
         * Sets an optional actor label (who or what raised the marker).
         *
         * @param actor at most 80 characters, or {@code null} to clear
         * @return this builder
         * @throws IllegalArgumentException if longer than 80 characters
         */
        public Builder actor(String actor) {
            if (actor != null && actor.length() > 80) {
                throw new IllegalArgumentException("actor must be at most 80 characters");
            }
            this.actor = actor;
            return this;
        }

        /**
         * Validates that a target and the required fields are present and builds
         * the immutable request.
         *
         * @return a new immutable request
         * @throws IllegalArgumentException if neither or both targets are set, or
         *         if the tick or label was not supplied
         */
        public TimelineEventRequest build() {
            boolean hasReplay = replayId != null;
            boolean hasServer = serverId != null;
            if (hasReplay == hasServer) {
                throw new IllegalArgumentException("exactly one of replayId or serverId must be set");
            }
            if (!tickSet) {
                throw new IllegalArgumentException("tick must be set");
            }
            if (label == null) {
                throw new IllegalArgumentException("label must be set");
            }
            return new TimelineEventRequest(this);
        }

        private static String requireNonBlank(String value, String field) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value;
        }
    }
}
