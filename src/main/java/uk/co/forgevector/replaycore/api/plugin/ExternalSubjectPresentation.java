/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Captured name and signed skin presentation effective from one replay tick. */
public final class ExternalSubjectPresentation {
    public static final int MAX_NAME_LENGTH = 64;

    private final long replayTick;
    private final long profileVersion;
    private final UUID profileUuid;
    private final String profileName;
    private final String displayName;
    private final SignedTexture skin;
    private final boolean visible;
    private final boolean nameVisible;
    private final boolean skinVisible;

    private ExternalSubjectPresentation(Builder builder) {
        if (builder.replayTick < 0L) throw new IllegalArgumentException("replayTick must not be negative");
        if (builder.profileVersion < 1L) throw new IllegalArgumentException("profileVersion must be at least 1");
        this.replayTick = builder.replayTick;
        this.profileVersion = builder.profileVersion;
        this.profileUuid = builder.profileUuid;
        this.profileName = name(builder.profileName, "profileName");
        this.displayName = builder.displayName == null ? this.profileName : name(builder.displayName, "displayName");
        this.skin = builder.skin;
        this.visible = builder.visible;
        this.nameVisible = builder.nameVisible;
        this.skinVisible = builder.skinVisible;
        if (skinVisible && skin == null) throw new IllegalArgumentException("skin must be supplied when skinVisible is true");
    }

    public static Builder builder(long replayTick, long profileVersion, String profileName) {
        return new Builder(replayTick, profileVersion, profileName);
    }

    public long replayTick() { return replayTick; }
    public long effectiveTick() { return replayTick; }
    public long profileVersion() { return profileVersion; }
    public Optional<UUID> profileUuid() { return Optional.ofNullable(profileUuid); }
    public String profileName() { return profileName; }
    public String displayName() { return displayName; }
    public Optional<SignedTexture> skin() { return Optional.ofNullable(skin); }
    public boolean visible() { return visible; }
    public boolean nameVisible() { return nameVisible; }
    public boolean skinVisible() { return skinVisible; }

    private static String name(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be empty");
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException(field + " is too long");
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) throw new IllegalArgumentException(field + " contains a control character");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExternalSubjectPresentation)) return false;
        ExternalSubjectPresentation that = (ExternalSubjectPresentation) other;
        return replayTick == that.replayTick && profileVersion == that.profileVersion && visible == that.visible
                && nameVisible == that.nameVisible && skinVisible == that.skinVisible
                && Objects.equals(profileUuid, that.profileUuid) && profileName.equals(that.profileName)
                && displayName.equals(that.displayName) && Objects.equals(skin, that.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replayTick, profileVersion, profileUuid, profileName, displayName, skin,
                visible, nameVisible, skinVisible);
    }

    public static final class Builder {
        private final long replayTick;
        private final long profileVersion;
        private final String profileName;
        private UUID profileUuid;
        private String displayName;
        private SignedTexture skin;
        private boolean visible = true;
        private boolean nameVisible = true;
        private boolean skinVisible;

        private Builder(long replayTick, long profileVersion, String profileName) {
            this.replayTick = replayTick;
            this.profileVersion = profileVersion;
            this.profileName = profileName;
        }

        public Builder profileUuid(UUID value) { this.profileUuid = value; return this; }
        public Builder displayName(String value) { this.displayName = value; return this; }
        public Builder skin(SignedTexture value) { this.skin = value; this.skinVisible = value != null; return this; }
        public Builder visible(boolean value) { this.visible = value; return this; }
        public Builder nameVisible(boolean value) { this.nameVisible = value; return this; }
        public Builder skinVisible(boolean value) { this.skinVisible = value; return this; }
        public ExternalSubjectPresentation build() { return new ExternalSubjectPresentation(this); }
    }
}
