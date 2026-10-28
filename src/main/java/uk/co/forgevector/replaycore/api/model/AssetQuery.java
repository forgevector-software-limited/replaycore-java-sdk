/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A typed, validated set of filters for {@code listCollectionAssets}.
 *
 * Every filter maps to a query parameter accepted by {@code GET
 * /v1/replay-collections/{id}/assets}. Filters are AND-combined. Build one with {@link
 * #builder()}; an unconfigured query lists the collection's assets at the default page size.
 */
public final class AssetQuery {

    /** The maximum page size the server accepts. */
    public static final int MAX_PAGE_SIZE = 100;

    private final Integer pageSize;
    private final String pageToken;
    private final AssetKind assetKind;
    private final EventKind eventKind;
    private final ProcessingState processingState;
    private final ReleaseState releaseState;
    private final CollectionVisibility visibility;
    private final AssetRelation viewerRelation;
    private final String playerUuid;
    private final Boolean playable;

    private AssetQuery(Builder b) {
        this.pageSize = b.pageSize;
        this.pageToken = b.pageToken;
        this.assetKind = b.assetKind;
        this.eventKind = b.eventKind;
        this.processingState = b.processingState;
        this.releaseState = b.releaseState;
        this.visibility = b.visibility;
        this.viewerRelation = b.viewerRelation;
        this.playerUuid = b.playerUuid;
        this.playable = b.playable;
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
        if (assetKind != null) {
            params.put("asset_kind", assetKind.wireValue());
        }
        if (eventKind != null) {
            params.put("event_kind", eventKind.wireValue());
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
        if (viewerRelation != null) {
            params.put("viewer_relation", viewerRelation.wireValue());
        }
        if (playerUuid != null) {
            params.put("player_uuid", playerUuid);
        }
        if (playable != null) {
            params.put("playable", playable.toString());
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
     * A fluent builder for {@link AssetQuery} that validates each filter as it is set. Not
     * thread-safe.
     */
    public static final class Builder {
        private Integer pageSize;
        private String pageToken;
        private AssetKind assetKind;
        private EventKind eventKind;
        private ProcessingState processingState;
        private ReleaseState releaseState;
        private CollectionVisibility visibility;
        private AssetRelation viewerRelation;
        private String playerUuid;
        private Boolean playable;

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
         * Restricts results to assets of one kind.
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
         * Restricts results to assets for one event kind.
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
         * Restricts results to assets in one processing state.
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
         * Restricts results to assets in one embargo state.
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
         * Restricts results to assets with one visibility.
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
         * Restricts results to assets where the player has one relation.
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
         * Restricts results to assets that involve one player.
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
         * Restricts results to assets whose footage is playable or not playable.
         *
         * @param playable the playable filter
         * @return this builder
         */
        public Builder playable(boolean playable) {
            this.playable = playable;
            return this;
        }

        /**
         * Validates cross-field constraints and builds the immutable query.
         *
         * @return a new immutable query
         */
        public AssetQuery build() {
            return new AssetQuery(this);
        }
    }
}
