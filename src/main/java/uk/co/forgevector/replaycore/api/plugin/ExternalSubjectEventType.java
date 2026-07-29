/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * Frozen visual and lifecycle events supported for a player-shaped external subject.
 */
public enum ExternalSubjectEventType {
    ARM_SWING,
    ITEM_USE_START,
    ITEM_USE_STOP,
    BOW_DRAW_START,
    BOW_RELEASE,
    HELD_ITEM_CHANGE,
    EQUIPMENT_CHANGE,
    HURT_ANIMATION,
    KNOCKBACK,
    DEATH,
    RESPAWN,
    TELEPORT,
    SPAWN,
    DESPAWN,
    EFFECT_ADD,
    EFFECT_REMOVE
}
