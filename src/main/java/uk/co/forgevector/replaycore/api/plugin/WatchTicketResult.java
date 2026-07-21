/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The outcome {@link ReplayCatalogApi#createWatchTicket} completes with: EITHER a minted, redeemable ticket
 * ({@link #ready()} {@code true}) OR a bounded preparing status for an asset that passed every
 * authorisation check but has not finished processing yet ({@link #ready()} {@code false}), mirroring
 * {@code POST /v1/replay-assets/{id}/watch-ticket}'s {@code 200}/{@code 202} response split (RFC-0009
 * section 10).
 *
 * <h2>This is a MINT, not a redemption</h2>
 * <p>{@link #ticket()} is a short-lived, single-use, single-purpose token authorising ONE viewer to watch
 * ONE asset; it carries no permanent storage URL of any kind. Exchanging it for playable bytes is a
 * separate, viewer-side {@code POST /v1/replay-assets/{id}/watch-ticket/redeem} call outside this type's
 * scope - nothing here performs that exchange.
 *
 * <p>Never logged and never persisted beyond the caller's own immediate use: {@link #ticket()} is returned
 * exactly once, from this mint call, exactly like the cloud's own single-use ticket contract.
 *
 * <p>Obtain an instance only from {@link ReplayCatalogApi#createWatchTicket}; there is no public
 * constructor, since a hand-built instance could never be redeemed against a real, cloud-issued ticket.
 *
 * <p>Immutable and thread-safe.
 */
public final class WatchTicketResult {

    private final String assetId;
    private final boolean ready;
    private final String ticket;
    private final List<PlaybackSegment> segments;
    private final Instant expiresAt;
    private final long expiresInSeconds;
    private final ProcessingState processingState;
    private final int retryAfterSeconds;

    private WatchTicketResult(String assetId, boolean ready, String ticket, List<PlaybackSegment> segments,
                               Instant expiresAt, long expiresInSeconds, ProcessingState processingState,
                               int retryAfterSeconds) {
        this.assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        this.ready = ready;
        this.ticket = ticket;
        this.segments = segments;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
        this.processingState = processingState;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Creates a minted, ready-to-redeem result. Not for addon use; the recorder's own catalogue
     * implementation calls this when parsing a {@code 200} mint response.
     *
     * @param assetId          the minted asset's id; must not be {@code null}
     * @param ticket           the single-use ticket token; must not be {@code null}
     * @param segments         the ordered playback segments backing this asset; must not be {@code null}
     * @param expiresAt        the instant the ticket stops being claimable; must not be {@code null}
     * @param expiresInSeconds how many seconds from mint the ticket remains claimable
     * @return a ready {@link WatchTicketResult}
     */
    public static WatchTicketResult ready(String assetId, String ticket, List<PlaybackSegment> segments,
                                           Instant expiresAt, long expiresInSeconds) {
        Objects.requireNonNull(ticket, "ticket must not be null");
        Objects.requireNonNull(segments, "segments must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        return new WatchTicketResult(assetId, true, ticket,
                Collections.unmodifiableList(new ArrayList<PlaybackSegment>(segments)), expiresAt,
                expiresInSeconds, null, 0);
    }

    /**
     * Creates a bounded preparing result. Not for addon use; the recorder's own catalogue implementation
     * calls this when parsing a {@code 202} preparing response.
     *
     * @param assetId           the asset's id; must not be {@code null}
     * @param processingState   the asset's current processing state; must not be {@code null}
     * @param retryAfterSeconds the bounded interval, in seconds, a caller should wait before retrying
     * @return a preparing {@link WatchTicketResult}
     */
    public static WatchTicketResult preparing(String assetId, ProcessingState processingState, int retryAfterSeconds) {
        Objects.requireNonNull(processingState, "processingState must not be null");
        return new WatchTicketResult(assetId, false, null, Collections.<PlaybackSegment>emptyList(), null,
                0L, processingState, retryAfterSeconds);
    }

    /** @return the asset this result concerns; never {@code null} */
    public String assetId() { return assetId; }
    /** @return {@code true} if a ticket was minted, {@code false} if the asset is still preparing */
    public boolean ready() { return ready; }
    /** @return the single-use ticket token, present only when {@link #ready()} */
    public Optional<String> ticket() { return Optional.ofNullable(ticket); }
    /** @return the ordered playback segments backing this asset; empty while {@link #ready()} is
     *          {@code false} */
    public List<PlaybackSegment> segments() { return segments; }
    /** @return the instant the ticket stops being claimable, present only when {@link #ready()} */
    public Optional<Instant> expiresAt() { return Optional.ofNullable(expiresAt); }
    /** @return how many seconds from mint the ticket remains claimable; {@code 0} while {@link #ready()}
     *          is {@code false} */
    public long expiresInSeconds() { return expiresInSeconds; }
    /** @return the asset's processing state, present only when {@link #ready()} is {@code false} */
    public Optional<ProcessingState> processingState() { return Optional.ofNullable(processingState); }
    /** @return the bounded interval, in seconds, a caller should wait before retrying;
     *          {@code 0} when {@link #ready()} */
    public int retryAfterSeconds() { return retryAfterSeconds; }

    /**
     * One contributing archive's playback window for a minted asset, in playback order, mirroring the
     * cloud's {@code PlaybackSegment}. {@link #startTick()}/{@link #endTick()} are SEGMENT-LOCAL to
     * {@link #replayId()}'s own archive, never a collection-wide or cross-segment coordinate - an asset
     * spanning an archive rotation resolves to more than one of these.
     *
     * <p>Immutable and thread-safe.
     */
    public static final class PlaybackSegment {
        private final String replayId;
        private final int sequenceIndex;
        private final long startTick;
        private final long endTick;

        /**
         * @param replayId      the contributing archive's id; must not be {@code null}
         * @param sequenceIndex 0-based playback order within the asset
         * @param startTick     the segment-local start tick
         * @param endTick       the segment-local end tick
         */
        public PlaybackSegment(String replayId, int sequenceIndex, long startTick, long endTick) {
            this.replayId = Objects.requireNonNull(replayId, "replayId must not be null");
            this.sequenceIndex = sequenceIndex;
            this.startTick = startTick;
            this.endTick = endTick;
        }

        /** @return the contributing archive's id; never {@code null} */
        public String replayId() { return replayId; }
        /** @return this segment's 0-based playback order within the asset */
        public int sequenceIndex() { return sequenceIndex; }
        /** @return the segment-local start tick */
        public long startTick() { return startTick; }
        /** @return the segment-local end tick */
        public long endTick() { return endTick; }
    }
}
