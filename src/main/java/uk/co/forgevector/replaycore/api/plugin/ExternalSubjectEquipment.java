/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.Optional;

/** One authoritative equipment-slot change. An absent item clears the slot. */
public final class ExternalSubjectEquipment {
    private final ExternalSubjectEquipmentSlot slot;
    private final ExternalSubjectItem item;

    private ExternalSubjectEquipment(ExternalSubjectEquipmentSlot slot, ExternalSubjectItem item) {
        this.slot = Objects.requireNonNull(slot, "slot must not be null");
        this.item = item;
    }

    public static ExternalSubjectEquipment set(ExternalSubjectEquipmentSlot slot, ExternalSubjectItem item) {
        return new ExternalSubjectEquipment(slot, Objects.requireNonNull(item, "item must not be null"));
    }

    public static ExternalSubjectEquipment clear(ExternalSubjectEquipmentSlot slot) {
        return new ExternalSubjectEquipment(slot, null);
    }

    public ExternalSubjectEquipmentSlot slot() { return slot; }
    public Optional<ExternalSubjectItem> item() { return Optional.ofNullable(item); }
}
