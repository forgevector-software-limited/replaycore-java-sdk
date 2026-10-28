/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A typed, validated set of filters for {@code listCollections}.
 *
 * Every filter maps to a query parameter accepted by {@code GET /v1/replay-collections}. Filters
 * are AND-combined. The asset, event and player filters match collections that have at least one
 * asset satisfying every one of them. Build one with {@link #builder()}; an unconfigured query
 * lists the most recently started collections at the default page size.
 */
public final class CollectionQuery {

    /** The maximum page size the server accepts. */
    public static final int MAX_PAGE_SIZE = 100;

    private final Integer pageSize;
    private final String pageToken;
    private final String category;
    private final String mode;
    private final String competitionId;
    private final String region;
    private final String externalMatchId;
    private final String partyEventId;
    private final String serverId;
    private final String playerUuid;
    private final Instant startedAfter;
    private final Instant startedBefore;
    private final ProcessingState processingState;
    private final ReleaseState releaseState;
    private final CollectionVisibility visibility;
    private final AssetKind assetKind;
    private final EventKind eventKind;
    private final AssetRelation viewerRelation;
    private final Integer teamSize;
    private final Boolean ranked;
    private final Boolean tournament;
    private final Boolean playable;
    private final Map<String, String> customFacets;

    private CollectionQuery(Builder b) {
        this.pageSize = b.pageSize;
        this.pageToken = b.pageToken;
        this.category = b.category;
        this.mode = b.mode;
        this.competitionId = b.competitionId;
        this.region = b.region;
        this.externalMatchId = b.externalMatchId;
        this.partyEventId = b.partyEventId;
        this.serverId = b.serverId;
        this.playerUuid = b.playerUuid;
        this.startedAfter = b.startedAfter;
        this.startedBefore = b.startedBefore;
        this.processingState = b.processingState;
        this.releaseState = b.releaseState;
        this.visibility = b.visibility;
        this.assetKind = b.assetKind;
        this.eventKind = b.eventKind;
        this.viewerRelation = b.viewerRelation;
        this.teamSize = b.teamSize;
        this.ranked = b.ranked;
        this.tournament = b.tournament;
        this.playable = b.playable;
        this.customFacets = new LinkedHashMap<String, String>(b.customFacets);
    }

    /**
     * Renders this query as an ordered map of query-parameter name to value, including only the
     * filters that were set. Used internally by the client to build the request URL; exposed for
     * testing and transparency.
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
        if (category != null) {
            params.put("category", category);
        }
        if (mode != null) {
            params.put("mode", mode);
        }
        if (competitionId != null) {
            params.put("competition_id", competitionId);
        }
        if (region != null) {
            params.put("region", region);
        }
        if (externalMatchId != null) {
            params.put("external_match_id", externalMatchId);
        }
        if (partyEventId != null) {
            params.put("party_event_id", partyEventId);
        }
        if (serverId != null) {
            params.put("server_id", serverId);
        }
        if (playerUuid != null) {
            params.put("player_uuid", playerUuid);
        }
        if (startedAfter != null) {
            params.put("from", startedAfter.toString());
        }
        if (startedBefore != null) {
            params.put("to", startedBefore.toString());
        }
        if (processingState != null) {
            params.put("processing_state", processingState.wireValue());
        }
        if (releaseState != null) {
            params.put("release_state", releaseState.wireValue());
        }
        if (visibility != null) {
            params.put("visibility", visibility.wireValue());
        }
        if (assetKind != null) {
            params.put("asset_kind", assetKind.wireValue());
        }
        if (eventKind != null) {
            params.put("event_kind", eventKind.wireValue());
        }
        if (viewerRelation != null) {
            params.put("viewer_relation", viewerRelation.wireValue());
        }
        if (teamSize != null) {
            params.put("team_size", Integer.toString(teamSize));
        }
        if (ranked != null) {
            params.put("ranked", ranked.toString());
        }
        if (tournament != null) {
            params.put("tournament", tournament.toString());
        }
        if (playable != null) {
            params.put("playable", playable.toString());
        }
        for (Map.Entry<String, String> facet : customFacets.entrySet()) {
            params.put("facet." + facet.getKey(), facet.getValue());
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
     * Returns a query that fetches the next page of a prior result. Re-apply the filters of the
     * original query to the returned builder, since only the cursor is carried over.
     *
     * @param page the page whose successor is wanted; must have a next page
     * @return a builder seeded with that page's cursor
     * @throws IllegalArgumentException if {@code page} has no next page
     */
    public static Builder nextPageOf(CatalogPage<?> page) {
        if (page == null || !page.hasNextPage()) {
            throw new IllegalArgumentException("page has no next page");
        }
        return new Builder().pageToken(page.getNextPageToken().get());
    }

    /**
     * A fluent builder for {@link CollectionQuery} that validates each filter as it is set. Not
     * thread-safe.
     */
    public static final class Builder {
        private Integer pageSize;
        private String pageToken;
        private String category;
        private String mode;
        private String competitionId;
        private String region;
        private String externalMatchId;
        private String partyEventId;
        private String serverId;
        private String playerUuid;
        private Instant startedAfter;
        private Instant startedBefore;
        private ProcessingState processingState;
        private ReleaseState releaseState;
        private CollectionVisibility visibility;
        private AssetKind assetKind;
        private EventKind eventKind;
        private AssetRelation viewerRelation;
        private Integer teamSize;
        private Boolean ranked;
        private Boolean tournament;
        private Boolean playable;
        private final Map<String, String> customFacets = new LinkedHashMap<String, String>();

        private Builder() {
        }

        /**
         * Sets the page size (number of results per page).
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
            this.pageToken = Validation.requireNonBlank(token, "pageToken");
            return this;
        }

        /**
         * Restricts results to collections in one category.
         *
         * @param category the category; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder category(String category) {
            this.category = Validation.requireText(category, "category", 256);
            return this;
        }

        /**
         * Restricts results to collections of one game mode.
         *
         * @param mode the game mode; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder mode(String mode) {
            this.mode = Validation.requireText(mode, "mode", 256);
            return this;
        }

        /**
         * Restricts results to collections of one competition.
         *
         * @param competitionId the competition id; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder competitionId(String competitionId) {
            this.competitionId = Validation.requireText(competitionId, "competitionId", 256);
            return this;
        }

        /**
         * Restricts results to collections played in one region.
         *
         * @param region the region; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder region(String region) {
            this.region = Validation.requireText(region, "region", 256);
            return this;
        }

        /**
         * Restricts results to the collection with one integration match id.
         *
         * @param externalMatchId the integration's own match id; must be non-blank and at most 256
         *         characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder externalMatchId(String externalMatchId) {
            this.externalMatchId = Validation.requireText(externalMatchId, "externalMatchId", 256);
            return this;
        }

        /**
         * Restricts results to collections grouped under one parent event.
         *
         * @param partyEventId the parent event id; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder partyEventId(String partyEventId) {
            this.partyEventId = Validation.requireText(partyEventId, "partyEventId", 256);
            return this;
        }

        /**
         * Restricts results to collections recorded by one server.
         *
         * @param serverId the server id; must be a UUID
         * @return this builder
         * @throws IllegalArgumentException if not a UUID
         */
        public Builder serverId(String serverId) {
            this.serverId = Validation.requireUuid(serverId, "serverId");
            return this;
        }

        /**
         * Restricts results to collections with an asset that involves one player.
         *
         * @param playerUuid the player UUID; must be a UUID
         * @return this builder
         * @throws IllegalArgumentException if not a UUID
         */
        public Builder playerUuid(String playerUuid) {
            this.playerUuid = Validation.requireUuid(playerUuid, "playerUuid");
            return this;
        }

        /**
         * Restricts results to collections started at or after an instant.
         *
         * @param startedAfter the lower time bound
         * @return this builder
         */
        public Builder startedAfter(Instant startedAfter) {
            this.startedAfter = startedAfter;
            return this;
        }

        /**
         * Restricts results to collections started before an instant.
         *
         * @param startedBefore the upper time bound
         * @return this builder
         */
        public Builder startedBefore(Instant startedBefore) {
            this.startedBefore = startedBefore;
            return this;
        }

        /**
         * Restricts results to collections in one processing state.
         *
         * @param processingState the processing state; must not be {@link ProcessingState#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder processingState(ProcessingState processingState) {
            if (processingState == null || processingState == ProcessingState.UNKNOWN) {
                throw new IllegalArgumentException("processingState must be a known ProcessingState");
            }
            this.processingState = processingState;
            return this;
        }

        /**
         * Restricts results to collections in one embargo state.
         *
         * @param releaseState the release state; must not be {@link ReleaseState#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder releaseState(ReleaseState releaseState) {
            if (releaseState == null || releaseState == ReleaseState.UNKNOWN) {
                throw new IllegalArgumentException("releaseState must be a known ReleaseState");
            }
            this.releaseState = releaseState;
            return this;
        }

        /**
         * Restricts results to collections with one visibility.
         *
         * @param visibility the visibility; must not be {@link CollectionVisibility#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder visibility(CollectionVisibility visibility) {
            if (visibility == null || visibility == CollectionVisibility.UNKNOWN) {
                throw new IllegalArgumentException("visibility must be a known CollectionVisibility");
            }
            this.visibility = visibility;
            return this;
        }

        /**
         * Restricts results to collections that have an asset of one kind.
         *
         * @param assetKind the asset kind; must not be {@link AssetKind#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder assetKind(AssetKind assetKind) {
            if (assetKind == null || assetKind == AssetKind.UNKNOWN) {
                throw new IllegalArgumentException("assetKind must be a known AssetKind");
            }
            this.assetKind = assetKind;
            return this;
        }

        /**
         * Restricts results to collections that have an asset for one event kind.
         *
         * @param eventKind the event kind; must not be {@link EventKind#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder eventKind(EventKind eventKind) {
            if (eventKind == null || eventKind == EventKind.UNKNOWN) {
                throw new IllegalArgumentException("eventKind must be a known EventKind");
            }
            this.eventKind = eventKind;
            return this;
        }

        /**
         * Restricts results to collections where the player has one relation to an asset.
         *
         * @param viewerRelation the relation; must not be {@link AssetRelation#UNKNOWN}
         * @return this builder
         * @throws IllegalArgumentException if null or unknown
         */
        public Builder viewerRelation(AssetRelation viewerRelation) {
            if (viewerRelation == null || viewerRelation == AssetRelation.UNKNOWN) {
                throw new IllegalArgumentException("viewerRelation must be a known AssetRelation");
            }
            this.viewerRelation = viewerRelation;
            return this;
        }

        /**
         * Restricts results to collections with one team size.
         *
         * @param teamSize the number of players per team; must be positive
         * @return this builder
         * @throws IllegalArgumentException if not positive
         */
        public Builder teamSize(int teamSize) {
            if (teamSize < 1) {
                throw new IllegalArgumentException("teamSize must be positive");
            }
            this.teamSize = teamSize;
            return this;
        }

        /**
         * Restricts results to ranked or unranked collections.
         *
         * @param ranked whether the match was ranked
         * @return this builder
         */
        public Builder ranked(boolean ranked) {
            this.ranked = ranked;
            return this;
        }

        /**
         * Restricts results to tournament or non-tournament collections.
         *
         * @param tournament whether the match was a tournament match
         * @return this builder
         */
        public Builder tournament(boolean tournament) {
            this.tournament = tournament;
            return this;
        }

        /**
         * Restricts results to collections whose footage is playable or not playable.
         *
         * @param playable the playable filter
         * @return this builder
         */
        public Builder playable(boolean playable) {
            this.playable = playable;
            return this;
        }

        /**
         * Restricts results to collections whose metadata carries a value for a
         * namespaced custom key. The same key may be set only once.
         *
         * @param key   a namespaced metadata key such as {@code acme:region}
         * @param value the value to match; must be non-blank and at most 256 characters
         * @return this builder
         * @throws IllegalArgumentException if the key is not well formed or the value is blank
         */
        public Builder customFacet(String key, String value) {
            this.customFacets.put(Validation.requireMetadataKey(key, "key"),
                    Validation.requireText(value, "value", 256));
            return this;
        }

        /**
         * Validates cross-field constraints and builds the immutable query.
         *
         * @return a new immutable query
         */
        public CollectionQuery build() {
            return new CollectionQuery(this);
        }
    }
}
