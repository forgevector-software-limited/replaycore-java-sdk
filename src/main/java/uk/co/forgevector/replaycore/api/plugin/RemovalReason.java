/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/** Why an external subject handle was permanently unregistered. */
public enum RemovalReason {
    EXPLICIT,
    REPLACED,
    SCOPE_ENDED,
    RECORDING_STOPPED,
    INTEGRATION_SHUTDOWN,
    ERROR
}
