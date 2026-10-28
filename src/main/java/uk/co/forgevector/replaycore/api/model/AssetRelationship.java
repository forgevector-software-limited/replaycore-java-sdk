/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A player's relation to a replay asset, as returned when a single asset is read.
 *
 * Immutable.
 */
public final class AssetRelationship {

    private final String id;
    private final String assetId;
    private final String playerUuid;
    private final AssetRelation relation;
    private final Instant createdAt;

    /**
     * Constructs a asset relationship. Intended for internal deserialisation and tests.
     *
     * @param id the relationship id
     * @param assetId the id of the asset
     * @param playerUuid the player's UUID
     * @param relation how the player relates to the asset
     * @param createdAt when the relationship was created
     */
    public AssetRelationship(
            String id,
            String assetId,
            String playerUuid,
            AssetRelation relation,
            Instant createdAt) {
        this.id = id != null ? id : "";
        this.assetId = assetId != null ? assetId : "";
        this.playerUuid = playerUuid != null ? playerUuid : "";
        this.relation = relation != null ? relation : AssetRelation.UNKNOWN;
        this.createdAt = createdAt;
    }

    /**
     * Returns the relationship id.
     *
     * @return the relationship id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the id of the asset.
     *
     * @return the id of the asset; never {@code null}
     */
    public String getAssetId() {
        return assetId;
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
     * Returns how the player relates to the asset.
     *
     * @return how the player relates to the asset
     */
    public AssetRelation getRelation() {
        return relation;
    }

    /**
     * Returns when the relationship was created.
     *
     * @return when the relationship was created, or an empty optional
     */
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssetRelationship)) {
            return false;
        }
        AssetRelationship that = (AssetRelationship) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AssetRelationship{id=" + id
                + ", playerUuid=" + playerUuid
                + ", relation=" + relation + '}';
    }
}
