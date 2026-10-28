/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One player in a replay collection's roster.
 *
 * Immutable. Ticks are positions on the collection's own timeline, not wall-clock times.
 */
public final class CollectionParticipant {

    private final String id;
    private final String collectionId;
    private final String playerUuid;
    private final String playerName;
    private final String teamKey;
    private final String role;
    private final long joinedTick;
    private final Long leftTick;
    private final String result;
    private final Instant createdAt;
    private final Instant updatedAt;

    private CollectionParticipant(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.collectionId = b.collectionId != null ? b.collectionId : "";
        this.playerUuid = b.playerUuid != null ? b.playerUuid : "";
        this.playerName = b.playerName != null ? b.playerName : "";
        this.teamKey = b.teamKey;
        this.role = b.role != null ? b.role : "";
        this.joinedTick = b.joinedTick;
        this.leftTick = b.leftTick;
        this.result = b.result;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    /**
     * Returns the roster entry id.
     *
     * @return the roster entry id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the collection the entry belongs to.
     *
     * @return the id of the collection the entry belongs to; never {@code null}
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the player's UUID.
     *
     * @return the player's UUID; never {@code null}
     */
    public String getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Returns the player's last known name.
     *
     * @return the player's last known name; never {@code null}
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Returns the key of the team the player is on.
     *
     * @return the key of the team the player is on, or an empty optional
     */
    public Optional<String> getTeamKey() {
        return Optional.ofNullable(teamKey);
    }

    /**
     * Returns the player's role (for example {@code "participant"} or {@code "spectator"}).
     *
     * @return the player's role (for example {@code "participant"} or {@code "spectator"}); never
     *         {@code null}
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the tick at which the player joined.
     *
     * @return the tick at which the player joined
     */
    public long getJoinedTick() {
        return joinedTick;
    }

    /**
     * Returns the tick at which the player left; absent while still present.
     *
     * @return the tick at which the player left; absent while still present, or an empty optional
     */
    public Optional<Long> getLeftTick() {
        return Optional.ofNullable(leftTick);
    }

    /**
     * Returns the player's result label.
     *
     * @return the player's result label, or an empty optional
     */
    public Optional<String> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns when the entry was created.
     *
     * @return when the entry was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns when the entry was last updated.
     *
     * @return when the entry was last updated, or an empty optional
     */
    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CollectionParticipant)) {
            return false;
        }
        return id.equals(((CollectionParticipant) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CollectionParticipant{id=" + id
                + ", playerUuid=" + playerUuid
                + ", role=" + role + '}';
    }

    /**
     * Returns a new builder for assembling a {@link CollectionParticipant}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @param id the roster entry id; never {@code null}
     * @return a fresh builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link CollectionParticipant}. Not thread-safe. Every setter returns
     * {@code this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private final String id;
        private String collectionId;
        private String playerUuid;
        private String playerName;
        private String teamKey;
        private String role;
        private long joinedTick;
        private Long leftTick;
        private String result;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the id of the collection the entry belongs to.
         *
         * @param v the id of the collection the entry belongs to
         * @return this builder
         */
        public Builder collectionId(String v) { this.collectionId = v; return this; }

        /**
         * Sets the player's UUID.
         *
         * @param v the player's UUID
         * @return this builder
         */
        public Builder playerUuid(String v) { this.playerUuid = v; return this; }

        /**
         * Sets the player's last known name.
         *
         * @param v the player's last known name
         * @return this builder
         */
        public Builder playerName(String v) { this.playerName = v; return this; }

        /**
         * Sets the key of the team the player is on.
         *
         * @param v the key of the team the player is on
         * @return this builder
         */
        public Builder teamKey(String v) { this.teamKey = v; return this; }

        /**
         * Sets the player's role (for example {@code "participant"} or {@code "spectator"}).
         *
         * @param v the player's role (for example {@code "participant"} or {@code "spectator"})
         * @return this builder
         */
        public Builder role(String v) { this.role = v; return this; }

        /**
         * Sets the tick at which the player joined.
         *
         * @param v the tick at which the player joined
         * @return this builder
         */
        public Builder joinedTick(long v) { this.joinedTick = v; return this; }

        /**
         * Sets the tick at which the player left; absent while still present.
         *
         * @param v the tick at which the player left; absent while still present
         * @return this builder
         */
        public Builder leftTick(Long v) { this.leftTick = v; return this; }

        /**
         * Sets the player's result label.
         *
         * @param v the player's result label
         * @return this builder
         */
        public Builder result(String v) { this.result = v; return this; }

        /**
         * Sets when the entry was created.
         *
         * @param v when the entry was created
         * @return this builder
         */
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        /**
         * Sets when the entry was last updated.
         *
         * @param v when the entry was last updated
         * @return this builder
         */
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        /**
         * Builds the immutable {@link CollectionParticipant}.
         *
         * @return a new immutable instance
         */
        public CollectionParticipant build() {
            return new CollectionParticipant(this);
        }
    }
}
