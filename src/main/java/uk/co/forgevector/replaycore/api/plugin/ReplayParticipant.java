/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One player's membership of a {@code ReplayCollection}: who they are, which team they were on, what role
 * they played, when they were present, and how the collection resolved for them.
 *
 * <p>Build one with the fluent {@link #builder(UUID, String)} (a {@code uuid} and a {@code username}
 * snapshot are required), or with the all-arguments constructor. Every optional field is bounded and
 * sanitised on construction, exactly as {@link IntegrationBookmark} bounds and sanitises its own optional
 * fields: strings are trimmed, control characters are stripped and the result is capped at
 * {@link #MAX_FIELD_LENGTH} characters. {@link #teamId()}, when supplied, is treated as an identifier
 * rather than free text: it is required to be non-blank and within {@link #MAX_ID_LENGTH} characters
 * outright rather than silently truncated, because a truncated team id could quietly re-attribute a
 * participant to a different, shorter team id that happens to share the same prefix.
 *
 * <p>{@link #role()} defaults to {@link ParticipantRole#PARTICIPANT} when not set, the same "sensible
 * default for an unset optional" pattern {@link IntegrationBookmark#severity()} uses for
 * {@link IntegrationBookmark.Severity#INFO}.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ReplayParticipant {

    /** Maximum length (characters) of any single bounded text field; over-length values are truncated. */
    public static final int MAX_FIELD_LENGTH = 128;
    /** Maximum length (characters) of {@link #teamId()}; over-length values are rejected, not truncated. */
    public static final int MAX_ID_LENGTH = 128;

    private final UUID uuid;
    private final String username;
    private final String teamId;
    private final ParticipantRole role;
    private final Instant joinedAt;
    private final Instant leftAt;
    private final String result;

    /**
     * Creates a participant directly. Prefer {@link #builder(UUID, String)} for readability; this
     * constructor exists for callers that already hold every field.
     *
     * @param uuid     the player's id (required)
     * @param username the player's username at the time of this snapshot (required, non-blank)
     * @param teamId   the team this participant belonged to, or {@code null}; validated as an identifier
     *                 (non-blank, at most {@link #MAX_ID_LENGTH} characters) when supplied
     * @param role     the participant's role, or {@code null} for {@link ParticipantRole#PARTICIPANT}
     * @param joinedAt when the participant joined the collection, or {@code null} if unknown
     * @param leftAt   when the participant left the collection, or {@code null} if still present or
     *                 unknown; must not be before {@code joinedAt} when both are supplied
     * @param result   a free-text outcome for this participant (for example {@code "won"} or
     *                 {@code "eliminated"}), or {@code null}
     */
    public ReplayParticipant(UUID uuid, String username, String teamId, ParticipantRole role,
                              Instant joinedAt, Instant leftAt, String result) {
        this.uuid = Objects.requireNonNull(uuid, "uuid must not be null");
        this.username = bounded(required(username, "username"));
        this.teamId = teamId == null ? null : requiredIdentifier(teamId, "teamId");
        this.role = role == null ? ParticipantRole.PARTICIPANT : role;
        if (joinedAt != null && leftAt != null && leftAt.isBefore(joinedAt)) {
            throw new IllegalArgumentException("leftAt must not be before joinedAt");
        }
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.result = nullableBounded(result);
    }

    /**
     * Starts building a participant with the two required fields.
     *
     * @param uuid     the player's id (required)
     * @param username the player's username at the time of this snapshot (required, non-blank)
     * @return a new builder
     */
    public static Builder builder(UUID uuid, String username) {
        return new Builder(uuid, username);
    }

    /** @return the player's id; never {@code null} */
    public UUID uuid() { return uuid; }
    /** @return the player's username snapshot; never {@code null} */
    public String username() { return username; }
    /** @return the team this participant belonged to, or {@code null} if unset or not on a team */
    public String teamId() { return teamId; }
    /** @return the participant's role; never {@code null} */
    public ParticipantRole role() { return role; }
    /** @return when the participant joined, or {@code null} if unknown */
    public Instant joinedAt() { return joinedAt; }
    /** @return when the participant left, or {@code null} if still present or unknown */
    public Instant leftAt() { return leftAt; }
    /** @return the free-text outcome for this participant, or {@code null} if unset */
    public String result() { return result; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }

    // Identifiers that another entry in the same request can reference (here, ReplayTeam#teamId()) are
    // rejected outright rather than truncated: silently shortening one would risk a collision with a
    // different, shorter id that happens to share the same prefix, quietly re-attributing this participant
    // to the wrong team. Free-text fields below use bounded()/nullableBounded() instead, which truncate.
    private static String requiredIdentifier(String value, String name) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(name + " must be at most " + MAX_ID_LENGTH + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) < 0x20 || trimmed.charAt(i) == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return trimmed;
    }

    private static String nullableBounded(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return bounded(value.trim());
    }

    private static String bounded(String value) {
        String stripped = stripControl(value);
        if (stripped.length() <= MAX_FIELD_LENGTH) return stripped;
        return stripped.substring(0, MAX_FIELD_LENGTH);
    }

    private static String stripControl(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 0x20 && c != 0x7f) || c == '\t') out.append(c);
        }
        return out.toString();
    }

    /**
     * A fluent builder for {@link ReplayParticipant}. Not thread-safe; build one participant per builder.
     */
    public static final class Builder {
        private final UUID uuid;
        private final String username;
        private String teamId;
        private ParticipantRole role;
        private Instant joinedAt;
        private Instant leftAt;
        private String result;

        private Builder(UUID uuid, String username) {
            this.uuid = uuid;
            this.username = username;
        }

        /**
         * Sets the team this participant belonged to.
         *
         * @param teamId the team id, or {@code null} to clear
         * @return this builder
         */
        public Builder teamId(String teamId) {
            this.teamId = teamId;
            return this;
        }

        /**
         * Sets the participant's role (defaults to {@link ParticipantRole#PARTICIPANT}).
         *
         * @param role the role, or {@code null} for {@link ParticipantRole#PARTICIPANT}
         * @return this builder
         */
        public Builder role(ParticipantRole role) {
            this.role = role;
            return this;
        }

        /**
         * Sets when the participant joined and left the collection.
         *
         * @param joinedAt when the participant joined, or {@code null} if unknown
         * @param leftAt   when the participant left, or {@code null} if still present or unknown
         * @return this builder
         */
        public Builder presence(Instant joinedAt, Instant leftAt) {
            this.joinedAt = joinedAt;
            this.leftAt = leftAt;
            return this;
        }

        /**
         * Sets a free-text outcome for this participant.
         *
         * @param result the outcome, or {@code null} to clear
         * @return this builder
         */
        public Builder result(String result) {
            this.result = result;
            return this;
        }

        /**
         * Builds the immutable participant, applying all field bounds and sanitisation.
         *
         * @return a new {@link ReplayParticipant}
         * @throws IllegalArgumentException if {@code username} is blank, or {@code teamId} is present but
         *                                   blank, too long, or contains a control character
         */
        public ReplayParticipant build() {
            return new ReplayParticipant(uuid, username, teamId, role, joinedAt, leftAt, result);
        }
    }
}
