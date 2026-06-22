/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.time.Duration;

import uk.co.forgevector.replaycore.api.internal.HttpTransport;
import uk.co.forgevector.replaycore.api.internal.HttpUrlConnectionTransport;

/**
 * Configures and constructs a {@link ReplayCoreClient} (and, via
 * {@link #buildAsync()}, a {@link ReplayCoreAsyncClient}).
 *
 * <p>The only required setting is the API key. Everything else has a sensible
 * default: the production base URL, ten-second connect and thirty-second read
 * timeouts, and a descriptive {@code User-Agent}.
 *
 * <pre>{@code
 * ReplayCoreClient client = ReplayCoreClient.builder()
 *         .apiKey(System.getenv("REPLAYCORE_API_KEY"))
 *         .connectTimeout(Duration.ofSeconds(5))
 *         .build();
 * }</pre>
 *
 * <p>A builder is single-use-friendly but not thread-safe; configure it on one
 * thread, then share the immutable client it produces.
 */
public final class ReplayCoreClientBuilder {

    /** The branded prefix every valid ReplayCore API key carries. */
    static final String API_KEY_PREFIX = "rc_live_";

    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = 30_000;
    private static final String DEFAULT_USER_AGENT = "replaycore-java-sdk/1.0.0";

    private String baseUrl = ReplayCoreClient.DEFAULT_BASE_URL;
    private String apiKey;
    private int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
    private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;
    private String userAgent = DEFAULT_USER_AGENT;
    private HttpTransport transport;

    ReplayCoreClientBuilder() {
    }

    /**
     * Sets the server-owner-scoped API key used to authenticate every request.
     *
     * <p>The key is validated for shape only (it must be non-blank and carry the
     * {@code rc_live_} prefix); whether it is live, revoked or expired is decided
     * by the server. The key is the SDK's only secret and should be supplied from
     * configuration or an environment variable, never hard-coded.
     *
     * @param apiKey the API key, beginning {@code rc_live_}
     * @return this builder
     * @throws IllegalArgumentException if the key is blank or not of the expected
     *                                  shape
     */
    public ReplayCoreClientBuilder apiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("apiKey must not be blank");
        }
        String trimmed = apiKey.trim();
        if (!trimmed.startsWith(API_KEY_PREFIX)) {
            throw new IllegalArgumentException(
                    "apiKey is not a ReplayCore API key (expected the '" + API_KEY_PREFIX + "' prefix)");
        }
        this.apiKey = trimmed;
        return this;
    }

    /**
     * Overrides the API base URL. Defaults to
     * {@link ReplayCoreClient#DEFAULT_BASE_URL}. Useful for pointing the SDK at a
     * staging environment.
     *
     * @param baseUrl an absolute {@code http} or {@code https} URL, no trailing
     *                path required
     * @return this builder
     * @throws IllegalArgumentException if the URL is blank or not http(s)
     */
    public ReplayCoreClientBuilder baseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        String trimmed = baseUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("baseUrl must be an http or https URL");
        }
        // Trim a trailing slash so path joining is unambiguous.
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        this.baseUrl = trimmed;
        return this;
    }

    /**
     * Sets the connection-establishment timeout. Defaults to ten seconds.
     *
     * @param timeout a non-negative duration ({@link Duration#ZERO} means no
     *                timeout)
     * @return this builder
     * @throws IllegalArgumentException if the duration is null or negative
     */
    public ReplayCoreClientBuilder connectTimeout(Duration timeout) {
        this.connectTimeoutMillis = toMillis(timeout, "connectTimeout");
        return this;
    }

    /**
     * Sets the response-read timeout. Defaults to thirty seconds.
     *
     * @param timeout a non-negative duration ({@link Duration#ZERO} means no
     *                timeout)
     * @return this builder
     * @throws IllegalArgumentException if the duration is null or negative
     */
    public ReplayCoreClientBuilder readTimeout(Duration timeout) {
        this.readTimeoutMillis = toMillis(timeout, "readTimeout");
        return this;
    }

    /**
     * Overrides the {@code User-Agent} header. The default identifies this SDK and
     * its version; append your own application name if you wish to be
     * distinguishable in ReplayCore's request logs.
     *
     * @param userAgent the user-agent string; must be non-blank
     * @return this builder
     * @throws IllegalArgumentException if blank
     */
    public ReplayCoreClientBuilder userAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            throw new IllegalArgumentException("userAgent must not be blank");
        }
        this.userAgent = userAgent.trim();
        return this;
    }

    /**
     * Supplies a custom HTTP transport. Intended primarily for testing (an
     * in-memory transport lets unit tests exercise the client without a network)
     * and for advanced callers who need to route requests through their own HTTP
     * stack. When unset, the SDK uses a {@link HttpUrlConnectionTransport} with
     * the configured timeouts.
     *
     * @param transport the transport to use; must not be {@code null}
     * @return this builder
     */
    public ReplayCoreClientBuilder transport(HttpTransport transport) {
        if (transport == null) {
            throw new IllegalArgumentException("transport must not be null");
        }
        this.transport = transport;
        return this;
    }

    /**
     * Builds an immutable, thread-safe synchronous client.
     *
     * @return the configured {@link ReplayCoreClient}
     * @throws IllegalStateException if no API key has been set
     */
    public ReplayCoreClient build() {
        if (apiKey == null) {
            throw new IllegalStateException("apiKey is required");
        }
        HttpTransport t = transport != null
                ? transport
                : new HttpUrlConnectionTransport(connectTimeoutMillis, readTimeoutMillis);
        return new ReplayCoreClient(baseUrl, apiKey, userAgent, t);
    }

    /**
     * Builds an asynchronous client that returns {@link java.util.concurrent.CompletableFuture}s.
     *
     * <p>The async client wraps a synchronous client built from the same
     * configuration and runs each call on an executor (the common fork-join pool
     * by default; supply your own with
     * {@link ReplayCoreAsyncClient#ReplayCoreAsyncClient(ReplayCoreClient, java.util.concurrent.Executor)}).
     *
     * @return the configured {@link ReplayCoreAsyncClient}
     * @throws IllegalStateException if no API key has been set
     */
    public ReplayCoreAsyncClient buildAsync() {
        return new ReplayCoreAsyncClient(build());
    }

    private static int toMillis(Duration timeout, String field) {
        if (timeout == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        if (timeout.isNegative()) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        long millis = timeout.toMillis();
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) millis;
    }
}
