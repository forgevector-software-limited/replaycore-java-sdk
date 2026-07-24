/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.UUID;

/**
 * One participant of a {@code ReplayAsset}: a player and how they relate to it.
 *
 * <p>An asset's FULL participant set is the list of these on a {@link ReplayCatalogEntry#participants()}.
 * Where {@link ReplayCatalogEntry#viewerRelation()} answers only "how does the QUERIED player relate to this
 * asset", this answers "who ELSE is on it, and how", so an integration can pin the exact killer
 * ({@link AssetRelationship#KILL}) and victim ({@link AssetRelationship#DEATH}) of a kill-clip even in a
 * multi-kill match, rather than only the one player it looked the asset up by.
 *
 * <p>A combat moment produces exactly one asset (RFC-0009 section 2.4); per-player meaning is carried by one
 * of these per player, never by a second copy of the asset. The same asset therefore lists a killer, a
 * victim and every bystander without ever storing the clip twice.
 *
 * <p>Immutable and thread-safe.
 */
public final class AssetParticipant {

    private final UUID playerUuid;
    private final AssetRelationship relation;

    /**
     * Creates a participant.
     *
     * @param playerUuid the participant's player id; must not be {@code null}
     * @param relation   how the player relates to the asset; must not be {@code null}
     */
    public AssetParticipant(UUID playerUuid, AssetRelationship relation) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid must not be null");
        this.relation = Objects.requireNonNull(relation, "relation must not be null");
    }

    /** @return the participant's player id; never {@code null} */
    public UUID playerUuid() { return playerUuid; }
    /** @return how the player relates to the asset; never {@code null} */
    public AssetRelationship relation() { return relation; }
}
