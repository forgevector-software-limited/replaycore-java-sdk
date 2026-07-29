/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;
import java.util.OptionalInt;

/** Typed optional payload carried by an {@link ExternalSubjectEvent}. */
public final class ExternalSubjectEventPayload {
    private static final ExternalSubjectEventPayload EMPTY = new Builder().build();

    private final ExternalSubjectHand hand;
    private final Integer heldSlot;
    private final ExternalSubjectEquipment equipment;
    private final ExternalSubjectEffect effect;
    private final ExternalSubjectFrame frame;
    private final boolean vectorPresent;
    private final double vectorX;
    private final double vectorY;
    private final double vectorZ;

    private ExternalSubjectEventPayload(Builder builder) {
        this.hand = builder.hand;
        this.heldSlot = builder.heldSlot;
        this.equipment = builder.equipment;
        this.effect = builder.effect;
        this.frame = builder.frame;
        this.vectorPresent = builder.vectorPresent;
        this.vectorX = finite(builder.vectorX, "vectorX");
        this.vectorY = finite(builder.vectorY, "vectorY");
        this.vectorZ = finite(builder.vectorZ, "vectorZ");
        if (heldSlot != null && (heldSlot < 0 || heldSlot > 8)) {
            throw new IllegalArgumentException("heldSlot must be between 0 and 8");
        }
    }

    public static ExternalSubjectEventPayload empty() { return EMPTY; }
    public static Builder builder() { return new Builder(); }

    public Optional<ExternalSubjectHand> hand() { return Optional.ofNullable(hand); }
    public OptionalInt heldSlot() { return heldSlot == null ? OptionalInt.empty() : OptionalInt.of(heldSlot); }
    public Optional<ExternalSubjectEquipment> equipment() { return Optional.ofNullable(equipment); }
    public Optional<ExternalSubjectEffect> effect() { return Optional.ofNullable(effect); }
    public Optional<ExternalSubjectFrame> frame() { return Optional.ofNullable(frame); }
    public boolean vectorPresent() { return vectorPresent; }
    public double vectorX() { return vectorX; }
    public double vectorY() { return vectorY; }
    public double vectorZ() { return vectorZ; }

    private static double finite(double value, String name) {
        if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException(name + " must be finite");
        return value;
    }

    public static final class Builder {
        private ExternalSubjectHand hand;
        private Integer heldSlot;
        private ExternalSubjectEquipment equipment;
        private ExternalSubjectEffect effect;
        private ExternalSubjectFrame frame;
        private boolean vectorPresent;
        private double vectorX;
        private double vectorY;
        private double vectorZ;

        public Builder hand(ExternalSubjectHand value) { this.hand = value; return this; }
        public Builder heldSlot(int value) { this.heldSlot = value; return this; }
        public Builder equipment(ExternalSubjectEquipment value) { this.equipment = value; return this; }
        public Builder effect(ExternalSubjectEffect value) { this.effect = value; return this; }
        public Builder frame(ExternalSubjectFrame value) { this.frame = value; return this; }
        public Builder vector(double x, double y, double z) {
            this.vectorPresent = true;
            this.vectorX = x;
            this.vectorY = y;
            this.vectorZ = z;
            return this;
        }
        public ExternalSubjectEventPayload build() { return new ExternalSubjectEventPayload(this); }
    }
}
