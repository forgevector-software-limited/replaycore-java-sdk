/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The request that ends a logical match scope with {@link ReplayCoreMatchApi#endScope}: the scope's own
 * {@code idempotencyKey}, distinct from the one supplied at {@link BeginScopeRequest}, plus the final
 * participant and team outcomes and any closing metadata.
 *
 * <p>{@code idempotencyKey} makes the end-scope call itself durable and retry-safe: re-sending the same
 * value returns the original {@link FinalizeResult} rather than finalizing the collection a second time, so
 * a plugin that is unsure whether an earlier {@code endScope} call reached the backend can safely retry
 * with the identical key. Ending a scope must never start, stop, rotate or cut the physical recording; only
 * the logical scope closes.
 *
 * <p>Build one with the fluent {@link #builder(String)} (an {@code idempotencyKey} is required), or with
 * the all-arguments constructor. Bounds and sanitisation match {@link BeginScopeRequest}: a repeated
 * {@link ReplayParticipant#uuid()} in {@link #participants()} or a repeated {@link ReplayTeam#teamId()} in
 * {@link #teams()} collapses to the last supplied value, and every collection is capped at its documented
 * maximum with the excess dropped.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class EndScopeRequest {

    /** Maximum length (characters) of {@code idempotencyKey}; over-length values are rejected, not truncated. */
    public static final int MAX_ID_LENGTH = 128;
    /** Maximum number of {@link #participants()} entries retained; extra entries are dropped. */
    public static final int MAX_PARTICIPANTS = 256;
    /** Maximum number of {@link #teams()} entries retained; extra entries are dropped. */
    public static final int MAX_TEAMS = 64;
    /** Maximum number of {@link #metadata()} entries retained; extra entries are dropped. */
    public static final int MAX_METADATA_ENTRIES = 16;

    private final String idempotencyKey;
    private final List<ReplayParticipant> participants;
    private final List<ReplayTeam> teams;
    private final Map<String, String> metadata;

    /**
     * Creates an end-scope request directly. Prefer {@link #builder(String)} for readability; this
     * constructor exists for callers that already hold every field.
     *
     * @param idempotencyKey the caller-provided idempotency key for this end-scope call (required,
     *                       non-blank, at most {@link #MAX_ID_LENGTH} characters)
     * @param participants   the final participant outcomes, or {@code null} for none
     * @param teams          the final team outcomes, or {@code null} for none
     * @param metadata       closing metadata to merge into the collection's existing metadata, or
     *                       {@code null} for none
     */
    public EndScopeRequest(String idempotencyKey, Collection<ReplayParticipant> participants,
                            Collection<ReplayTeam> teams, Map<String, String> metadata) {
        this.idempotencyKey = requiredIdentifier(idempotencyKey, "idempotencyKey");
        this.participants = boundedParticipants(participants);
        this.teams = boundedTeams(teams);
        this.metadata = sanitiseMetadata(metadata);
    }

    /**
     * Starts building an end-scope request with the required idempotency key.
     *
     * @param idempotencyKey the caller-provided idempotency key for this end-scope call (required,
     *                       non-blank, at most {@link #MAX_ID_LENGTH} characters)
     * @return a new builder
     */
    public static Builder builder(String idempotencyKey) {
        return new Builder(idempotencyKey);
    }

    /** @return the idempotency key for this end-scope call; never {@code null} */
    public String idempotencyKey() { return idempotencyKey; }
    /** @return the final participant outcomes; never {@code null}, possibly empty */
    public List<ReplayParticipant> participants() { return participants; }
    /** @return the final team outcomes; never {@code null}, possibly empty */
    public List<ReplayTeam> teams() { return teams; }
    /** @return closing metadata to merge into the collection's existing metadata; never {@code null},
     *          possibly empty */
    public Map<String, String> metadata() { return metadata; }

    // Rejected, not truncated: see BeginScopeRequest's class documentation for why identifiers fail closed.
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

    private static List<ReplayParticipant> boundedParticipants(Collection<ReplayParticipant> participants) {
        if (participants == null || participants.isEmpty()) return Collections.emptyList();
        LinkedHashMap<java.util.UUID, ReplayParticipant> byUuid = new LinkedHashMap<java.util.UUID, ReplayParticipant>();
        for (ReplayParticipant participant : participants) {
            if (participant == null) continue;
            if (byUuid.size() >= MAX_PARTICIPANTS && !byUuid.containsKey(participant.uuid())) break;
            byUuid.put(participant.uuid(), participant);
        }
        return Collections.unmodifiableList(new ArrayList<ReplayParticipant>(byUuid.values()));
    }

    private static List<ReplayTeam> boundedTeams(Collection<ReplayTeam> teams) {
        if (teams == null || teams.isEmpty()) return Collections.emptyList();
        LinkedHashMap<String, ReplayTeam> byId = new LinkedHashMap<String, ReplayTeam>();
        for (ReplayTeam team : teams) {
            if (team == null) continue;
            if (byId.size() >= MAX_TEAMS && !byId.containsKey(team.teamId())) break;
            byId.put(team.teamId(), team);
        }
        return Collections.unmodifiableList(new ArrayList<ReplayTeam>(byId.values()));
    }

    private static Map<String, String> sanitiseMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) return Collections.emptyMap();
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (out.size() >= MAX_METADATA_ENTRIES) break;
            if (entry.getKey() == null || entry.getValue() == null) continue;
            String key = entry.getKey().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
            if (key.isEmpty()) continue;
            out.put(bounded(key), bounded(entry.getValue().trim()));
        }
        return Collections.unmodifiableMap(out);
    }

    private static String bounded(String value) {
        String stripped = stripControl(value);
        if (stripped.length() <= 128) return stripped;
        return stripped.substring(0, 128);
    }

    private static String stripControl(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c >= 0x20 && c != 0x7f) || c == '\t') out.append(c);
        }
        return out.toString();
    }

    /** A fluent builder for {@link EndScopeRequest}. Not thread-safe; build one request per builder. */
    public static final class Builder {
        private final String idempotencyKey;
        private Collection<ReplayParticipant> participants;
        private Collection<ReplayTeam> teams;
        private Map<String, String> metadata;

        private Builder(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        /**
         * Sets the final participant outcomes. Extra entries beyond {@link #MAX_PARTICIPANTS} are dropped;
         * a repeated {@link ReplayParticipant#uuid()} collapses to the last supplied value.
         *
         * @param participants the final participant outcomes, or {@code null} for none
         * @return this builder
         */
        public Builder participants(Collection<ReplayParticipant> participants) {
            this.participants = participants;
            return this;
        }

        /**
         * Sets the final team outcomes. Extra entries beyond {@link #MAX_TEAMS} are dropped; a repeated
         * {@link ReplayTeam#teamId()} collapses to the last supplied value.
         *
         * @param teams the final team outcomes, or {@code null} for none
         * @return this builder
         */
        public Builder teams(Collection<ReplayTeam> teams) {
            this.teams = teams;
            return this;
        }

        /**
         * Adds a single closing metadata entry. May be called repeatedly; a later call with the same key
         * overwrites the earlier value. The full set is bounded to {@link #MAX_METADATA_ENTRIES} sanitised
         * entries on {@link #build()}.
         *
         * @param key   the metadata key (lower-cased and restricted to {@code [a-z0-9_.-]} on build)
         * @param value the metadata value
         * @return this builder
         */
        public Builder metadata(String key, String value) {
            if (this.metadata == null) {
                this.metadata = new LinkedHashMap<String, String>();
            }
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Adds every entry from {@code metadata} (a convenience for a pre-built map). The full set is
         * bounded to {@link #MAX_METADATA_ENTRIES} sanitised entries on {@link #build()}.
         *
         * @param metadata the metadata to add, or {@code null} for none
         * @return this builder
         */
        public Builder metadata(Map<String, String> metadata) {
            if (metadata != null && !metadata.isEmpty()) {
                if (this.metadata == null) {
                    this.metadata = new LinkedHashMap<String, String>();
                }
                this.metadata.putAll(metadata);
            }
            return this;
        }

        /**
         * Builds the immutable request, applying all field bounds and validation.
         *
         * @return a new {@link EndScopeRequest}
         * @throws IllegalArgumentException if {@code idempotencyKey} is blank, too long or contains a
         *                                   control character
         */
        public EndScopeRequest build() {
            return new EndScopeRequest(idempotencyKey, participants, teams, metadata);
        }
    }
}
