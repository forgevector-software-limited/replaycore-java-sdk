/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One team in a replay collection.
 *
 * Immutable.
 */
public final class CollectionTeam {

    private final String id;
    private final String collectionId;
    private final String key;
    private final String name;
    private final String result;
    private final Integer placement;
    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Constructs a collection team. Intended for internal deserialisation and tests.
     *
     * @param id the team id
     * @param collectionId the id of the collection the team belongs to
     * @param key the integration's own key for the team
     * @param name the team's display name
     * @param result the team's result label
     * @param placement the team's final placement, starting at 1
     * @param createdAt when the team was created
     * @param updatedAt when the team was last updated
     */
    public CollectionTeam(
            String id,
            String collectionId,
            String key,
            String name,
            String result,
            Integer placement,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id != null ? id : "";
        this.collectionId = collectionId != null ? collectionId : "";
        this.key = key != null ? key : "";
        this.name = name;
        this.result = result;
        this.placement = placement;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the team id.
     *
     * @return the team id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the collection the team belongs to.
     *
     * @return the id of the collection the team belongs to; never {@code null}
     */
    public String getCollectionId() {
        return collectionId;
    }

    /**
     * Returns the integration's own key for the team.
     *
     * @return the integration's own key for the team; never {@code null}
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the team's display name.
     *
     * @return the team's display name, or an empty optional
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the team's result label.
     *
     * @return the team's result label, or an empty optional
     */
    public Optional<String> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns the team's final placement, starting at 1.
     *
     * @return the team's final placement, starting at 1, or an empty optional
     */
    public Optional<Integer> getPlacement() {
        return Optional.ofNullable(placement);
    }

    /**
     * Returns when the team was created.
     *
     * @return when the team was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns when the team was last updated.
     *
     * @return when the team was last updated, or an empty optional
     */
    public Optional<Instant> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CollectionTeam)) {
            return false;
        }
        CollectionTeam that = (CollectionTeam) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CollectionTeam{id=" + id
                + ", key=" + key
                + ", name=" + name + '}';
    }
}
