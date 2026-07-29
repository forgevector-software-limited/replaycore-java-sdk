/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Complete authoritative state for one external player-shaped subject at one replay tick. */
public final class ExternalSubjectFrame {
    private final long replayTick;
    private final SubjectTransform transform;
    private final SubjectMetadata metadata;
    private final SubjectEquipment equipment;
    private final SubjectEffects effects;

    public ExternalSubjectFrame(long replayTick, SubjectTransform transform, SubjectMetadata metadata,
                                SubjectEquipment equipment, SubjectEffects effects) {
        if (replayTick < 0L) throw new IllegalArgumentException("replayTick must not be negative");
        this.replayTick = replayTick;
        this.transform = Objects.requireNonNull(transform, "transform must not be null");
        this.metadata = metadata;
        this.equipment = equipment;
        this.effects = effects;
    }

    public static Builder builder(long replayTick, SubjectTransform transform) {
        return new Builder(replayTick, transform);
    }

    public long replayTick() { return replayTick; }
    public UUID worldId() { return transform.worldId(); }
    public SubjectTransform transform() { return transform; }
    public double x() { return transform.x(); }
    public double y() { return transform.y(); }
    public double z() { return transform.z(); }
    public float yaw() { return transform.yaw(); }
    public float pitch() { return transform.pitch(); }
    public float headYaw() { return transform.headYaw(); }
    public boolean onGround() { return transform.onGround(); }
    public SubjectPose pose() { return transform.pose(); }
    public Optional<SubjectMetadata> metadata() { return Optional.ofNullable(metadata); }
    public Optional<SubjectEquipment> equipment() { return Optional.ofNullable(equipment); }
    public Optional<SubjectEffects> effects() { return Optional.ofNullable(effects); }

    public static final class Builder {
        private final long replayTick;
        private final SubjectTransform transform;
        private SubjectMetadata metadata;
        private SubjectEquipment equipment;
        private SubjectEffects effects;

        private Builder(long replayTick, SubjectTransform transform) {
            this.replayTick = replayTick;
            this.transform = transform;
        }

        public Builder metadata(SubjectMetadata value) { this.metadata = value; return this; }
        public Builder equipment(SubjectEquipment value) { this.equipment = value; return this; }
        public Builder effects(SubjectEffects value) { this.effects = value; return this; }
        public ExternalSubjectFrame build() {
            return new ExternalSubjectFrame(replayTick, transform, metadata, equipment, effects);
        }
    }
}
