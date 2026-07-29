/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Result of minting a short-lived, single-use replay watch ticket. */
public final class WatchTicketResult {
    private final String assetId;
    private final boolean ready;
    private final String token;
    private final URI redemptionUrl;
    private final List<PlaybackSegment> segments;
    private final Instant expiresAt;
    private final long expiresInSeconds;
    private final boolean singleUse;
    private final ProcessingState processingState;
    private final int retryAfterSeconds;

    private WatchTicketResult(String assetId, boolean ready, String token, URI redemptionUrl,
                              List<PlaybackSegment> segments, Instant expiresAt,
                              long expiresInSeconds, boolean singleUse,
                              ProcessingState processingState, int retryAfterSeconds) {
        this.assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        this.ready = ready;
        this.token = token;
        this.redemptionUrl = redemptionUrl;
        this.segments = segments;
        this.expiresAt = expiresAt;
        this.expiresInSeconds = expiresInSeconds;
        this.singleUse = singleUse;
        this.processingState = processingState;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Compatibility factory for a legacy response that did not carry its redemption URL. */
    public static WatchTicketResult ready(String assetId, String token, List<PlaybackSegment> segments,
                                          Instant expiresAt, long expiresInSeconds) {
        return ready(assetId, token, null, segments, expiresAt, expiresInSeconds, true);
    }

    public static WatchTicketResult ready(String assetId, String token, URI redemptionUrl,
                                          List<PlaybackSegment> segments, Instant expiresAt,
                                          long expiresInSeconds, boolean singleUse) {
        Objects.requireNonNull(token, "token must not be null");
        Objects.requireNonNull(segments, "segments must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        return new WatchTicketResult(assetId, true, token, redemptionUrl,
                Collections.unmodifiableList(new ArrayList<PlaybackSegment>(segments)),
                expiresAt, expiresInSeconds, singleUse, null, 0);
    }

    public static WatchTicketResult preparing(String assetId, ProcessingState processingState,
                                               int retryAfterSeconds) {
        Objects.requireNonNull(processingState, "processingState must not be null");
        return new WatchTicketResult(assetId, false, null, null,
                Collections.<PlaybackSegment>emptyList(), null, 0L, false,
                processingState, retryAfterSeconds);
    }

    public String assetId() { return assetId; }
    public boolean ready() { return ready; }
    /** Null only for a bounded preparing result. */
    public String token() { return token; }
    /** Direct redemption endpoint; null only for preparing or a legacy response. */
    public URI redemptionUrl() { return redemptionUrl; }
    public List<PlaybackSegment> segments() { return segments; }
    /** Null only for a bounded preparing result. */
    public Optional<Instant> expiresAt() { return Optional.ofNullable(expiresAt); }
    public long expiresInSeconds() { return expiresInSeconds; }
    public boolean singleUse() { return singleUse; }
    public Optional<ProcessingState> processingState() { return Optional.ofNullable(processingState); }
    public int retryAfterSeconds() { return retryAfterSeconds; }

    /** Compatibility alias for the established ticket accessor. */
    public Optional<String> ticket() { return Optional.ofNullable(token); }
    public Optional<URI> optionalRedemptionUrl() { return Optional.ofNullable(redemptionUrl); }

    /** One contributing archive's playback window, in playback order. */
    public static final class PlaybackSegment {
        private final String replayId;
        private final int sequenceIndex;
        private final long startTick;
        private final long endTick;

        public PlaybackSegment(String replayId, int sequenceIndex, long startTick, long endTick) {
            this.replayId = Objects.requireNonNull(replayId, "replayId must not be null");
            this.sequenceIndex = sequenceIndex;
            this.startTick = startTick;
            this.endTick = endTick;
        }

        public String replayId() { return replayId; }
        public int sequenceIndex() { return sequenceIndex; }
        public long startTick() { return startTick; }
        public long endTick() { return endTick; }
    }
}
