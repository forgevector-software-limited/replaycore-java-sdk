/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.exception;

/**
 * Raised when ReplayCore returns a non-success HTTP status (4xx or 5xx).
 *
 * <p>The cloud answers errors with an RFC 9457 {@code application/problem+json}
 * body whose machine-readable {@code code} field is the stable contract a caller
 * should branch on (for example {@code "REPLAY_NOT_FOUND"},
 * {@code "INVALID_API_KEY"}, {@code "INSUFFICIENT_SCOPE"},
 * {@code "RATE_LIMITED"}). The numeric {@link #getStatusCode() HTTP status} is
 * also exposed for callers that prefer to switch on the status family.
 *
 * <p>For the convenience of common control flow, the SDK promotes the most
 * frequent conditions to dedicated subtypes:
 * <ul>
 *   <li>{@link AuthenticationException} &mdash; 401 (missing, invalid, revoked or
 *       expired key).</li>
 *   <li>{@link AuthorizationException} &mdash; 403 (the key lacks the scope the
 *       endpoint requires).</li>
 *   <li>{@link NotFoundException} &mdash; 404 (no such replay, or it belongs to a
 *       different tenant and is therefore invisible).</li>
 *   <li>{@link RateLimitException} &mdash; 429 (per-tenant rate limit; carries the
 *       advised retry delay).</li>
 * </ul>
 * Any other status surfaces as a plain {@code ReplayCoreApiException}.
 */
public class ReplayCoreApiException extends ReplayCoreException {

    private static final long serialVersionUID = 1L;

    /** The HTTP status the server returned. */
    private final int statusCode;
    /** The machine-readable error code from the problem body, if any. */
    private final String code;
    /** The human-readable detail from the problem body, if any. */
    private final String detail;

    /**
     * Creates an API exception from a parsed problem detail.
     *
     * @param statusCode the HTTP status returned by the server
     * @param code       the machine-readable {@code code} field from the problem
     *                   body, or {@code null} if the body carried none
     * @param detail     the human-readable {@code detail} field, or {@code null}
     */
    public ReplayCoreApiException(int statusCode, String code, String detail) {
        super(buildMessage(statusCode, code, detail));
        this.statusCode = statusCode;
        this.code = code;
        this.detail = detail;
    }

    private static String buildMessage(int statusCode, String code, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("ReplayCore API error ").append(statusCode);
        if (code != null && !code.isEmpty()) {
            sb.append(" [").append(code).append(']');
        }
        if (detail != null && !detail.isEmpty()) {
            sb.append(": ").append(detail);
        }
        return sb.toString();
    }

    /**
     * Returns the HTTP status code the server returned (for example 404).
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the stable machine-readable error code from the problem body (for
     * example {@code "REPLAY_NOT_FOUND"}). Prefer branching on this over the
     * human-readable detail, which is not part of the API contract.
     *
     * @return the error code, or {@code null} if the response carried none
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the human-readable explanation from the problem body. Intended for
     * logging and diagnostics, not for programmatic branching.
     *
     * @return the detail message, or {@code null} if the response carried none
     */
    public String getDetail() {
        return detail;
    }
}
