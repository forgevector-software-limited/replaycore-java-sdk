/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A team to add to a replay collection.
 *
 * <p>Used by {@link CollectionRequest}, {@link CollectionUpdateRequest} and
 * {@link CollectionFinalizeRequest}. A team is identified by its key, so sending
 * the same key again updates the existing team. Send a team before, or in the
 * same request as, any participant that names it.
 *
 * <p>The builder validates field bounds locally (key &le; 64 characters, name
 * &le; 80, result &le; 64, placement at least 1) to match the server's own checks.
 */
public final class ScopeTeam {

    private final String key;
    private final String name;
    private final String result;
    private final Integer placement;

    private ScopeTeam(Builder b) {
        this.key = b.key;
        this.name = b.name;
        this.result = b.result;
        this.placement = b.placement;
    }

    /**
     * Renders this team as the JSON object the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("key", key);
        if (name != null) {
            body.put("name", name);
        }
        if (result != null) {
            body.put("result", result);
        }
        if (placement != null) {
            body.put("placement", placement);
        }
        return body;
    }

    /**
     * Returns a new builder for a team.
     *
     * @param key the integration's own key for the team, 1 to 64 characters
     * @return a builder seeded with the key
     * @throws IllegalArgumentException if blank or too long
     */
    public static Builder builder(String key) {
        return new Builder(key);
    }

    /**
     * A fluent builder for {@link ScopeTeam}. Not thread-safe.
     */
    public static final class Builder {
        private final String key;
        private String name;
        private String result;
        private Integer placement;

        private Builder(String key) {
            this.key = Validation.requireText(key, "key", 64);
        }

        /**
         * Sets the team's display name.
         *
         * @param name at most 80 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder name(String name) {
            this.name = Validation.requireText(name, "name", 80);
            return this;
        }

        /**
         * Sets the team's result label (for example {@code "winner"}).
         *
         * @param result at most 64 characters
         * @return this builder
         * @throws IllegalArgumentException if blank or too long
         */
        public Builder result(String result) {
            this.result = Validation.requireText(result, "result", 64);
            return this;
        }

        /**
         * Sets the team's final placement.
         *
         * @param placement 1 or more, where 1 is first place
         * @return this builder
         * @throws IllegalArgumentException if less than 1
         */
        public Builder placement(int placement) {
            if (placement < 1) {
                throw new IllegalArgumentException("placement must be positive");
            }
            this.placement = placement;
            return this;
        }

        /**
         * Builds the immutable team.
         *
         * @return a new immutable team
         */
        public ScopeTeam build() {
            return new ScopeTeam(this);
        }
    }
}
