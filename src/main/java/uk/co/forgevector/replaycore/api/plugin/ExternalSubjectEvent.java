/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;

/** One authoritative visual event for an external player-shaped subject. */
public final class ExternalSubjectEvent {
    private final long replayTick;
    private final ExternalSubjectEventType type;
    private final ExternalSubjectEventPayload payload;

    public ExternalSubjectEvent(long replayTick, ExternalSubjectEventType type,
                                ExternalSubjectEventPayload payload) {
        if (replayTick < 0L) throw new IllegalArgumentException("replayTick must not be negative");
        this.replayTick = replayTick;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.payload = payload == null ? ExternalSubjectEventPayload.empty() : payload;
        validatePayload();
    }

    public static ExternalSubjectEvent of(long replayTick, ExternalSubjectEventType type) {
        return new ExternalSubjectEvent(replayTick, type, ExternalSubjectEventPayload.empty());
    }

    public long replayTick() { return replayTick; }
    public ExternalSubjectEventType type() { return type; }
    public ExternalSubjectEventPayload payload() { return payload; }

    private void validatePayload() {
        switch (type) {
            case HELD_ITEM_CHANGE:
                require(payload.heldSlot().isPresent(), "HELD_ITEM_CHANGE requires heldSlot");
                break;
            case EQUIPMENT_CHANGE:
                require(payload.equipment().isPresent(), "EQUIPMENT_CHANGE requires equipment");
                break;
            case KNOCKBACK:
                require(payload.vectorPresent(), "KNOCKBACK requires a vector");
                break;
            case EFFECT_ADD:
            case EFFECT_REMOVE:
                require(payload.effect().isPresent(), type + " requires effect");
                break;
            case SPAWN:
            case RESPAWN:
            case TELEPORT:
                require(payload.frame().isPresent(), type + " requires frame");
                require(payload.frame().get().replayTick() == replayTick,
                        type + " frame must have the same replayTick");
                break;
            default:
                break;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
