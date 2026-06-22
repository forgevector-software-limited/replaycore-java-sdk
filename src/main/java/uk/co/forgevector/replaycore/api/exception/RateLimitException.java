/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

import java.time.Duration;

/**
 * Raised for HTTP 429 responses: the per-tenant rate limit for the endpoint has
 * been exceeded.
 *
 * <p>ReplayCore enforces read and write limits separately, keyed on the tenant
 * the API key resolves to. When the server can tell when the window resets it
 * sends a {@code Retry-After} header; the SDK parses it into {@link #getRetryAfter()}
 * so callers can back off cleanly rather than hammering a closed window.
 */
public class RateLimitException extends ReplayCoreApiException {

    private static final long serialVersionUID = 1L;

    /** The advised back-off from the {@code Retry-After} header, or {@code null}. */
    private final Duration retryAfter;

    /**
     * Creates a rate-limit exception from a 429 problem detail.
     *
     * @param statusCode the HTTP status (always 429 in practice)
     * @param code       the machine-readable error code, or {@code null}
     * @param detail     the human-readable detail, or {@code null}
     * @param retryAfter the advised back-off taken from the {@code Retry-After}
     *                   header, or {@code null} if the server sent none
     */
    public RateLimitException(int statusCode, String code, String detail, Duration retryAfter) {
        super(statusCode, code, detail);
        this.retryAfter = retryAfter;
    }

    /**
     * Returns the advised back-off delay before retrying, parsed from the
     * {@code Retry-After} response header.
     *
     * @return the retry delay, or {@code null} if the server did not advise one
     */
    public Duration getRetryAfter() {
        return retryAfter;
    }
}
