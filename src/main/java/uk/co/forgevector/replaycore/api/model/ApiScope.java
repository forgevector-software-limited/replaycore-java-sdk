/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

/**
 * The scopes a ReplayCore API key may be granted.
 *
 * <p>A key carries a fixed subset of these, chosen when it is issued from the
 * ReplayCore panel. The SDK cannot widen a key's scopes; if a call fails with
 * {@link uk.co.forgevector.replaycore.api.exception.AuthorizationException}, the
 * remedy is to issue a key with the missing scope.
 *
 * <p>Only {@link #REPLAYS_READ} and {@link #REPLAYS_WRITE} are consumed by an
 * endpoint today. {@link #SERVERS_READ} and {@link #ANALYTICS_READ} are
 * recognised and storable but currently have no key-authed endpoint; they are
 * declared here so a key minted for them maps cleanly once those endpoints ship.
 */
public enum ApiScope {

    /** Read replay listings and metadata. Wire value {@code "replays:read"}. */
    REPLAYS_READ("replays:read"),

    /** Write timeline markers onto replays. Wire value {@code "replays:write"}. */
    REPLAYS_WRITE("replays:write"),

    /**
     * Read connected-server metadata. Wire value {@code "servers:read"}.
     *
     * <p>Reserved: no key-authed endpoint consumes this scope yet.
     */
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
