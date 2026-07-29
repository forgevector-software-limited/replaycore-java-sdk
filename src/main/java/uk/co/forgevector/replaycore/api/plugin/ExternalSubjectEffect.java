/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/** Captured status-effect state used by EFFECT_ADD and EFFECT_REMOVE events. */
public final class ExternalSubjectEffect {
    private final String effectKey;
    private final int amplifier;
    private final long durationTicks;
    private final boolean ambient;
    private final boolean showParticles;
    private final boolean showIcon;

    public ExternalSubjectEffect(String effectKey, int amplifier, long durationTicks, boolean ambient,
                                 boolean showParticles, boolean showIcon) {
        if (effectKey == null || effectKey.trim().isEmpty()) throw new IllegalArgumentException("effectKey is required");
        if (effectKey.length() > 256) throw new IllegalArgumentException("effectKey is too long");
        if (amplifier < 0 || amplifier > 255) throw new IllegalArgumentException("amplifier must be between 0 and 255");
        if (durationTicks < 0L) throw new IllegalArgumentException("durationTicks must not be negative");
        this.effectKey = effectKey.trim();
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.ambient = ambient;
        this.showParticles = showParticles;
        this.showIcon = showIcon;
    }

    public String effectKey() { return effectKey; }
    public int amplifier() { return amplifier; }
    public long durationTicks() { return durationTicks; }
    public boolean ambient() { return ambient; }
    public boolean showParticles() { return showParticles; }
    public boolean showIcon() { return showIcon; }
}
