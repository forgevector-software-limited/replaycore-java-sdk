/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Result of minting a durable browser watch link for one replay asset. */
public final class WatchLinkResult {
    private final String assetId;
    private final String token;
    private final URI watchUrl;
    private final Instant expiresAt;

    public WatchLinkResult(String assetId, String token, URI watchUrl, Instant expiresAt) {
        this.assetId = Objects.requireNonNull(assetId, "assetId must not be null");
        this.token = Objects.requireNonNull(token, "token must not be null");
        this.watchUrl = Objects.requireNonNull(watchUrl, "watchUrl must not be null");
        this.expiresAt = expiresAt;
    }

    public String assetId() { return assetId; }
    /** The capability the link carries. Anyone holding it can watch, so keep it out of logs. */
    public String token() { return token; }
    /** The page to send a viewer to. */
    public URI watchUrl() { return watchUrl; }
    /** Empty for a link that does not expire, which is every link the cloud mints today. */
    public Optional<Instant> expiresAt() { return Optional.ofNullable(expiresAt); }
}
