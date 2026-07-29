/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;
import java.util.UUID;

/** Immutable registration descriptor for a caller-owned external subject. */
public final class ExternalSubjectDescriptor {
    private final UUID subjectId;
    private final SubjectType type;
    private final String profileName;
    private final SignedTexture skin;
    private final SubjectTransform initialTransform;
    private final SubjectMetadata initialMetadata;
    private final SubjectEquipment initialEquipment;

    public ExternalSubjectDescriptor(UUID subjectId, SubjectType type, String profileName,
                                     SignedTexture skin, SubjectTransform initialTransform,
                                     SubjectMetadata initialMetadata,
                                     SubjectEquipment initialEquipment) {
        this.subjectId = Objects.requireNonNull(subjectId, "subjectId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.profileName = profileName(profileName);
        this.skin = Objects.requireNonNull(skin, "skin must not be null");
        this.initialTransform = Objects.requireNonNull(initialTransform,
                "initialTransform must not be null");
        this.initialMetadata = initialMetadata == null ? SubjectMetadata.empty() : initialMetadata;
        this.initialEquipment = initialEquipment == null ? SubjectEquipment.empty() : initialEquipment;
    }

    public static ExternalSubjectDescriptor playerShaped(UUID subjectId, String profileName,
                                                          SignedTexture skin,
                                                          SubjectTransform initialTransform) {
        return new ExternalSubjectDescriptor(subjectId, SubjectType.PLAYER_SHAPED, profileName, skin,
                initialTransform, SubjectMetadata.empty(), SubjectEquipment.empty());
    }

    public UUID subjectId() { return subjectId; }
    public SubjectType type() { return type; }
    public String profileName() { return profileName; }
    public SignedTexture skin() { return skin; }
    public SubjectTransform initialTransform() { return initialTransform; }
    public SubjectMetadata initialMetadata() { return initialMetadata; }
    public SubjectEquipment initialEquipment() { return initialEquipment; }

    private static String profileName(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("profileName must not be empty");
        String trimmed = value.trim();
        if (trimmed.length() > ExternalSubjectPresentation.MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("profileName is too long");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c < 0x20 || c == 0x7f) throw new IllegalArgumentException("profileName contains a control character");
        }
        return trimmed;
    }
}
