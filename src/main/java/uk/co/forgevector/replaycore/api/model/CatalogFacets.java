/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Per-dimension counts across the catalogue of replay collections.
 *
 * Immutable. Each dimension is counted independently, over the rows the caller may see: a held or
 * revoked row never inflates a count.
 */
public final class CatalogFacets {

    private final List<FacetCount> category;
    private final List<FacetCount> mode;
    private final List<FacetCount> assetKind;
    private final List<FacetCount> eventKind;
    private final List<FacetCount> viewerRelation;
    private final List<FacetCount> serverId;
    private final List<FacetCount> region;
    private final List<FacetCount> teamSize;
    private final List<FacetCount> ranked;
    private final List<FacetCount> tournament;
    private final List<FacetCount> competitionId;

    private CatalogFacets(Builder b) {
        this.category = b.category != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.category))
                : Collections.<FacetCount>emptyList();
        this.mode = b.mode != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.mode))
                : Collections.<FacetCount>emptyList();
        this.assetKind = b.assetKind != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.assetKind))
                : Collections.<FacetCount>emptyList();
        this.eventKind = b.eventKind != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.eventKind))
                : Collections.<FacetCount>emptyList();
        this.viewerRelation = b.viewerRelation != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.viewerRelation))
                : Collections.<FacetCount>emptyList();
        this.serverId = b.serverId != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.serverId))
                : Collections.<FacetCount>emptyList();
        this.region = b.region != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.region))
                : Collections.<FacetCount>emptyList();
        this.teamSize = b.teamSize != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.teamSize))
                : Collections.<FacetCount>emptyList();
        this.ranked = b.ranked != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.ranked))
                : Collections.<FacetCount>emptyList();
        this.tournament = b.tournament != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.tournament))
                : Collections.<FacetCount>emptyList();
        this.competitionId = b.competitionId != null
                ? Collections.unmodifiableList(new ArrayList<FacetCount>(b.competitionId))
                : Collections.<FacetCount>emptyList();
    }

    /**
     * Returns the counts by category.
     *
     * @return the counts by category; never {@code null}
     */
    public List<FacetCount> getCategory() {
        return category;
    }

    /**
     * Returns the counts by game mode.
     *
     * @return the counts by game mode; never {@code null}
     */
    public List<FacetCount> getMode() {
        return mode;
    }

    /**
     * Returns the counts by asset kind.
     *
     * @return the counts by asset kind; never {@code null}
     */
    public List<FacetCount> getAssetKind() {
        return assetKind;
    }

    /**
     * Returns the counts by event kind.
     *
     * @return the counts by event kind; never {@code null}
     */
    public List<FacetCount> getEventKind() {
        return eventKind;
    }

    /**
     * Returns the counts by viewer relation.
     *
     * @return the counts by viewer relation; never {@code null}
     */
    public List<FacetCount> getViewerRelation() {
        return viewerRelation;
    }

    /**
     * Returns the counts by recording server.
     *
     * @return the counts by recording server; never {@code null}
     */
    public List<FacetCount> getServerId() {
        return serverId;
    }

    /**
     * Returns the counts by region.
     *
     * @return the counts by region; never {@code null}
     */
    public List<FacetCount> getRegion() {
        return region;
    }

    /**
     * Returns the counts by team size.
     *
     * @return the counts by team size; never {@code null}
     */
    public List<FacetCount> getTeamSize() {
        return teamSize;
    }

    /**
     * Returns the counts by ranked flag.
     *
     * @return the counts by ranked flag; never {@code null}
     */
    public List<FacetCount> getRanked() {
        return ranked;
    }

    /**
     * Returns the counts by tournament flag.
     *
     * @return the counts by tournament flag; never {@code null}
     */
    public List<FacetCount> getTournament() {
        return tournament;
    }

    /**
     * Returns the counts by competition.
     *
     * @return the counts by competition; never {@code null}
     */
    public List<FacetCount> getCompetitionId() {
        return competitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CatalogFacets)) {
            return false;
        }
        CatalogFacets that = (CatalogFacets) o;
        return Objects.equals(category, that.category)
                && Objects.equals(mode, that.mode)
                && Objects.equals(assetKind, that.assetKind)
                && Objects.equals(eventKind, that.eventKind)
                && Objects.equals(viewerRelation, that.viewerRelation)
                && Objects.equals(serverId, that.serverId)
                && Objects.equals(region, that.region)
                && Objects.equals(teamSize, that.teamSize)
                && Objects.equals(ranked, that.ranked)
                && Objects.equals(tournament, that.tournament)
                && Objects.equals(competitionId, that.competitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, mode, assetKind, eventKind, viewerRelation, serverId, region, teamSize, ranked, tournament, competitionId);
    }

    @Override
    public String toString() {
        return "CatalogFacets{category=" + category
                + ", mode=" + mode + '}';
    }

    /**
     * Returns a new builder for assembling a {@link CatalogFacets}. Used internally for
     * deserialisation and by tests building fixtures.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A mutable builder for {@link CatalogFacets}. Not thread-safe. Every setter returns {@code
     * this} for chaining; {@link #build()} produces an immutable instance.
     */
    public static final class Builder {
        private List<FacetCount> category;
        private List<FacetCount> mode;
        private List<FacetCount> assetKind;
        private List<FacetCount> eventKind;
        private List<FacetCount> viewerRelation;
        private List<FacetCount> serverId;
        private List<FacetCount> region;
        private List<FacetCount> teamSize;
        private List<FacetCount> ranked;
        private List<FacetCount> tournament;
        private List<FacetCount> competitionId;

        private Builder() {
        }

        /**
         * Sets the counts by category.
         *
         * @param v the counts by category
         * @return this builder
         */
        public Builder category(List<FacetCount> v) { this.category = v; return this; }

        /**
         * Sets the counts by game mode.
         *
         * @param v the counts by game mode
         * @return this builder
         */
        public Builder mode(List<FacetCount> v) { this.mode = v; return this; }

        /**
         * Sets the counts by asset kind.
         *
         * @param v the counts by asset kind
         * @return this builder
         */
        public Builder assetKind(List<FacetCount> v) { this.assetKind = v; return this; }

        /**
         * Sets the counts by event kind.
         *
         * @param v the counts by event kind
         * @return this builder
         */
        public Builder eventKind(List<FacetCount> v) { this.eventKind = v; return this; }

        /**
         * Sets the counts by viewer relation.
         *
         * @param v the counts by viewer relation
         * @return this builder
         */
        public Builder viewerRelation(List<FacetCount> v) { this.viewerRelation = v; return this; }

        /**
         * Sets the counts by recording server.
         *
         * @param v the counts by recording server
         * @return this builder
         */
        public Builder serverId(List<FacetCount> v) { this.serverId = v; return this; }

        /**
         * Sets the counts by region.
         *
         * @param v the counts by region
         * @return this builder
         */
        public Builder region(List<FacetCount> v) { this.region = v; return this; }

        /**
         * Sets the counts by team size.
         *
         * @param v the counts by team size
         * @return this builder
         */
        public Builder teamSize(List<FacetCount> v) { this.teamSize = v; return this; }

        /**
         * Sets the counts by ranked flag.
         *
         * @param v the counts by ranked flag
         * @return this builder
         */
        public Builder ranked(List<FacetCount> v) { this.ranked = v; return this; }

        /**
         * Sets the counts by tournament flag.
         *
         * @param v the counts by tournament flag
         * @return this builder
         */
        public Builder tournament(List<FacetCount> v) { this.tournament = v; return this; }

        /**
         * Sets the counts by competition.
         *
         * @param v the counts by competition
         * @return this builder
         */
        public Builder competitionId(List<FacetCount> v) { this.competitionId = v; return this; }

        /**
         * Builds the immutable {@link CatalogFacets}.
         *
         * @return a new immutable instance
         */
        public CatalogFacets build() {
            return new CatalogFacets(this);
        }
    }
}
