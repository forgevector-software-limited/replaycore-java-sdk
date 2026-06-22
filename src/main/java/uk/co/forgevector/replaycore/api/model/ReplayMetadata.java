/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable metadata describing a single recorded replay.
 *
 * <p>This is the SDK's view of the cloud's replay record (spec &sect;11.4),
 * returned by {@code getReplay} and within each page of {@code listReplays}.
 * Every field corresponds to a field on the wire; nullable and omittable fields
 * are exposed as {@link Optional} so a caller is never handed a surprise
 * {@code null}.
 *
 * <p>Instances are built by the SDK from a server response and are never mutated.
 * Use {@link Builder} only in tests or when constructing fixtures.
 *
 * <p><strong>Readiness.</strong> The cloud has no single "status" field; a replay
 * is watchable when it has been signed and its manifest hash is present.
 * {@link #isReady()} encodes that rule so callers do not have to.
 */
public final class ReplayMetadata {

    private final String id;
    private final String tenantId;
    private final String serverId;
    private final String serverName;
    private final String displayName;
    private final String integration;
    private final Quality quality;
    private final Instant startedAt;
    private final Instant endedAt;
    private final Long durationMs;
    private final Long sizeBytes;
    private final StorageTier storageTier;
    private final Instant retentionUntil;
    private final Visibility visibility;
    private final String signatureKid;
    private final String manifestHash;
    private final boolean hasStaffArchive;
    private final String redactedFrom;
    private final ArchiveStatus archiveStatus;
    private final boolean crashFinalised;
    private final boolean starred;
    private final String recoveryStatus;
    private final int formatVersion;
    private final List<Participant> participants;
    private final String storageMode;
    private final String sessionId;

    private ReplayMetadata(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.tenantId = b.tenantId;
        this.serverId = b.serverId;
        this.serverName = b.serverName;
        this.displayName = b.displayName;
        this.integration = b.integration;
        this.quality = b.quality != null ? b.quality : Quality.UNKNOWN;
        this.startedAt = b.startedAt;
        this.endedAt = b.endedAt;
        this.durationMs = b.durationMs;
        this.sizeBytes = b.sizeBytes;
        this.storageTier = b.storageTier != null ? b.storageTier : StorageTier.UNKNOWN;
        this.retentionUntil = b.retentionUntil;
        this.visibility = b.visibility != null ? b.visibility : Visibility.UNKNOWN;
        this.signatureKid = b.signatureKid;
        this.manifestHash = b.manifestHash;
        this.hasStaffArchive = b.hasStaffArchive;
        this.redactedFrom = b.redactedFrom;
        this.archiveStatus = b.archiveStatus != null ? b.archiveStatus : ArchiveStatus.UNKNOWN;
        this.crashFinalised = b.crashFinalised;
        this.starred = b.starred;
        this.recoveryStatus = b.recoveryStatus;
        this.formatVersion = b.formatVersion;
        this.participants = b.participants != null
                ? Collections.unmodifiableList(b.participants)
                : Collections.<Participant>emptyList();
        this.storageMode = b.storageMode;
        this.sessionId = b.sessionId;
    }

    /**
     * Returns the replay's unique identifier (a v4 UUID string).
     *
     * @return the replay id; never {@code null}
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the identifier of the tenant that owns this replay. Always the
     * caller's own tenant, because every keyed request is tenant-scoped.
     *
     * @return the tenant id, or an empty optional if the server omitted it
     */
    public Optional<String> getTenantId() {
        return Optional.ofNullable(tenantId);
    }

    /**
     * Returns the identifier of the server that recorded this replay.
     *
     * @return the server id, or an empty optional if the server omitted it
     */
    public Optional<String> getServerId() {
        return Optional.ofNullable(serverId);
    }

    /**
     * Returns the human-readable name of the recording server, when set.
     *
     * @return the server name, or an empty optional
     */
    public Optional<String> getServerName() {
        return Optional.ofNullable(serverName);
    }

    /**
     * Returns the owner-set display name for this replay, when one has been
     * assigned in the panel. When absent, the panel derives a name from the
     * server label, game mode and date.
     *
     * @return the display name, or an empty optional
     */
    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the integration / game mode the replay was recorded under (for
     * example a BedWars or Duels mode), when set.
     *
     * @return the integration label, or an empty optional
     */
    public Optional<String> getIntegration() {
        return Optional.ofNullable(integration);
    }

    /**
     * Returns the capture quality tier.
     *
     * @return the quality; never {@code null} (may be {@link Quality#UNKNOWN})
     */
    public Quality getQuality() {
        return quality;
    }

    /**
     * Returns the instant the recording started.
     *
     * @return the start instant, or an empty optional if the server omitted it
     */
    public Optional<Instant> getStartedAt() {
        return Optional.ofNullable(startedAt);
    }

    /**
     * Returns the instant the recording ended. Absent while a replay is still
     * being recorded.
     *
     * @return the end instant, or an empty optional
     */
    public Optional<Instant> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }

    /**
     * Returns the recording's wall-clock duration in milliseconds. Absent until
     * the replay is finalised.
     *
     * @return the duration in milliseconds, or an empty optional
     */
    public Optional<Long> getDurationMs() {
        return Optional.ofNullable(durationMs);
    }

    /**
     * Returns the recording duration as a {@link Duration}, when known.
     *
     * @return the duration, or an empty optional
     */
    public Optional<Duration> getDuration() {
        return durationMs != null ? Optional.of(Duration.ofMillis(durationMs)) : Optional.<Duration>empty();
    }

    /**
     * Returns the archive size in bytes, when known.
     *
     * @return the size in bytes, or an empty optional
     */
    public Optional<Long> getSizeBytes() {
        return Optional.ofNullable(sizeBytes);
    }

    /**
     * Returns the storage tier the archive currently sits in.
     *
     * @return the storage tier; never {@code null} (may be {@link StorageTier#UNKNOWN})
     */
    public StorageTier getStorageTier() {
        return storageTier;
    }

    /**
     * Returns the instant after which the replay is eligible for retention
     * deletion, unless it is starred.
     *
     * @return the retention deadline, or an empty optional if the server omitted it
     */
    public Optional<Instant> getRetentionUntil() {
        return Optional.ofNullable(retentionUntil);
    }

    /**
     * Returns who may watch this replay.
     *
     * @return the visibility; never {@code null} (may be {@link Visibility#UNKNOWN})
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Returns the key id of the signature over the archive. The sentinel value
     * {@code "staging-unsigned"} (and an empty value) indicates the replay is not
     * yet finalised; see {@link #isReady()}.
     *
     * @return the signing key id, or an empty optional
     */
    public Optional<String> getSignatureKid() {
        return Optional.ofNullable(signatureKid);
    }

    /**
     * Returns the SHA-256 manifest hash of the archive, when finalised.
     *
     * @return the manifest hash (64 hex chars), or an empty optional
     */
    public Optional<String> getManifestHash() {
        return Optional.ofNullable(manifestHash);
    }

    /**
     * Returns whether a separately-signed staff-stripped derivative archive
     * exists for this replay. Archive selection is always server-side; this flag
     * is informational only.
     *
     * @return {@code true} if a staff archive exists
     */
    public boolean hasStaffArchive() {
        return hasStaffArchive;
    }

    /**
     * Returns the id of the source replay this one was redacted from, when this
     * is a redaction derivative.
     *
     * @return the source replay id, or an empty optional
     */
    public Optional<String> getRedactedFrom() {
        return Optional.ofNullable(redactedFrom);
    }

    /**
     * Returns the provenance of the stored archive.
     *
     * @return the archive status; never {@code null} (may be {@link ArchiveStatus#UNKNOWN})
     */
    public ArchiveStatus getArchiveStatus() {
        return archiveStatus;
    }

    /**
     * Returns whether this replay was completed by crash recovery. Such a replay
     * is watchable but visibly flagged.
     *
     * @return {@code true} if crash-finalised
     */
    public boolean isCrashFinalised() {
        return crashFinalised;
    }

    /**
     * Returns whether the replay is starred. A starred replay is exempt from the
     * retention auto-delete sweep for as long as it stays starred.
     *
     * @return {@code true} if starred
     */
    public boolean isStarred() {
        return starred;
    }

    /**
     * Returns a recovery-status label (for example {@code "crash-finalised"}),
     * when the replay went through recovery.
     *
     * @return the recovery status, or an empty optional
     */
    public Optional<String> getRecoveryStatus() {
        return Optional.ofNullable(recoveryStatus);
    }

    /**
     * Returns the replay archive's format version.
     *
     * @return the format version
     */
    public int getFormatVersion() {
        return formatVersion;
    }

    /**
     * Returns the roster of players seen during the recording. Never {@code null};
     * empty when the recorder sent no roster.
     *
     * @return an unmodifiable list of participants
     */
    public List<Participant> getParticipants() {
        return participants;
    }

    /**
     * Returns the RFC-0006 storage layout. {@code "segmented"} indicates the
     * recording was rotated into a linked sequence of session segments; absent
     * for an ordinary single-archive replay.
     *
     * @return the storage mode, or an empty optional
     */
    public Optional<String> getStorageMode() {
        return Optional.ofNullable(storageMode);
    }

    /**
     * Returns whether this replay is stored as a segmented RFC-0006 session.
     *
     * @return {@code true} if {@code storageMode} is {@code "segmented"}
     */
    public boolean isSegmented() {
        return "segmented".equals(storageMode);
    }

    /**
     * Returns the stable RFC-0006 session identifier shared by every segment of a
     * segmented replay. Present only for segmented replays.
     *
     * @return the session id, or an empty optional
     */
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }

    /**
     * Returns whether the replay is finalised and watchable.
     *
     * <p>The cloud exposes no single status field; a replay is ready once it has
     * been signed with a real key (the signing key id is present and is not the
     * {@code "staging-unsigned"} sentinel) and its manifest hash is set. This
     * method encodes exactly that rule.
     *
     * @return {@code true} if the replay is finalised and watchable
     */
    public boolean isReady() {
        return signatureKid != null
                && !signatureKid.isEmpty()
                && !"staging-unsigned".equals(signatureKid)
                && manifestHash != null
                && !manifestHash.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReplayMetadata)) {
            return false;
        }
        ReplayMetadata that = (ReplayMetadata) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ReplayMetadata{id=" + id
                + ", displayName=" + displayName
                + ", integration=" + integration
                + ", visibility=" + visibility
                + ", startedAt=" + startedAt
                + ", ready=" + isReady() + '}';
    }

    /**
     * Returns a new builder for assembling a {@link ReplayMetadata} instance.
     * Used internally for deserialisation and by tests building fixtures.
     *
     * @param id the replay id; never {@code null}
     * @return a fresh builder seeded with the required id
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * A mutable builder for {@link ReplayMetadata}. Not thread-safe. Every setter
     * returns {@code this} for chaining; {@link #build()} produces an immutable
     * instance.
     */
    public static final class Builder {
        private final String id;
        private String tenantId;
        private String serverId;
        private String serverName;
        private String displayName;
        private String integration;
        private Quality quality;
        private Instant startedAt;
        private Instant endedAt;
        private Long durationMs;
        private Long sizeBytes;
        private StorageTier storageTier;
        private Instant retentionUntil;
        private Visibility visibility;
        private String signatureKid;
        private String manifestHash;
        private boolean hasStaffArchive;
        private String redactedFrom;
        private ArchiveStatus archiveStatus;
        private boolean crashFinalised;
        private boolean starred;
        private String recoveryStatus;
        private int formatVersion;
        private List<Participant> participants;
        private String storageMode;
        private String sessionId;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Sets the owning tenant id.
         *
         * @param v the tenant id
         * @return this builder
         */
        public Builder tenantId(String v) { this.tenantId = v; return this; }

        /**
         * Sets the recording server id.
         *
         * @param v the server id
         * @return this builder
         */
        public Builder serverId(String v) { this.serverId = v; return this; }

        /**
         * Sets the recording server's display name.
         *
         * @param v the server name
         * @return this builder
         */
        public Builder serverName(String v) { this.serverName = v; return this; }

        /**
         * Sets the owner-assigned display name.
         *
         * @param v the display name
         * @return this builder
         */
        public Builder displayName(String v) { this.displayName = v; return this; }

        /**
         * Sets the integration / game mode label.
         *
         * @param v the integration or game mode
         * @return this builder
         */
        public Builder integration(String v) { this.integration = v; return this; }

        /**
         * Sets the capture quality tier.
         *
         * @param v the quality tier
         * @return this builder
         */
        public Builder quality(Quality v) { this.quality = v; return this; }

        /**
         * Sets the recording start instant.
         *
         * @param v the start instant
         * @return this builder
         */
        public Builder startedAt(Instant v) { this.startedAt = v; return this; }

        /**
         * Sets the recording end instant.
         *
         * @param v the end instant
         * @return this builder
         */
        public Builder endedAt(Instant v) { this.endedAt = v; return this; }

        /**
         * Sets the recording duration in milliseconds.
         *
         * @param v the duration in milliseconds
         * @return this builder
         */
        public Builder durationMs(Long v) { this.durationMs = v; return this; }

        /**
         * Sets the archive size in bytes.
         *
         * @param v the size in bytes
         * @return this builder
         */
        public Builder sizeBytes(Long v) { this.sizeBytes = v; return this; }

        /**
         * Sets the storage tier.
         *
         * @param v the storage tier
         * @return this builder
         */
        public Builder storageTier(StorageTier v) { this.storageTier = v; return this; }

        /**
         * Sets the retention deadline.
         *
         * @param v the retention deadline
         * @return this builder
         */
        public Builder retentionUntil(Instant v) { this.retentionUntil = v; return this; }

        /**
         * Sets the visibility.
         *
         * @param v the visibility
         * @return this builder
         */
        public Builder visibility(Visibility v) { this.visibility = v; return this; }

        /**
         * Sets the archive signing key id.
         *
         * @param v the signing key id
         * @return this builder
         */
        public Builder signatureKid(String v) { this.signatureKid = v; return this; }

        /**
         * Sets the archive manifest hash.
         *
         * @param v the manifest hash
         * @return this builder
         */
        public Builder manifestHash(String v) { this.manifestHash = v; return this; }

        /**
         * Sets whether a staff-stripped derivative archive exists.
         *
         * @param v {@code true} if a staff archive exists
         * @return this builder
         */
        public Builder hasStaffArchive(boolean v) { this.hasStaffArchive = v; return this; }

        /**
         * Sets the source replay id for a redaction derivative.
         *
         * @param v the source replay id
         * @return this builder
         */
        public Builder redactedFrom(String v) { this.redactedFrom = v; return this; }

        /**
         * Sets the archive provenance.
         *
         * @param v the archive status
         * @return this builder
         */
        public Builder archiveStatus(ArchiveStatus v) { this.archiveStatus = v; return this; }

        /**
         * Sets whether the replay was completed by crash recovery.
         *
         * @param v {@code true} if crash-finalised
         * @return this builder
         */
        public Builder crashFinalised(boolean v) { this.crashFinalised = v; return this; }

        /**
         * Sets whether the replay is starred.
         *
         * @param v {@code true} if starred
         * @return this builder
         */
        public Builder starred(boolean v) { this.starred = v; return this; }

        /**
         * Sets the recovery-status label.
         *
         * @param v the recovery status
         * @return this builder
         */
        public Builder recoveryStatus(String v) { this.recoveryStatus = v; return this; }

        /**
         * Sets the archive format version.
         *
         * @param v the format version
         * @return this builder
         */
        public Builder formatVersion(int v) { this.formatVersion = v; return this; }

        /**
         * Sets the participant roster.
         *
         * @param v the participants
         * @return this builder
         */
        public Builder participants(List<Participant> v) { this.participants = v; return this; }

        /**
         * Sets the RFC-0006 storage mode.
         *
         * @param v the storage mode
         * @return this builder
         */
        public Builder storageMode(String v) { this.storageMode = v; return this; }

        /**
         * Sets the RFC-0006 session id.
         *
         * @param v the session id
         * @return this builder
         */
        public Builder sessionId(String v) { this.sessionId = v; return this; }

        /**
         * Builds the immutable {@link ReplayMetadata}.
         *
         * @return a new immutable instance
         */
        public ReplayMetadata build() {
            return new ReplayMetadata(this);
        }
    }
}
