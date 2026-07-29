/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/** Durable state of an externally identified recording. */
public enum ReplayState {
    RECORDING,
    FINALIZING,
    LINK_READY,
    PLAYABLE,
    FAILED,
    EXPIRED,
    DELETED;

    /** Compatibility mapping from the older catalogue processing vocabulary. */
    public static ReplayState fromProcessingState(ProcessingState value) {
        if (value == null) throw new IllegalArgumentException("value must not be null");
        switch (value) {
            case RECORDING: return RECORDING;
            case READY: return PLAYABLE;
            case FAILED: return FAILED;
            case EXPIRED: return EXPIRED;
            case DELETED: return DELETED;
            default: return FINALIZING;
        }
    }
}
