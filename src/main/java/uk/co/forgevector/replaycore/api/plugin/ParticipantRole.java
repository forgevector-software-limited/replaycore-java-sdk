/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * How a player named in a {@link ReplayParticipant} took part in a collection.
 *
 * <p>{@link #CUSTOM} covers any role a network needs beyond the five named here, so a network's own staff
 * or audience taxonomy never needs a new enum constant here; describe the specific meaning through
 * namespaced metadata instead. ReplayCore stores and filters on the role; it runs no permission or
 * competition logic that depends on which value is set.
 */
public enum ParticipantRole {
    /** An ordinary participant in the collection (a player, by default). */
    PARTICIPANT,
    /** Present without taking part (for example watching from a spectator platform). */
    SPECTATOR,
    /** Present for oversight rather than as a competitor (for example a referee or moderator). */
    STAFF_OBSERVER,
    /** Present in a coaching capacity for a team or participant. */
    COACH,
    /** Present to commentate or produce broadcast coverage rather than to compete. */
    BROADCASTER,
    /** A role outside the five named above; the specific meaning belongs in namespaced metadata. */
    CUSTOM
}
