/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * An in-process timeline annotation an addon asks the recorder to place on the active recording.
 *
 * <p>This is the local counterpart to the REST
 * {@link uk.co.forgevector.replaycore.api.model.TimelineEventRequest}: the same idea (mark a named
 * moment), but emitted from within the game tick instead of over HTTP, so it pins to whatever tick the
 * recording is on right now. The recorder resolves the concrete replay; the addon never names one.
 *
 * <p>Immutable; build one with {@link #builder(String)}. Field bounds match the cloud's timeline-event
 * validation (label 1 to 120 characters, category at most 60, actor at most 80, colour {@code #rrggbb}).
 *
 * @deprecated Use the canonical {@link IntegrationBookmark} with
 *             {@link ReplayCoreTimelineApi#tagTimelineEvent(IntegrationBookmark)} instead. That type is
 *             the one the recorder writes to the replay timeline, so a marker raised through it renders
 *             identically to the built-in events; this thin type is retained only for source
 *             compatibility and will be removed in a future major version.
 */
@Deprecated
public final class Bookmark {

    private static final Pattern HEX_COLOUR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    private final String label;
    private final String category;
    private final String colour;
    private final String actor;

    private Bookmark(Builder b) {
        this.label = b.label;
        this.category = b.category;
        this.colour = b.colour;
        this.actor = b.actor;
    }

    /**
     * Returns the marker's visible label.
     *
     * @return the label; never {@code null}
     */
    public String label() {
        return label;
    }

    /**
     * Returns the marker's grouping category, if set.
     *
     * @return the category, or an empty optional
     */
    public Optional<String> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the marker's {@code #rrggbb} colour, if set.
     *
     * @return the colour, or an empty optional
     */
    public Optional<String> colour() {
        return Optional.ofNullable(colour);
    }

    /**
     * Returns the marker's actor label (who or what raised the bookmark), if set.
     *
     * @return the actor, or an empty optional
     */
    public Optional<String> actor() {
        return Optional.ofNullable(actor);
    }

    /**
     * Starts building a bookmark with the given label.
     *
     * @param label the visible label, 1 to 120 characters
     * @return a new builder
     * @throws IllegalArgumentException if the label is blank or too long
     */
    public static Builder builder(String label) {
        return new Builder(label);
    }

    @Override
    public String toString() {
        return "Bookmark{label=" + label + ", category=" + category
                + ", colour=" + colour + ", actor=" + actor + '}';
    }

    /** A fluent builder for {@link Bookmark}. Not thread-safe. */
    public static final class Builder {
        private final String label;
        private String category;
        private String colour;
        private String actor;

        private Builder(String label) {
            if (label == null || label.trim().isEmpty()) {
                throw new IllegalArgumentException("label must not be blank");
            }
            if (label.length() > 120) {
                throw new IllegalArgumentException("label must be at most 120 characters");
            }
            this.label = label;
        }

        /**
         * Sets an optional category for grouping bookmarks.
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
         * Sets an optional {@code #rrggbb} colour.
         *
         * @param colour a six-digit hex colour, or {@code null} to clear
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
         * Sets an optional actor label.
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
         * Builds the immutable bookmark.
         *
         * @return a new {@link Bookmark}
         */
        public Bookmark build() {
            return new Bookmark(this);
        }
    }
}
