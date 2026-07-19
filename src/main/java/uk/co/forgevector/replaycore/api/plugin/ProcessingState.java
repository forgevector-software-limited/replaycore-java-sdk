/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * The processing lifecycle of a {@code ReplayCollection} or {@code ReplayAsset}: how far the footage has
 * progressed from live capture towards playable bytes.
 *
 * <p>This is one of three orthogonal states a collection or asset carries; it must never be collapsed with
 * {@link ReleaseState} (whether an operator has chosen to make ready footage reachable) or
 * {@link ReplayVisibility} (who footage is reachable to once released). Fully processed and completely
 * unreleased is the expected, central case for embargoed content: {@link #READY} alongside
 * {@link ReleaseState#HELD}.
 *
 * <p>{@link #FAILED}, {@link #EXPIRED} and {@link #DELETED} are terminal: no further transition is
 * expected once one of these is reached.
 */
public enum ProcessingState {
    /** Capture is live; the collection's segments are still being written. */
    RECORDING,
    /** Capture has stopped and the recorder is sealing the collection's final segment. */
    FINALIZING,
    /** Sealed and waiting for a worker to pick it up. */
    QUEUED,
    /** The sealed archive is being transferred to cloud storage. */
    UPLOADING,
    /** Uploaded and undergoing server-side processing (indexing, thumbnailing, derivative asset work). */
    PROCESSING,
    /** Fully uploaded and prepared; playable once release and visibility also permit it. */
    READY,
    /** Processing could not complete; see the operation's failure code for the reason. */
    FAILED,
    /** Retention removed the underlying archive before it could be watched. */
    EXPIRED,
    /** Removed by an operator or an automated deletion request. */
    DELETED
}
