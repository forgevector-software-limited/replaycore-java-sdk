/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A custom timeline marker, as returned by the cloud after a successful
 * {@link TimelineEventRequest}.
 *
 * <p>Immutable. The cloud resolves the request's target (a replay or a server's
 * active recording) to a concrete replay and assigns the marker an id and a
 * creation timestamp.
 */
public final class TimelineMarker {

    private final String id;
    private final String replayId;
    private final long tick;
    private final String label;
    private final String category;
    private final String colour;
    private final String actor;
    private final Instant createdAt;

    /**
     * Constructs a marker. Intended for internal deserialisation and tests.
     *
     * @param id        the marker id; never {@code null}
     * @param replayId  the resolved replay id; never {@code null}
     * @param tick      the timeline position in ticks
     * @param label     the marker label; never {@code null}
     * @param category  an optional category, or {@code null}
     * @param colour    an optional {@code #rrggbb} colour, or {@code null}
     * @param actor     an optional actor label, or {@code null}
     * @param createdAt the creation instant, or {@code null} if the server omitted it
     */
    public TimelineMarker(String id, String replayId, long tick, String label,
                          String category, String colour, String actor, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.tick = tick;
        this.label = Objects.requireNonNull(label, "label");
        this.category = category;
        this.colour = colour;
        this.actor = actor;
        this.createdAt = createdAt;
    }

    /**
     * Returns the marker's unique id.
     *
     * @return the marker id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the replay the marker is attached to. When the request
     * targeted a server's active recording, this is the replay that recording
     * resolved to.
     *
     * @return the replay id; never {@code null}
     */
    public String getReplayId() {
        return replayId;
    }

    /**
     * Returns the timeline position, in ticks.
     *
     * @return the tick index
     */
    public long getTick() {
        return tick;
    }

    /**
     * Returns the marker's visible label.
     *
     * @return the label; never {@code null}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the marker's category, if one was supplied.
     *
     * @return the category, or an empty optional
     */
    public Optional<String> getCategory() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the marker's colour as a {@code #rrggbb} string, if one was supplied.
     *
     * @return the colour, or an empty optional
     */
    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

    /**
     * Returns the marker's actor label, if one was supplied.
     *
     * @return the actor, or an empty optional
     */
    public Optional<String> getActor() {
        return Optional.ofNullable(actor);
    }

    /**
     * Returns when the marker was created.
     *
     * @return the creation instant, or an empty optional if the server omitted it
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimelineMarker)) {
            return false;
        }
        return id.equals(((TimelineMarker) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "TimelineMarker{id=" + id + ", replayId=" + replayId
                + ", tick=" + tick + ", label=" + label + '}';
    }
}
