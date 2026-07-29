/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/** Immediate, local outcome of a lightweight external-subject publication call. */
public enum ExternalSubjectPublishResult {
    ACCEPTED,
    DUPLICATE,
    BACKPRESSURE,
    CLOSED,
    WRONG_SCOPE,
    OUT_OF_ORDER,
    INVALID
}
