/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Raised for HTTP 403 responses: the API key is valid but does not grant the
 * scope the endpoint requires (for example calling a write endpoint with a key
 * that only holds {@code replays:read}).
 *
 * <p>The remedy is to issue a key with the missing scope from the ReplayCore
 * panel; it is never something the SDK can work around at runtime.
 */
public class AuthorizationException extends ReplayCoreApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an authorization exception from a 403 problem detail.
     *
     * @param statusCode the HTTP status (always 403 in practice)
     * @param code       the machine-readable error code, or {@code null}
     * @param detail     the human-readable detail, or {@code null}
     */
    public AuthorizationException(int statusCode, String code, String detail) {
        super(statusCode, code, detail);
    }
}
