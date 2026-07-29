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
 * <p>Management scopes are checked together with the key creator's current
 * workspace permissions. The SDK cannot bypass or replace server-side access
 * control.
 */
public enum ApiScope {

    /** Read replay listings and metadata. Wire value {@code "replays:read"}. */
    REPLAYS_READ("replays:read"),

    /** Write timeline markers onto replays. Wire value {@code "replays:write"}. */
    REPLAYS_WRITE("replays:write"),

    /** Read connected-server metadata. Wire value {@code "servers:read"}. */
    SERVERS_READ("servers:read"),

    /** Create, rename and remove server setups. */
    SERVERS_WRITE("servers:write"),

    /** Inspect allowlisted workspace setup resources. */
    SETUP_READ("setup:read"),

    /** Change allowlisted workspace setup resources. Owner-only to grant. */
    SETUP_WRITE("setup:write"),

    /** Read aggregate analytics. */
    ANALYTICS_READ("analytics:read"),

    /** Read Network Portal configuration and media metadata. */
    PORTALS_READ("portals:read"),

    /** Manage Network Portal revisions and media. */
    PORTALS_WRITE("portals:write"),

    /** Drive recording lifecycle operations. */
    RECORDINGS_WRITE("recordings:write"),

    /** Create and update replay collections. */
    COLLECTIONS_WRITE("collections:write"),

    /** Create and finalise replay assets. */
    CLIPS_WRITE("clips:write"),

    /** Read collections, assets and player replay history. */
    CATALOG_READ("catalog:read"),

    /** Hold, release and revoke embargoed collections. */
    RELEASE_WRITE("release:write"),

    /** Issue replay-asset watch tickets and links. */
    PLAYBACK_ISSUE("playback:issue"),

    /** View held content when the governing release policy permits it. */
    STAFF_BYPASS("staff:bypass"),

    /** Administrative actions with no narrower capability. */
    ADMIN("admin");

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
