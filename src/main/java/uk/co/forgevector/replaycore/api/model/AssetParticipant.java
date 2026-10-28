/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * A player and their relation to a replay asset, as listed on a player's replays.
 *
 * Immutable. It names the victim and killer of a kill clip, not only how the queried player
 * relates to it.
 */
public final class AssetParticipant {

    private final String playerUuid;
    private final AssetRelation relation;

    /**
     * Constructs a asset participant. Intended for internal deserialisation and tests.
     *
     * @param playerUuid the player's UUID
     * @param relation how the player relates to the asset
     */
    public AssetParticipant(String playerUuid, AssetRelation relation) {
        this.playerUuid = playerUuid != null ? playerUuid : "";
        this.relation = relation != null ? relation : AssetRelation.UNKNOWN;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssetParticipant)) {
            return false;
        }
        AssetParticipant that = (AssetParticipant) o;
        return Objects.equals(playerUuid, that.playerUuid)
                && Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerUuid, relation);
    }

    @Override
    public String toString() {
        return "AssetParticipant{playerUuid=" + playerUuid
                + ", relation=" + relation + '}';
    }
}
