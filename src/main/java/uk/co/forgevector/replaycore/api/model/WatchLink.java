/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A durable browser watch link for a replay asset.
 *
 * Immutable. The link does not expire on its own; it stops working when the asset is revoked or
 * becomes unavailable.
 */
public final class WatchLink {

    private final String token;
    private final Instant expiresAt;
    private final String watchUrl;

    /**
     * Constructs a watch link. Intended for internal deserialisation and tests.
     *
     * @param token the link token
     * @param expiresAt when the link expires; absent for a link that does not expire
     * @param watchUrl the URL to open in a browser
     */
    public WatchLink(String token, Instant expiresAt, String watchUrl) {
        this.token = token != null ? token : "";
        this.expiresAt = expiresAt;
        this.watchUrl = watchUrl != null ? watchUrl : "";
    }

    /**
     * Returns the link token.
     *
     * Treat it as a credential.
     *
     * @return the link token; never {@code null}
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns when the link expires; absent for a link that does not expire.
     *
     * @return when the link expires; absent for a link that does not expire, or an empty optional
     */
    public Optional<Instant> getExpiresAt() {
        return Optional.ofNullable(expiresAt);
    }

    /**
     * Returns the URL to open in a browser.
     *
     * @return the URL to open in a browser; never {@code null}
     */
    public String getWatchUrl() {
        return watchUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WatchLink)) {
            return false;
        }
        WatchLink that = (WatchLink) o;
        return Objects.equals(token, that.token)
                && Objects.equals(expiresAt, that.expiresAt)
                && Objects.equals(watchUrl, that.watchUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, expiresAt, watchUrl);
    }

    @Override
    public String toString() {
        return "WatchLink{watchUrl=" + watchUrl + '}';
    }
}
