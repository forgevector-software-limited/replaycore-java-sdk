/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A request to mint one playable event clip on ONE named scope, handed to
 * {@link ReplayCoreMatchApi#recordScopeClip}. It describes a moment - a kill, a round ending - as a window
 * AROUND NOW plus the players it concerns, and the recorder turns that into exactly one asset with one
 * relationship row per player, reachable from a killer's kill feed, a victim's death feed and the match
 * timeline without ever storing a second copy.
 *
 * <h2>No absolute ticks, deliberately</h2>
 * <p>This request carries NO tick coordinate, only two RELATIVE offsets: how far before the moment the clip
 * should start ({@link #preRollTicks()}) and how far after it should end ({@link #postRollTicks()}). The
 * moment itself is always "now" - the recorder stamps it at the current segment-local tick as the call is
 * made, because a gameplay event handler runs AT the moment it is describing and "now" is the only tick it
 * can honestly express.
 *
 * <p>This is a deliberate removal, not an omission. Three tick spaces exist on a running recorder - the raw
 * server tick, the cumulative session tick ({@link ReplayScope#startTick()}, what
 * {@link RecordingControlApi#currentTick()} reports), and the segment-local tick a clip window is actually
 * stored in - and only the last is correct here. An integrator can reach the first two and has no reliable
 * way to reach the third, so an API that accepted an absolute tick would make the most natural call an
 * integrator could write silently store a coordinate wrong by the whole uncaptured span, which reads back as
 * entirely plausible footage of the wrong moment with nothing ever throwing. Supplying only offsets removes
 * that whole class of mistake: there is no wrong value to pass.
 *
 * <p><strong>Both offsets are TICKS, never a {@link java.time.Duration}.</strong> 20 ticks is one second on
 * an unlagged server. The recorder never converts wall-clock time into ticks and neither should a caller:
 * ticks pause when the server is empty, so no wall-clock quantity is a valid tick quantity.
 *
 * <h2>How the window is clamped</h2>
 * <p>The recorder always clamps toward LESS footage, never more:
 * <ul>
 *   <li>The start never precedes the scope's own start on the current archive, so a kill two seconds into a
 *       duel can never show the previous match on the same arena - footage the viewer has no entitlement to,
 *       served under this match's collection.</li>
 *   <li>If the recording rotates to a new archive before the post-roll has elapsed, the window is clamped to
 *       the end of the archive the moment happened on. A clip window belongs to exactly one archive; a
 *       slightly short clip is correct, a window running past its archive's end is not.</li>
 *   <li>If the scope ends before the post-roll has elapsed, the window is clamped to the scope's end.</li>
 * </ul>
 *
 * <h2>What a clip shows</h2>
 * <p>A clip's ROUTING is guaranteed - see {@link ReplayCoreMatchApi#recordScopeClip} - but its FOOTAGE is
 * not spatially filtered. A scope is a tick window over one shared recording, so playing this clip renders
 * everything the recorder captured in those ticks, including an unrelated match happening simultaneously in
 * the same world. {@link BeginScopeRequest#worlds()} narrows to worlds only, which narrows nothing when two
 * concurrent duels share one arena world.
 *
 * <pre>{@code
 * matches.recordScopeClip(scopeId, ScopeClipRequest.builder(EventKind.KILL)
 *         .preRollTicks(120L)   // 6s before the kill
 *         .postRollTicks(60L)   // 3s after it
 *         .killer(killerUuid)
 *         .victim(victimUuid)
 *         .clientEventId(duelId + "-kill-" + killerUuid + "-" + victimUuid)
 *         .build());
 * }</pre>
 *
 * <p>Immutable and thread-safe once built.
 */
public final class ScopeClipRequest {

    /** Maximum number of {@link #relationships()} entries retained; matches the cloud's own cap exactly. */
    public static final int MAX_RELATIONSHIPS = 256;
    /** Maximum {@link #preRollTicks()}; larger values are clamped, never rejected. 1200 ticks is 60 seconds. */
    public static final long MAX_PRE_ROLL_TICKS = 1_200L;
    /** Maximum {@link #postRollTicks()}; larger values are clamped, never rejected. 600 ticks is 30 seconds. */
    public static final long MAX_POST_ROLL_TICKS = 600L;
    /** Maximum length (characters) of {@link #clientEventId()}; over-length values are rejected, not truncated. */
    public static final int MAX_CLIENT_EVENT_ID_LENGTH = 200;

    private final EventKind eventKind;
    private final long preRollTicks;
    private final long postRollTicks;
    private final Map<UUID, AssetRelationship> relationships;
    private final String clientEventId;

    private ScopeClipRequest(Builder b) {
        this.eventKind = Objects.requireNonNull(b.eventKind, "eventKind must not be null");
        // Clamped rather than rejected: an over-long pre-roll is a caller asking for more context than the
        // recorder will give, which is an ordinary operational condition rather than a programmer error,
        // and this surface does not throw out of a gameplay path for those.
        this.preRollTicks = clamp(b.preRollTicks, MAX_PRE_ROLL_TICKS);
        this.postRollTicks = clamp(b.postRollTicks, MAX_POST_ROLL_TICKS);
        this.relationships = boundedRelationships(b.relationships);
        this.clientEventId = b.clientEventId == null ? null
                : requiredIdentifier(b.clientEventId, "clientEventId");
    }

    /**
     * Starts building a clip request for one kind of in-game moment.
     *
     * @param eventKind what happened; must not be {@code null}
     * @return a new builder
     */
    public static Builder builder(EventKind eventKind) {
        return new Builder(eventKind);
    }

    /** @return the kind of in-game moment this clip captures; never {@code null} */
    public EventKind eventKind() { return eventKind; }
    /** @return how many ticks BEFORE the moment the window starts, clamped to {@link #MAX_PRE_ROLL_TICKS} */
    public long preRollTicks() { return preRollTicks; }
    /** @return how many ticks AFTER the moment the window ends, clamped to {@link #MAX_POST_ROLL_TICKS} */
    public long postRollTicks() { return postRollTicks; }
    /** @return the bounded, unmodifiable player-relationship map; never {@code null}, possibly empty */
    public Map<UUID, AssetRelationship> relationships() { return relationships; }
    /**
     * @return the caller's own id for this moment, or {@code null} to have the recorder mint one. Supplying
     *         one lets a caller that retries its OWN event handling converge on a single clip rather than
     *         minting a second one for the same moment; leaving it {@code null} is correct for an event
     *         handler that fires exactly once per moment, which is the normal case.
     */
    public String clientEventId() { return clientEventId; }

    private static long clamp(long value, long max) {
        if (value < 0L) return 0L;
        return value > max ? max : value;
    }

    private static String requiredIdentifier(String value, String name) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (trimmed.length() > MAX_CLIENT_EVENT_ID_LENGTH) {
            throw new IllegalArgumentException(
                    name + " must be at most " + MAX_CLIENT_EVENT_ID_LENGTH + " characters");
        }
        for (int i = 0; i < trimmed.length(); i++) {
            if (trimmed.charAt(i) < 0x20 || trimmed.charAt(i) == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return trimmed;
    }

    private static Map<UUID, AssetRelationship> boundedRelationships(Map<UUID, AssetRelationship> relationships) {
        if (relationships == null || relationships.isEmpty()) return Collections.emptyMap();
        LinkedHashMap<UUID, AssetRelationship> out = new LinkedHashMap<UUID, AssetRelationship>();
        for (Map.Entry<UUID, AssetRelationship> entry : relationships.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) continue;
            if (out.size() >= MAX_RELATIONSHIPS && !out.containsKey(entry.getKey())) break;
            out.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(out);
    }

    @Override
    public String toString() {
        return "ScopeClipRequest{eventKind=" + eventKind.name().toLowerCase(Locale.ROOT)
                + ", preRollTicks=" + preRollTicks
                + ", postRollTicks=" + postRollTicks
                + ", relationships=" + relationships.size()
                + (clientEventId == null ? "" : ", clientEventId=" + clientEventId)
                + '}';
    }

    /** A fluent builder for {@link ScopeClipRequest}. Not thread-safe; build one request per builder. */
    public static final class Builder {
        private final EventKind eventKind;
        private long preRollTicks;
        private long postRollTicks;
        private Map<UUID, AssetRelationship> relationships;
        private String clientEventId;

        private Builder(EventKind eventKind) {
            this.eventKind = eventKind;
        }

        /**
         * Sets how many ticks BEFORE the moment the clip window starts (20 ticks is one second on an
         * unlagged server). Clamped to {@link ScopeClipRequest#MAX_PRE_ROLL_TICKS}, and clamped again by the
         * recorder so the window can never reach back before the scope's own start on the current archive.
         *
         * @param ticks the pre-roll in ticks; a negative value is treated as zero
         * @return this builder
         */
        public Builder preRollTicks(long ticks) {
            this.preRollTicks = ticks;
            return this;
        }

        /**
         * Sets how many ticks AFTER the moment the clip window ends. Clamped to
         * {@link ScopeClipRequest#MAX_POST_ROLL_TICKS}, and clamped again by the recorder if the recording
         * rotates or the scope ends before the post-roll has elapsed.
         *
         * @param ticks the post-roll in ticks; a negative value is treated as zero
         * @return this builder
         */
        public Builder postRollTicks(long ticks) {
            this.postRollTicks = ticks;
            return this;
        }

        /**
         * Records the player who caused the moment, as {@link AssetRelationship#KILL}.
         *
         * @param playerUuid the killer's id; must not be {@code null}
         * @return this builder
         */
        public Builder killer(UUID playerUuid) {
            return relationship(playerUuid, AssetRelationship.KILL);
        }

        /**
         * Records the player the moment happened to, as {@link AssetRelationship#DEATH}.
         *
         * @param playerUuid the victim's id; must not be {@code null}
         * @return this builder
         */
        public Builder victim(UUID playerUuid) {
            return relationship(playerUuid, AssetRelationship.DEATH);
        }

        /**
         * Records another player present in the moment, as {@link AssetRelationship#PARTICIPANT}.
         *
         * @param playerUuid the player's id; must not be {@code null}
         * @return this builder
         */
        public Builder participant(UUID playerUuid) {
            return relationship(playerUuid, AssetRelationship.PARTICIPANT);
        }

        /**
         * Records how one player relates to this clip. May be called repeatedly; a later call for the same
         * player overwrites the earlier relation, so a player is never given two conflicting meanings on one
         * asset. The full set is bounded to {@link ScopeClipRequest#MAX_RELATIONSHIPS} entries on
         * {@link #build()}.
         *
         * @param playerUuid the player's id; must not be {@code null}
         * @param relation   how the player relates to this clip; must not be {@code null}
         * @return this builder
         */
        public Builder relationship(UUID playerUuid, AssetRelationship relation) {
            Objects.requireNonNull(playerUuid, "playerUuid must not be null");
            Objects.requireNonNull(relation, "relation must not be null");
            if (this.relationships == null) {
                this.relationships = new LinkedHashMap<UUID, AssetRelationship>();
            }
            this.relationships.put(playerUuid, relation);
            return this;
        }

        /**
         * Adds every entry from {@code relationships} (a convenience for a pre-built map).
         *
         * @param relationships the relationships to add, or {@code null} for none
         * @return this builder
         */
        public Builder relationships(Map<UUID, AssetRelationship> relationships) {
            if (relationships != null && !relationships.isEmpty()) {
                if (this.relationships == null) {
                    this.relationships = new LinkedHashMap<UUID, AssetRelationship>();
                }
                this.relationships.putAll(relationships);
            }
            return this;
        }

        /**
         * Sets the caller's own id for this moment. See {@link ScopeClipRequest#clientEventId()} for when
         * this is worth supplying.
         *
         * @param id the id, or {@code null} to have the recorder mint one; must not be blank, longer than
         *           {@link ScopeClipRequest#MAX_CLIENT_EVENT_ID_LENGTH} characters, or contain control
         *           characters
         * @return this builder
         */
        public Builder clientEventId(String id) {
            this.clientEventId = id;
            return this;
        }

        /**
         * Builds the immutable request, applying all field bounds.
         *
         * @return a new {@link ScopeClipRequest}
         * @throws IllegalArgumentException if {@code clientEventId} is set but blank, over-length or
         *                                   contains a control character
         */
        public ScopeClipRequest build() {
            return new ScopeClipRequest(this);
        }
    }
}
