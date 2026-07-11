/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The scopes a ReplayCore API key may be granted.
 *
 * <p>A key carries a fixed subset of these, chosen when it is issued from the
 * ReplayCore dashboard. The SDK cannot widen a key's scopes; if a call fails with
 * {@link uk.co.forgevector.replaycore.api.exception.AuthorizationException}, the
 * remedy is to issue a key with the missing scope.
 *
 * <p>{@link #REPLAYS_READ}, {@link #REPLAYS_WRITE} and {@link #SERVERS_READ}
 * are consumed by the current public API. {@link #ANALYTICS_READ} is reserved
 * for future public analytics endpoints.
 */
public enum ApiScope {

    /** Read replay listings and metadata. Wire value {@code "replays:read"}. */
    REPLAYS_READ("replays:read"),

    /** Write timeline markers onto replays. Wire value {@code "replays:write"}. */
    REPLAYS_WRITE("replays:write"),

    /** Read connected-server metadata. Wire value {@code "servers:read"}. */
    SERVERS_READ("servers:read"),

    /**
     * Read aggregate analytics. Wire value {@code "analytics:read"}.
     *
     * <p>Reserved: no key-authed endpoint consumes this scope yet.
     */
    ANALYTICS_READ("analytics:read");

    private final String wireValue;

    ApiScope(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire value the cloud uses for this scope.
     *
     * @return the scope token (for example {@code "replays:read"})
     */
    public String wireValue() {
        return wireValue;
    }
}
