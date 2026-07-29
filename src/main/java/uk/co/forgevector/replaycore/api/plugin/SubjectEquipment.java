/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Complete immutable equipment snapshot for a player-shaped subject. */
public final class SubjectEquipment {
    private static final SubjectEquipment EMPTY = new SubjectEquipment(
            Collections.<ExternalSubjectEquipmentSlot, ExternalSubjectItem>emptyMap());

    private final Map<ExternalSubjectEquipmentSlot, ExternalSubjectItem> slots;

    public SubjectEquipment(Map<ExternalSubjectEquipmentSlot, ExternalSubjectItem> source) {
        EnumMap<ExternalSubjectEquipmentSlot, ExternalSubjectItem> copy =
                new EnumMap<ExternalSubjectEquipmentSlot, ExternalSubjectItem>(ExternalSubjectEquipmentSlot.class);
        if (source != null) {
            for (Map.Entry<ExternalSubjectEquipmentSlot, ExternalSubjectItem> entry : source.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) copy.put(entry.getKey(), entry.getValue());
            }
        }
        this.slots = Collections.unmodifiableMap(copy);
    }

    public static SubjectEquipment empty() { return EMPTY; }
    public Map<ExternalSubjectEquipmentSlot, ExternalSubjectItem> slots() { return slots; }
}
