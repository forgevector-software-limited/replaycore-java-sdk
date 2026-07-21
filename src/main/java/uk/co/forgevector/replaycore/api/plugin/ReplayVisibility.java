/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * Who a released {@code ReplayCollection} or {@code ReplayAsset} is reachable to, as one of three
 * orthogonal states alongside {@link ProcessingState} and {@link ReleaseState}.
 *
 * <p>Visibility only matters once {@link ReleaseState#RELEASED} is reached; {@link ReleaseState#HELD}
 * content is unreachable regardless of the value here. This is a distinct enum from the physical
 * {@code replays} table's visibility column: the two have different value sets and different tables back
 * them, and this one must never be conflated with or substituted for the other.
 */
public enum ReplayVisibility {
    /** Reachable by anyone, including an unauthenticated visitor to a public catalogue or share link. */
    PUBLIC,
    /** Reachable only by a caller holding the direct id or share link; excluded from public listings. */
    UNLISTED,
    /** Reachable only by an authenticated participant of the collection (see {@link ParticipantRole}). */
    PARTICIPANTS,
    /** Reachable only by a caller with a verified staff authorisation scope. */
    STAFF_ONLY,
    /** Reachable only by the owning tenant's own authenticated callers. */
    PRIVATE
}
