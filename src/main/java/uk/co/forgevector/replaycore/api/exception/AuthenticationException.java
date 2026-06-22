/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Raised for HTTP 401 responses: the request carried no usable credentials, or
 * the presented API key is malformed, unknown, revoked, or expired.
 *
 * <p>ReplayCore deliberately returns the same {@code INVALID_API_KEY} code for
 * malformed, unknown, revoked, and expired keys so that an attacker cannot tell
 * which case they hit. A {@code UNAUTHENTICATED} code instead means no
 * {@code Authorization: Bearer} header was sent at all.
 */
public class AuthenticationException extends ReplayCoreApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an authentication exception from a 401 problem detail.
     *
     * @param statusCode the HTTP status (always 401 in practice)
     * @param code       the machine-readable error code, or {@code null}
     * @param detail     the human-readable detail, or {@code null}
     */
    public AuthenticationException(int statusCode, String code, String detail) {
        super(statusCode, code, detail);
    }
}
