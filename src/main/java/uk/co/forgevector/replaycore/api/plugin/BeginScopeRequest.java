/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The request that opens a logical match scope with {@link ReplayCoreMatchApi#beginScope}.
 *
 * <p>{@code idempotencyKey} is caller-provided: re-sending the same value returns the same
 * {@link ReplayScope} rather than opening a second one, so a plugin that is unsure whether an earlier
 * {@code beginScope} call actually reached the backend (for example after a timeout) can safely retry with
 * the identical key instead of risking a duplicate scope. {@code category}, {@code mode} and
 * {@code policyId} are extensible strings: ReplayCore never enumerates their values, so a network's own
 * game modes and policy names never need a corresponding change here.
 *
 * <p>Build one with the fluent {@link #builder(String, String, String, String, String)}; the five required
 * fields keep a positional all-arguments constructor error-prone at this field count, so none is exposed.
 * {@code idempotencyKey}, {@code externalMatchId} and {@code policyId} are validated as identifiers:
 * required, non-blank, at most {@link #MAX_ID_LENGTH} characters, and rejected outright (not truncated) on
 * violation, because silently truncating any of the three would risk a false match against an unrelated
 * key, external match or policy. {@code category} and {@code mode} are bounded and character-class folded
 * like {@link IntegrationBookmark}'s metadata keys: trimmed, control characters stripped, capped at
 * {@link #MAX_FIELD_LENGTH} characters, and any character outside {@code [A-Za-z0-9_.:-]} replaced with
 * {@code '_'}, since they gate catalogue faceting rather than any internal cross-reference. Every
 * collection field is capped at its documented maximum; entries beyond the cap are dropped, matching
 * {@link IntegrationBookmark}'s metadata bounding. Duplicate participants (by {@link ReplayParticipant#uuid()})
 * and duplicate teams (by {@link ReplayTeam#teamId()}) collapse to the last supplied value for that id,
 * exactly as a repeated {@link IntegrationBookmark.Builder#metadata(String, String)} key does. The backend,
 * not this builder, resolves whether a participant's {@link ReplayParticipant#teamId()} matches an entry in
 * {@link #teams()}.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class BeginScopeRequest {

    /** Maximum length (characters) of {@code idempotencyKey}, {@code externalMatchId} and {@code policyId};
     *  over-length values are rejected, not truncated. */
    public static final int MAX_ID_LENGTH = 128;
    /** Maximum length (characters) of {@code category}, {@code mode}, each world entry, and each
     *  competition-hierarchy string field; over-length values are truncated. */
    public static final int MAX_FIELD_LENGTH = 128;
    /** Maximum number of {@link #worlds()} entries retained; extra entries are dropped. */
    public static final int MAX_WORLDS = 32;
    /** Maximum number of {@link #participants()} entries retained; extra entries are dropped. */
    public static final int MAX_PARTICIPANTS = 256;
    /** Maximum number of {@link #teams()} entries retained; extra entries are dropped. */
    public static final int MAX_TEAMS = 64;
    /** Maximum number of {@link #spectators()} entries retained; extra entries are dropped. */
    public static final int MAX_SPECTATORS = 512;
    /** Maximum number of {@link #metadata()} entries retained; extra entries are dropped. */
    public static final int MAX_METADATA_ENTRIES = 16;

    private final String idempotencyKey;
    private final String externalMatchId;
    private final String category;
    private final String mode;
    private final String policyId;
    private final List<String> worlds;
    private final List<ReplayParticipant> participants;
    private final List<ReplayTeam> teams;
    private final Set<UUID> spectators;
    private final String competitionId;
    private final String seriesId;
    private final String roundId;
    private final String bracketMatchId;
    private final Integer gameNumber;
    private final Integer bestOf;
    private final String partyEventId;
    private final Map<String, String> metadata;

    private BeginScopeRequest(Builder b) {
        this.idempotencyKey = requiredIdentifier(b.idempotencyKey, "idempotencyKey", MAX_ID_LENGTH);
        this.externalMatchId = requiredIdentifier(b.externalMatchId, "externalMatchId", MAX_ID_LENGTH);
        this.category = foldedField(required(b.category, "category"));
        this.mode = foldedField(required(b.mode, "mode"));
        this.policyId = requiredIdentifier(b.policyId, "policyId", MAX_ID_LENGTH);
        this.worlds = boundedWorlds(b.worlds);
        this.participants = boundedParticipants(b.participants);
        this.teams = boundedTeams(b.teams);
        this.spectators = boundedSpectators(b.spectators);
        this.competitionId = nullableBounded(b.competitionId);
        this.seriesId = nullableBounded(b.seriesId);
        this.roundId = nullableBounded(b.roundId);
        this.bracketMatchId = nullableBounded(b.bracketMatchId);
        this.gameNumber = positiveOrNull(b.gameNumber, "gameNumber");
        this.bestOf = positiveOrNull(b.bestOf, "bestOf");
        this.partyEventId = nullableBounded(b.partyEventId);
        this.metadata = sanitiseMetadata(b.metadata);
    }

    /**
     * Starts building a begin-scope request with the five required fields.
     *
     * @param idempotencyKey  the caller-provided idempotency key (required, non-blank, at most
     *                        {@link #MAX_ID_LENGTH} characters); re-sending the same value returns the
     *                        same scope instead of opening a second one
     * @param externalMatchId the external system's own id for this match (required, non-blank, at most
     *                        {@link #MAX_ID_LENGTH} characters)
     * @param category        the extensible category, for example a game family (required, non-blank)
     * @param mode            the extensible mode within the category (required, non-blank)
     * @param policyId        the id of the release policy to resolve and snapshot on the collection
     *                        (required, non-blank, at most {@link #MAX_ID_LENGTH} characters)
     * @return a new builder
     */
    public static Builder builder(String idempotencyKey, String externalMatchId, String category,
                                   String mode, String policyId) {
        return new Builder(idempotencyKey, externalMatchId, category, mode, policyId);
    }

    /** @return the caller-provided idempotency key; never {@code null} */
    public String idempotencyKey() { return idempotencyKey; }
    /** @return the external match id; never {@code null} */
    public String externalMatchId() { return externalMatchId; }
    /** @return the extensible category; never {@code null} */
    public String category() { return category; }
    /** @return the extensible mode; never {@code null} */
    public String mode() { return mode; }
    /** @return the release policy id to resolve and snapshot; never {@code null} */
    public String policyId() { return policyId; }
    /** @return the worlds or regions this scope covers; never {@code null}, possibly empty */
    public List<String> worlds() { return worlds; }
    /** @return the participants known at scope start; never {@code null}, possibly empty */
    public List<ReplayParticipant> participants() { return participants; }
    /** @return the teams known at scope start; never {@code null}, possibly empty */
    public List<ReplayTeam> teams() { return teams; }
    /** @return the spectating players known at scope start; never {@code null}, possibly empty */
    public Set<UUID> spectators() { return spectators; }
    /** @return the competition id, or {@code null} if not part of a tracked competition */
    public String competitionId() { return competitionId; }
    /** @return the series id, or {@code null} if not part of a series */
    public String seriesId() { return seriesId; }
    /** @return the round id, or {@code null} if not part of a round */
    public String roundId() { return roundId; }
    /** @return the bracket match id, or {@code null} if not part of a bracket */
    public String bracketMatchId() { return bracketMatchId; }
    /** @return the game number within a best-of series (at least 1), or {@code null} if not applicable */
    public Integer gameNumber() { return gameNumber; }
    /** @return the best-of count for the series (at least 1), or {@code null} if not applicable */
    public Integer bestOf() { return bestOf; }
    /** @return the party or event id this match belongs to, or {@code null} if not applicable */
    public String partyEventId() { return partyEventId; }
    /** @return the sanitised, unmodifiable metadata map; never {@code null}, possibly empty */
    public Map<String, String> metadata() { return metadata; }

    // Rejected, not truncated: see the class documentation for why identifiers fail closed here.
    private static String requiredIdentifier(String value, String name, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must be at most " + maxLength + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) < 0x20 || trimmed.charAt(i) == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return trimmed;
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return value.trim();
    }

    private static String foldedField(String value) {
        String stripped = stripControl(value);
        String bounded = stripped.length() <= MAX_FIELD_LENGTH ? stripped : stripped.substring(0, MAX_FIELD_LENGTH);
        StringBuilder out = new StringBuilder(bounded.length());
        for (int i = 0; i < bounded.length(); i++) {
            char c = bounded.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == ':' || c == '-';
            out.append(allowed ? c : '_');
        }
        return out.toString();
    }

    private static Integer positiveOrNull(Integer value, String name) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException(name + " must be at least 1");
        }
        return value;
    }

    private static List<String> boundedWorlds(Collection<String> worlds) {
        if (worlds == null || worlds.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<String>();
        for (String world : worlds) {
            if (out.size() >= MAX_WORLDS) break;
            if (world == null || world.trim().isEmpty()) continue;
            out.add(bounded(world.trim()));
        }
        return Collections.unmodifiableList(out);
    }

    private static List<ReplayParticipant> boundedParticipants(Collection<ReplayParticipant> participants) {
        if (participants == null || participants.isEmpty()) return Collections.emptyList();
        LinkedHashMap<UUID, ReplayParticipant> byUuid = new LinkedHashMap<UUID, ReplayParticipant>();
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

    private static Set<UUID> boundedSpectators(Collection<UUID> spectators) {
        if (spectators == null || spectators.isEmpty()) return Collections.emptySet();
        LinkedHashSet<UUID> out = new LinkedHashSet<UUID>();
        for (UUID spectator : spectators) {
            if (out.size() >= MAX_SPECTATORS) break;
            if (spectator == null) continue;
            out.add(spectator);
        }
        return Collections.unmodifiableSet(out);
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
     * A fluent builder for {@link BeginScopeRequest}. Not thread-safe; build one request per builder.
     */
    public static final class Builder {
        private final String idempotencyKey;
        private final String externalMatchId;
        private final String category;
        private final String mode;
        private final String policyId;
        private Collection<String> worlds;
        private Collection<ReplayParticipant> participants;
        private Collection<ReplayTeam> teams;
        private Collection<UUID> spectators;
        private String competitionId;
        private String seriesId;
        private String roundId;
        private String bracketMatchId;
        private Integer gameNumber;
        private Integer bestOf;
        private String partyEventId;
        private Map<String, String> metadata;

        private Builder(String idempotencyKey, String externalMatchId, String category, String mode,
                         String policyId) {
            this.idempotencyKey = idempotencyKey;
            this.externalMatchId = externalMatchId;
            this.category = category;
            this.mode = mode;
            this.policyId = policyId;
        }

        /**
         * Sets the worlds or regions this scope covers. Extra entries beyond {@link #MAX_WORLDS} are
         * dropped; blank entries are dropped.
         *
         * @param worlds the worlds or regions, or {@code null} for none
         * @return this builder
         */
        public Builder worlds(Collection<String> worlds) {
            this.worlds = worlds;
            return this;
        }

        /**
         * Sets the participants known at scope start. Extra entries beyond {@link #MAX_PARTICIPANTS} are
         * dropped; a repeated {@link ReplayParticipant#uuid()} collapses to the last supplied value.
         *
         * @param participants the participants, or {@code null} for none
         * @return this builder
         */
        public Builder participants(Collection<ReplayParticipant> participants) {
            this.participants = participants;
            return this;
        }

        /**
         * Sets the teams known at scope start. Extra entries beyond {@link #MAX_TEAMS} are dropped; a
         * repeated {@link ReplayTeam#teamId()} collapses to the last supplied value.
         *
         * @param teams the teams, or {@code null} for none
         * @return this builder
         */
        public Builder teams(Collection<ReplayTeam> teams) {
            this.teams = teams;
            return this;
        }

        /**
         * Sets the spectating players known at scope start. Extra entries beyond {@link #MAX_SPECTATORS}
         * are dropped.
         *
         * @param spectators the spectating players' ids, or {@code null} for none
         * @return this builder
         */
        public Builder spectators(Collection<UUID> spectators) {
            this.spectators = spectators;
            return this;
        }

        /**
         * Sets the competition-hierarchy fields identifying the competition, series, round, bracket match
         * and party/event a scope belongs to. ReplayCore stores and filters on these; it derives no
         * standings from them.
         *
         * @param competitionId  the competition id, or {@code null}
         * @param seriesId       the series id, or {@code null}
         * @param roundId        the round id, or {@code null}
         * @param bracketMatchId the bracket match id, or {@code null}
         * @param gameNumber     the game number within a best-of series (at least 1 if supplied), or
         *                       {@code null}
         * @param bestOf         the best-of count for the series (at least 1 if supplied), or {@code null}
         * @param partyEventId   the party or event id this match belongs to, or {@code null}
         * @return this builder
         */
        public Builder competitionHierarchy(String competitionId, String seriesId, String roundId,
                                             String bracketMatchId, Integer gameNumber, Integer bestOf,
                                             String partyEventId) {
            this.competitionId = competitionId;
            this.seriesId = seriesId;
            this.roundId = roundId;
            this.bracketMatchId = bracketMatchId;
            this.gameNumber = gameNumber;
            this.bestOf = bestOf;
            this.partyEventId = partyEventId;
            return this;
        }

        /**
         * Adds a single metadata entry. May be called repeatedly; a later call with the same key
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
         * @return a new {@link BeginScopeRequest}
         * @throws IllegalArgumentException if {@code idempotencyKey}, {@code externalMatchId} or
         *                                   {@code policyId} is blank, too long or contains a control
         *                                   character; if {@code category} or {@code mode} is blank; or if
         *                                   {@code gameNumber} or {@code bestOf} is supplied and less
         *                                   than 1
         */
        public BeginScopeRequest build() {
            return new BeginScopeRequest(this);
        }
    }
}
