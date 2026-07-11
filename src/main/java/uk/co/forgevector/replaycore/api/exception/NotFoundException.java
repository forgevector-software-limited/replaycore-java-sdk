/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Raised for HTTP 404 responses: the requested resource does not exist within the
 * caller's ReplayCore account.
 *
 * <p>Because every keyed request is account-scoped, a replay that belongs to a
 * different account is indistinguishable from one that was never created &mdash;
 * both yield this exception with code {@code REPLAY_NOT_FOUND}. This is by design:
 * it prevents a caller from probing for the existence of other accounts' replays.
 */
public class NotFoundException extends ReplayCoreApiException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a not-found exception from a 404 problem detail.
     *
     * @param statusCode the HTTP status (always 404 in practice)
     * @param code       the machine-readable error code, or {@code null}
     * @param detail     the human-readable detail, or {@code null}
     */
    public NotFoundException(int statusCode, String code, String detail) {
        super(statusCode, code, detail);
    }
}
