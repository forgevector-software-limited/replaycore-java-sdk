/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.UUID;

/** Complete authoritative spatial state for a player-shaped subject. */
public final class SubjectTransform {
    private final UUID worldId;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final float headYaw;
    private final float bodyYaw;
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;
    private final boolean onGround;
    private final SubjectPose pose;

    private SubjectTransform(Builder builder) {
        this.worldId = Objects.requireNonNull(builder.worldId, "worldId must not be null");
        this.x = finite(builder.x, "x");
        this.y = finite(builder.y, "y");
        this.z = finite(builder.z, "z");
        this.yaw = finite(builder.yaw, "yaw");
        this.pitch = finite(builder.pitch, "pitch");
        this.headYaw = finite(builder.headYaw, "headYaw");
        this.bodyYaw = finite(builder.bodyYaw, "bodyYaw");
        this.velocityX = finite(builder.velocityX, "velocityX");
        this.velocityY = finite(builder.velocityY, "velocityY");
        this.velocityZ = finite(builder.velocityZ, "velocityZ");
        this.onGround = builder.onGround;
        this.pose = Objects.requireNonNull(builder.pose, "pose must not be null");
    }

    public static Builder builder(UUID worldId, double x, double y, double z) {
        return new Builder(worldId, x, y, z);
    }

    public UUID worldId() { return worldId; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float headYaw() { return headYaw; }
    public float bodyYaw() { return bodyYaw; }
    public double velocityX() { return velocityX; }
    public double velocityY() { return velocityY; }
    public double velocityZ() { return velocityZ; }
    public boolean onGround() { return onGround; }
    public SubjectPose pose() { return pose; }

    private static double finite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    private static float finite(float value, String name) {
        if (Float.isNaN(value) || Float.isInfinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private final UUID worldId;
        private final double x;
        private final double y;
        private final double z;
        private float yaw;
        private float pitch;
        private float headYaw;
        private float bodyYaw;
        private double velocityX;
        private double velocityY;
        private double velocityZ;
        private boolean onGround;
        private SubjectPose pose = SubjectPose.STANDING;

        private Builder(UUID worldId, double x, double y, double z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Builder rotation(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.headYaw = yaw;
            this.bodyYaw = yaw;
            return this;
        }
        public Builder headYaw(float value) { this.headYaw = value; return this; }
        public Builder bodyYaw(float value) { this.bodyYaw = value; return this; }
        public Builder velocity(double x, double y, double z) {
            this.velocityX = x;
            this.velocityY = y;
            this.velocityZ = z;
            return this;
        }
        public Builder onGround(boolean value) { this.onGround = value; return this; }
        public Builder pose(SubjectPose value) { this.pose = value; return this; }
        public SubjectTransform build() { return new SubjectTransform(this); }
    }
}
