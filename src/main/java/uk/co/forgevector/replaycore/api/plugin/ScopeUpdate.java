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
 * An incremental change to an open scope, applied with {@link ReplayCoreMatchApi#updateScope}: participants
 * joining or leaving, team snapshots to upsert, and metadata to merge. Every field is optional; a caller
 * sets only what changed since the last {@code beginScope} or {@code updateScope} call.
 *
 * <p>Build one with the fluent {@link #builder()}, or with the all-arguments constructor. Bounds and
 * sanitisation match {@link BeginScopeRequest}: a repeated {@link ReplayParticipant#uuid()} in
 * {@link #participantsJoined()} or a repeated {@link ReplayTeam#teamId()} in {@link #teams()} collapses to
 * the last supplied value, and every collection is capped at its documented maximum with the excess
 * dropped.
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ScopeUpdate {

    /** Maximum number of {@link #participantsJoined()} entries retained per call; extra entries are dropped. */
    public static final int MAX_PARTICIPANTS_JOINED = 128;
    /** Maximum number of {@link #participantsLeft()} entries retained per call; extra entries are dropped. */
    public static final int MAX_PARTICIPANTS_LEFT = 128;
    /** Maximum number of {@link #teams()} entries retained per call; extra entries are dropped. */
    public static final int MAX_TEAMS = 64;
    /** Maximum number of {@link #metadata()} entries retained per call; extra entries are dropped. */
    public static final int MAX_METADATA_ENTRIES = 16;

    private final List<ReplayParticipant> participantsJoined;
    private final Set<UUID> participantsLeft;
    private final List<ReplayTeam> teams;
    private final Map<String, String> metadata;

    /**
     * Creates an update directly. Prefer {@link #builder()} for readability; this constructor exists for
     * callers that already hold every field.
     *
     * @param participantsJoined participants who joined since the last update, or {@code null} for none
     * @param participantsLeft   ids of participants who left since the last update, or {@code null} for none
     * @param teams               team snapshots to upsert, or {@code null} for none
     * @param metadata            metadata entries to merge into the collection's existing metadata, or
     *                            {@code null} for none
     */
    public ScopeUpdate(Collection<ReplayParticipant> participantsJoined, Collection<UUID> participantsLeft,
                        Collection<ReplayTeam> teams, Map<String, String> metadata) {
        this.participantsJoined = boundedParticipants(participantsJoined);
        this.participantsLeft = boundedUuids(participantsLeft);
        this.teams = boundedTeams(teams);
        this.metadata = sanitiseMetadata(metadata);
    }

    /** @return a new builder */
    public static Builder builder() {
        return new Builder();
    }

    /** @return participants who joined since the last update; never {@code null}, possibly empty */
    public List<ReplayParticipant> participantsJoined() { return participantsJoined; }
    /** @return ids of participants who left since the last update; never {@code null}, possibly empty */
    public Set<UUID> participantsLeft() { return participantsLeft; }
    /** @return team snapshots to upsert; never {@code null}, possibly empty */
    public List<ReplayTeam> teams() { return teams; }
    /** @return metadata entries to merge into the collection's existing metadata; never {@code null},
     *          possibly empty */
    public Map<String, String> metadata() { return metadata; }

    private static List<ReplayParticipant> boundedParticipants(Collection<ReplayParticipant> participants) {
        if (participants == null || participants.isEmpty()) return Collections.emptyList();
        LinkedHashMap<UUID, ReplayParticipant> byUuid = new LinkedHashMap<UUID, ReplayParticipant>();
        for (ReplayParticipant participant : participants) {
            if (participant == null) continue;
            if (byUuid.size() >= MAX_PARTICIPANTS_JOINED && !byUuid.containsKey(participant.uuid())) break;
            byUuid.put(participant.uuid(), participant);
        }
        return Collections.unmodifiableList(new ArrayList<ReplayParticipant>(byUuid.values()));
    }

    private static Set<UUID> boundedUuids(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) return Collections.emptySet();
        LinkedHashSet<UUID> out = new LinkedHashSet<UUID>();
        for (UUID uuid : uuids) {
            if (out.size() >= MAX_PARTICIPANTS_LEFT) break;
            if (uuid == null) continue;
            out.add(uuid);
        }
        return Collections.unmodifiableSet(out);
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

    /** A fluent builder for {@link ScopeUpdate}. Not thread-safe; build one update per builder. */
    public static final class Builder {
        private Collection<ReplayParticipant> participantsJoined;
        private Collection<UUID> participantsLeft;
        private Collection<ReplayTeam> teams;
        private Map<String, String> metadata;

        private Builder() {
        }

        /**
         * Sets the participants who joined since the last update. Extra entries beyond
         * {@link #MAX_PARTICIPANTS_JOINED} are dropped; a repeated {@link ReplayParticipant#uuid()}
         * collapses to the last supplied value.
         *
         * @param participantsJoined the participants who joined, or {@code null} for none
         * @return this builder
         */
        public Builder participantsJoined(Collection<ReplayParticipant> participantsJoined) {
            this.participantsJoined = participantsJoined;
            return this;
        }

        /**
         * Sets the ids of participants who left since the last update. Extra entries beyond
         * {@link #MAX_PARTICIPANTS_LEFT} are dropped.
         *
         * @param participantsLeft the departed participants' ids, or {@code null} for none
         * @return this builder
         */
        public Builder participantsLeft(Collection<UUID> participantsLeft) {
            this.participantsLeft = participantsLeft;
            return this;
        }

        /**
         * Sets team snapshots to upsert. Extra entries beyond {@link #MAX_TEAMS} are dropped; a repeated
         * {@link ReplayTeam#teamId()} collapses to the last supplied value.
         *
         * @param teams the team snapshots, or {@code null} for none
         * @return this builder
         */
        public Builder teams(Collection<ReplayTeam> teams) {
            this.teams = teams;
            return this;
        }

        /**
         * Adds a single metadata entry to merge into the collection's existing metadata. May be called
         * repeatedly; a later call with the same key overwrites the earlier value. The full set is bounded
         * to {@link #MAX_METADATA_ENTRIES} sanitised entries on {@link #build()}.
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
         * Builds the immutable update, applying all field bounds and sanitisation.
         *
         * @return a new {@link ScopeUpdate}
         */
        public ScopeUpdate build() {
            return new ScopeUpdate(participantsJoined, participantsLeft, teams, metadata);
        }
    }
}
