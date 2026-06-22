/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A typed, validated set of filters for {@code listReplays}.
 *
 * <p>Every filter here maps to a real query parameter accepted by the
 * {@code GET /v1/api/replays} endpoint. Filters are AND-combined: a replay must
 * satisfy all of them to appear. Build one with {@link #builder()}; an
 * unconfigured query (just {@link Builder#build()}) lists the most recent
 * replays at the default page size.
 *
 * <p>The builder validates bounds up front &mdash; page size 1&ndash;100,
 * duration 0&ndash;86&nbsp;400&nbsp;000&nbsp;ms, the mutually-exclusive
 * {@code player} / {@code playerUuid} pair, and a coherent time range &mdash; so
 * invalid input fails fast in the caller's own JVM rather than after a network
 * round trip. These mirror the server's own validation.
 */
public final class ReplayQuery {

    /** The maximum page size the server accepts. */
    public static final int MAX_PAGE_SIZE = 100;

    /** The largest duration bound the server accepts, in milliseconds (24 hours). */
    public static final long MAX_DURATION_MS = 86_400_000L;

    private final Integer pageSize;
    private final String pageToken;
    private final String search;
    private final String serverId;
    private final String gameMode;
    private final Boolean flagged;
    private final Boolean starred;
    private final String playerUuid;
    private final String player;
    private final Instant startedAfter;
    private final Instant startedBefore;
    private final Long durationMinMs;
    private final Long durationMaxMs;

    private ReplayQuery(Builder b) {
        this.pageSize = b.pageSize;
        this.pageToken = b.pageToken;
        this.search = b.search;
        this.serverId = b.serverId;
        this.gameMode = b.gameMode;
        this.flagged = b.flagged;
        this.starred = b.starred;
        this.playerUuid = b.playerUuid;
        this.player = b.player;
        this.startedAfter = b.startedAfter;
        this.startedBefore = b.startedBefore;
        this.durationMinMs = b.durationMinMs;
        this.durationMaxMs = b.durationMaxMs;
    }

    /**
     * Renders this query as an ordered map of query-parameter name to value,
     * including only the filters that were set. Used internally by the client to
     * build the request URL; exposed for testing and transparency.
     *
     * @return an ordered map of query parameters (never {@code null})
     */
    public Map<String, String> toQueryParameters() {
        Map<String, String> params = new LinkedHashMap<String, String>();
        if (pageSize != null) {
            params.put("page_size", Integer.toString(pageSize));
        }
        if (pageToken != null) {
            params.put("page_token", pageToken);
        }
        if (search != null) {
            params.put("q", search);
        }
        if (serverId != null) {
            params.put("server", serverId);
        }
        if (gameMode != null) {
            params.put("game_mode", gameMode);
        }
        if (flagged != null) {
            params.put("flagged", flagged.toString());
        }
        if (starred != null) {
            params.put("starred", starred.toString());
        }
        if (playerUuid != null) {
            params.put("player_uuid", playerUuid);
        }
        if (player != null) {
            params.put("player", player);
        }
        if (startedAfter != null) {
            params.put("started_after", startedAfter.toString());
        }
        if (startedBefore != null) {
            params.put("started_before", startedBefore.toString());
        }
        if (durationMinMs != null) {
            params.put("duration_min_ms", Long.toString(durationMinMs));
        }
        if (durationMaxMs != null) {
            params.put("duration_max_ms", Long.toString(durationMaxMs));
        }
        return params;
    }

    /**
     * Returns a new, empty query builder.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a query that fetches the next page of a prior result.
     *
     * @param page the page whose successor is wanted; must have a next page
     * @return a builder seeded with that page's cursor
     * @throws IllegalArgumentException if {@code page} has no next page
     */
    public static Builder nextPageOf(ReplayPage page) {
        if (page == null || !page.hasNextPage()) {
            throw new IllegalArgumentException("page has no next page");
        }
        return new Builder().pageToken(page.getNextPageToken().get());
    }

    /**
     * A fluent builder for {@link ReplayQuery} that validates each filter as it is
     * set or at {@link #build()}. Not thread-safe.
     */
    public static final class Builder {
        private Integer pageSize;
        private String pageToken;
        private String search;
        private String serverId;
        private String gameMode;
        private Boolean flagged;
        private Boolean starred;
        private String playerUuid;
        private String player;
        private Instant startedAfter;
        private Instant startedBefore;
        private Long durationMinMs;
        private Long durationMaxMs;

        private Builder() {
        }

        /**
         * Sets the page size (number of replays per page).
         *
         * @param size between 1 and {@value #MAX_PAGE_SIZE} inclusive
         * @return this builder
         * @throws IllegalArgumentException if out of range
         */
        public Builder pageSize(int size) {
            if (size < 1 || size > MAX_PAGE_SIZE) {
                throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
            }
            this.pageSize = size;
            return this;
        }

        /**
         * Sets the opaque cursor returned by a prior page.
         *
         * @param token the next-page token; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder pageToken(String token) {
            this.pageToken = requireNonBlank(token, "pageToken");
            return this;
        }

        /**
         * Sets a free-text search matched (case-insensitively) against the server
         * name, game mode, and server id.
         *
         * @param text the search text; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder search(String text) {
            this.search = requireNonBlank(text, "search");
            return this;
        }

        /**
         * Restricts results to a single server by its id.
         *
         * @param serverId the server UUID; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder serverId(String serverId) {
            this.serverId = requireNonBlank(serverId, "serverId");
            return this;
        }

        /**
         * Restricts results to an exact (case-insensitive) game mode / integration.
         *
         * @param gameMode the game mode; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder gameMode(String gameMode) {
            this.gameMode = requireNonBlank(gameMode, "gameMode");
            return this;
        }

        /**
         * Restricts results to replays that have at least one open flag
         * ({@code true}) or none ({@code false}).
         *
         * @param flagged the flag filter
         * @return this builder
         */
        public Builder flagged(boolean flagged) {
            this.flagged = flagged;
            return this;
        }

        /**
         * Restricts results to starred replays ({@code true}) or unstarred
         * replays ({@code false}).
         *
         * @param starred the starred filter
         * @return this builder
         */
        public Builder starred(boolean starred) {
            this.starred = starred;
            return this;
        }

        /**
         * Restricts results to replays in which a specific player took part,
         * matched exactly by UUID.
         *
         * <p>Mutually exclusive with {@link #player(String)}.
         *
         * @param uuid the player UUID; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder playerUuid(String uuid) {
            this.playerUuid = requireNonBlank(uuid, "playerUuid");
            return this;
        }

        /**
         * Restricts results to replays whose participant names contain the given
         * text (case-insensitive substring).
         *
         * <p>Mutually exclusive with {@link #playerUuid(String)}.
         *
         * @param player the player-name substring; must be non-blank when supplied
         * @return this builder
         * @throws IllegalArgumentException if blank
         */
        public Builder player(String player) {
            this.player = requireNonBlank(player, "player");
            return this;
        }

        /**
         * Restricts results to replays started at or after the given instant.
         *
         * @param instant the lower time bound
         * @return this builder
         */
        public Builder startedAfter(Instant instant) {
            this.startedAfter = instant;
            return this;
        }

        /**
         * Restricts results to replays started strictly before the given instant.
         *
         * @param instant the upper time bound
         * @return this builder
         */
        public Builder startedBefore(Instant instant) {
            this.startedBefore = instant;
            return this;
        }

        /**
         * Restricts results to replays at least this long.
         *
         * @param ms the minimum duration in milliseconds
         *           (0&ndash;{@value #MAX_DURATION_MS})
         * @return this builder
         * @throws IllegalArgumentException if out of range
         */
        public Builder durationMinMs(long ms) {
            checkDuration(ms, "durationMinMs");
            this.durationMinMs = ms;
            return this;
        }

        /**
         * Restricts results to replays at most this long.
         *
         * @param ms the maximum duration in milliseconds
         *           (0&ndash;{@value #MAX_DURATION_MS})
         * @return this builder
         * @throws IllegalArgumentException if out of range
         */
        public Builder durationMaxMs(long ms) {
            checkDuration(ms, "durationMaxMs");
            this.durationMaxMs = ms;
            return this;
        }

        /**
         * Validates cross-field constraints and builds the immutable query.
         *
         * @return a new immutable query
         * @throws IllegalArgumentException if {@code player} and {@code playerUuid}
         *         are both set, if the time range is inverted, or if the duration
         *         range is inverted
         */
        public ReplayQuery build() {
            if (player != null && playerUuid != null) {
                throw new IllegalArgumentException("player and playerUuid are mutually exclusive");
            }
            if (startedAfter != null && startedBefore != null && !startedAfter.isBefore(startedBefore)) {
                throw new IllegalArgumentException("startedAfter must be before startedBefore");
            }
            if (durationMinMs != null && durationMaxMs != null && durationMinMs > durationMaxMs) {
                throw new IllegalArgumentException("durationMinMs must not exceed durationMaxMs");
            }
            return new ReplayQuery(this);
        }

        private static void checkDuration(long ms, String field) {
            if (ms < 0 || ms > MAX_DURATION_MS) {
                throw new IllegalArgumentException(field + " must be between 0 and " + MAX_DURATION_MS);
            }
        }

        private static String requireNonBlank(String value, String field) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return value;
        }
    }
}
