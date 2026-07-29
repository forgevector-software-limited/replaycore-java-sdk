/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The request used to create a pending ReplayCore server setup.
 */
public final class ServerSetupRequest {

    private final String name;
    private final String categoryId;
    private final String sourceServerId;

    private ServerSetupRequest(Builder builder) {
        this.name = builder.name;
        this.categoryId = builder.categoryId;
        this.sourceServerId = builder.sourceServerId;
    }

    /**
     * Starts a server setup request.
     *
     * @param name the server display name, 1 to 64 characters
     * @return a new builder
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /** @return the requested display name */
    public String getName() {
        return name;
    }

    /** @return the optional category id, or {@code null} */
    public String getCategoryId() {
        return categoryId;
    }

    /** @return the optional source server id, or {@code null} */
    public String getSourceServerId() {
        return sourceServerId;
    }

    /**
     * Returns the developer-API request object.
     *
     * @return a fresh ordered map
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("name", name);
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        if (sourceServerId != null) {
            body.put("sourceServerId", sourceServerId);
        }
        return body;
    }

    /** Builds an immutable server setup request. */
    public static final class Builder {
        private final String name;
        private String categoryId;
        private String sourceServerId;

        private Builder(String name) {
            this.name = requireValue(name, "name");
            if (this.name.length() > 64) {
                throw new IllegalArgumentException("name must be at most 64 characters");
            }
        }

        /**
         * @param categoryId the category UUID
         * @return this builder
         */
        public Builder categoryId(String categoryId) {
            this.categoryId = requireValue(categoryId, "categoryId");
            return this;
        }

        /**
         * @param sourceServerId a server whose compatible setup should be copied
         * @return this builder
         */
        public Builder sourceServerId(String sourceServerId) {
            this.sourceServerId = requireValue(sourceServerId, "sourceServerId");
            return this;
        }

        /** @return the immutable request */
        public ServerSetupRequest build() {
            return new ServerSetupRequest(this);
        }
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
