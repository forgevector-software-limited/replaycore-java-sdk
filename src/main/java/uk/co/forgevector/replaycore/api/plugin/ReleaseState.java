/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * Whether a {@code ReplayCollection} or {@code ReplayAsset} has been made reachable by an operator, as one
 * of three orthogonal states alongside {@link ProcessingState} and {@link ReplayVisibility}.
 *
 * <p>A collection or asset can be fully {@link ProcessingState#READY} while remaining {@link #HELD}
 * indefinitely; nothing about processing completion implies release. The transition into {@link #RELEASED}
 * is driven either by the release policy resolved at scope start (see the recorder's {@code policies}
 * configuration) or by an explicit hold/release/revoke call, never by processing completion alone.
 *
 * <p>Hold, release and revoke are idempotent: releasing an already-released collection, or revoking an
 * already-revoked one, is a no-op rather than an error.
 */
public enum ReleaseState {
    /** Not reachable through any listing, direct, or watch endpoint, regardless of visibility. */
    HELD,
    /** Reachable subject to {@link ReplayVisibility} and normal access checks. */
    RELEASED,
    /** Permanently withdrawn after having been released; does not return to {@link #HELD}. */
    REVOKED
}
