/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * How a specific player relates to a specific {@code ReplayAsset}.
 *
 * <p>A combat moment produces exactly one asset; per-player meaning is expressed by a row per player
 * carrying one of these values, never by minting a second copy of the asset. The same asset is therefore
 * reachable from a killer's kill feed, a victim's death feed and the match timeline without ever storing a
 * second copy. Creating one asset per viewer instead of one relationship row per viewer is forbidden: it
 * doubles storage, doubles processing, and makes retention and revocation inconsistent between two rows
 * that represent the same real event.
 *
 * <p><strong>Reachability.</strong> This type is an ordinary in-process argument. Declare a participant's
 * part in a clip with {@link ScopeClipRequest.Builder#relationship(java.util.UUID, AssetRelationship)},
 * and read the declared set back with {@link ScopeClipRequest#relationships()}. The same names are the
 * values of the {@code relation} field on the REST network integration API's asset rows, so a clip
 * recorded in process and one created over REST describe a participant identically.
 */
public enum AssetRelationship {
    /** The player who caused the moment (for example the killer in a kill clip). */
    KILL,
    /** The player the moment happened to (for example the victim in a kill clip). */
    DEATH,
    /** Any other player present in the moment without a more specific relationship. */
    PARTICIPANT
}
