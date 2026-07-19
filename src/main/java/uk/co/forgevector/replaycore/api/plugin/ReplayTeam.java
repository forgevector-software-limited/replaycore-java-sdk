/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * One team within a {@code ReplayCollection}: its id, display name, and how it finished.
 *
 * <p>Build one with the fluent {@link #builder(String)} (a {@code teamId} is required), or with the
 * all-arguments constructor. {@link #teamId()} is validated as an identifier (required, non-blank, at most
 * {@link #MAX_ID_LENGTH} characters, rejected outright rather than truncated) because
 * {@link ReplayParticipant#teamId()} references it: silently shortening a team id could quietly attribute a
 * participant to a different, shorter team id that happens to share the same prefix. {@link #name()} and
 * {@link #result()} are free text, bounded and sanitised exactly as {@link IntegrationBookmark} bounds its
 * own optional fields.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ReplayTeam {

    /** Maximum length (characters) of {@link #teamId()}; over-length values are rejected, not truncated. */
    public static final int MAX_ID_LENGTH = 128;
    /** Maximum length (characters) of {@link #name()} and {@link #result()}; over-length values are truncated. */
    public static final int MAX_FIELD_LENGTH = 128;

    private final String teamId;
    private final String name;
    private final String result;
    private final Integer placement;

    /**
     * Creates a team directly. Prefer {@link #builder(String)} for readability; this constructor exists for
     * callers that already hold every field.
     *
     * @param teamId    the team's id (required, non-blank, at most {@link #MAX_ID_LENGTH} characters)
     * @param name      the team's display name, or {@code null}
     * @param result    a free-text outcome for this team (for example {@code "won"} or
     *                  {@code "eliminated"}), or {@code null}
     * @param placement the team's finishing position (1 for first place), or {@code null} if not
     *                  applicable; must be at least 1 when supplied
     */
    public ReplayTeam(String teamId, String name, String result, Integer placement) {
        this.teamId = requiredIdentifier(teamId, "teamId");
        this.name = nullableBounded(name);
        this.result = nullableBounded(result);
        if (placement != null && placement < 1) {
            throw new IllegalArgumentException("placement must be at least 1");
        }
        this.placement = placement;
    }

    /**
     * Starts building a team with the required id.
     *
     * @param teamId the team's id (required, non-blank, at most {@link #MAX_ID_LENGTH} characters)
     * @return a new builder
     */
    public static Builder builder(String teamId) {
        return new Builder(teamId);
    }

    /** @return the team's id; never {@code null} */
    public String teamId() { return teamId; }
    /** @return the team's display name, or {@code null} if unset */
    public String name() { return name; }
    /** @return the free-text outcome for this team, or {@code null} if unset */
    public String result() { return result; }
    /** @return the team's finishing position (1 for first place), or {@code null} if not applicable */
    public Integer placement() { return placement; }

    // See ReplayParticipant#requiredIdentifier for why this rejects rather than truncates.
    private static String requiredIdentifier(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
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

    /** A fluent builder for {@link ReplayTeam}. Not thread-safe; build one team per builder. */
    public static final class Builder {
        private final String teamId;
        private String name;
        private String result;
        private Integer placement;

        private Builder(String teamId) {
            this.teamId = teamId;
        }

        /**
         * Sets the team's display name.
         *
         * @param name the display name, or {@code null} to clear
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets a free-text outcome for this team.
         *
         * @param result the outcome, or {@code null} to clear
         * @return this builder
         */
        public Builder result(String result) {
            this.result = result;
            return this;
        }

        /**
         * Sets the team's finishing position.
         *
         * @param placement the finishing position (1 for first place), or {@code null} to clear
         * @return this builder
         */
        public Builder placement(Integer placement) {
            this.placement = placement;
            return this;
        }

        /**
         * Builds the immutable team, applying all field bounds and sanitisation.
         *
         * @return a new {@link ReplayTeam}
         * @throws IllegalArgumentException if {@code teamId} is blank, too long, contains a control
         *                                   character, or {@code placement} is less than 1
         */
        public ReplayTeam build() {
            return new ReplayTeam(teamId, name, result, placement);
        }
    }
}
