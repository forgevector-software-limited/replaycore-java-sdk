/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/** Captured player-shaped pose, independent of Bukkit and protocol internals. */
public enum SubjectPose {
    STANDING,
    CROUCHING,
    SLEEPING,
    SWIMMING,
    FALL_FLYING,
    SPIN_ATTACK,
    DYING,
    RIDING
}
