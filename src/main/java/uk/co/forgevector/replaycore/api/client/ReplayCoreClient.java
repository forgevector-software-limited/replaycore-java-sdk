/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import uk.co.forgevector.replaycore.api.exception.AuthenticationException;
import uk.co.forgevector.replaycore.api.exception.AuthorizationException;
import uk.co.forgevector.replaycore.api.exception.NotFoundException;
import uk.co.forgevector.replaycore.api.exception.RateLimitException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreApiException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreException;
import uk.co.forgevector.replaycore.api.exception.ReplayCoreTransportException;
import uk.co.forgevector.replaycore.api.internal.HttpRequest;
import uk.co.forgevector.replaycore.api.internal.HttpResponse;
import uk.co.forgevector.replaycore.api.internal.HttpTransport;
import uk.co.forgevector.replaycore.api.internal.Json;
import uk.co.forgevector.replaycore.api.internal.JsonParseException;
import uk.co.forgevector.replaycore.api.internal.ModelMapper;
import uk.co.forgevector.replaycore.api.internal.Urls;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;

/**
 * The synchronous entry point to ReplayCore's public developer API.
 *
 * <p>A client is configured once, with a base URL and a server-owner-scoped API
 * key, and is then safe to share and call concurrently from any number of
 * threads. Build one with {@link #builder()}:
 *
 * <pre>{@code
 * ReplayCoreClient client = ReplayCoreClient.builder()
 *         .apiKey("rc_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
 *         .build();
 *
 * ReplayPage page = client.listReplays(
 *         ReplayQuery.builder().pageSize(25).gameMode("bedwars").build());
 * for (ReplayMetadata replay : page.getResults()) {
 *     System.out.println(replay.getId() + " ready=" + replay.isReady());
 * }
 * }</pre>
 *
 * <h2>Authentication and tenant scoping</h2>
 * Every request is sent with {@code Authorization: Bearer <apiKey>}. The API key
 * binds the request to exactly one tenant on the server side; this client cannot
 * widen that scope, name another tenant, or reach an admin endpoint. The key is
 * the only secret the SDK holds, and it is never logged or echoed.
 *
 * <h2>Errors</h2>
 * A non-success HTTP status raises a {@link ReplayCoreApiException} (or one of its
 * dedicated subtypes &mdash; {@link AuthenticationException},
 * {@link AuthorizationException}, {@link NotFoundException},
 * {@link RateLimitException}). A failure to reach the server raises a
 * {@link ReplayCoreTransportException}. Both extend {@link ReplayCoreException},
 * so a single catch suffices.
 *
 * <h2>Coverage</h2>
 * This client wraps the endpoints ReplayCore exposes to API-key holders today:
 * listing and reading replay metadata, and writing custom timeline markers.
 * Downloads, server listing and analytics are not part of the key-authed surface
 * at the time of writing; see the SDK documentation for the current scope.
 */
public final class ReplayCoreClient {

    /** The default production base URL for the ReplayCore public API. */
    public static final String DEFAULT_BASE_URL = "https://api.replaycore.com";

    private final String baseUrl;
    private final String apiKey;
    private final String userAgent;
    private final HttpTransport transport;

    ReplayCoreClient(String baseUrl, String apiKey, String userAgent, HttpTransport transport) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.userAgent = userAgent;
        this.transport = transport;
    }

    /**
     * Returns a new builder for configuring a client.
     *
     * @return a fresh {@link ReplayCoreClientBuilder}
     */
    public static ReplayCoreClientBuilder builder() {
        return new ReplayCoreClientBuilder();
    }

    /**
     * Lists replays in the caller's tenant, newest first, applying the supplied
     * filters.
     *
     * <p>Results are paginated with opaque cursors. When the returned page reports
     * {@link ReplayPage#hasNextPage()}, fetch the next page with
     * {@code listReplays(ReplayQuery.nextPageOf(page).build())}.
     *
     * @param query the filters and page settings; pass
     *              {@code ReplayQuery.builder().build()} for an unfiltered listing.
     *              Must not be {@code null}.
     * @return one page of replay metadata
     * @throws AuthenticationException if the API key is missing, invalid, revoked
     *                                 or expired (HTTP 401)
     * @throws AuthorizationException  if the key lacks the {@code replays:read}
     *                                 scope (HTTP 403)
     * @throws RateLimitException      if the tenant's read rate limit is exceeded
     *                                 (HTTP 429)
     * @throws ReplayCoreApiException  for any other non-success status
     * @throws ReplayCoreTransportException if the server cannot be reached
     * @throws ReplayCoreException     base type for all of the above
     */
    public ReplayPage listReplays(ReplayQuery query) throws ReplayCoreException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        String url = Urls.withQuery(
                Urls.join(baseUrl, "/v1/api/replays"),
                query.toQueryParameters());
        HttpResponse response = send("GET", url, null);
        Map<String, Object> obj = parseObject(response);
        return ModelMapper.toReplayPage(obj);
    }

    /**
     * Fetches the metadata for a single replay by id.
     *
     * @param replayId the replay's UUID; must not be {@code null} or blank
     * @return the replay metadata
     * @throws NotFoundException       if no such replay exists in the caller's
     *                                 tenant (HTTP 404)
     * @throws AuthenticationException if the API key is missing, invalid, revoked
     *                                 or expired (HTTP 401)
     * @throws AuthorizationException  if the key lacks the {@code replays:read}
     *                                 scope (HTTP 403)
     * @throws RateLimitException      if the tenant's read rate limit is exceeded
     *                                 (HTTP 429)
     * @throws ReplayCoreApiException  for any other non-success status
     * @throws ReplayCoreTransportException if the server cannot be reached
     * @throws ReplayCoreException     base type for all of the above
     */
    public ReplayMetadata getReplay(String replayId) throws ReplayCoreException {
        if (replayId == null || replayId.trim().isEmpty()) {
            throw new IllegalArgumentException("replayId must not be blank");
        }
        String url = Urls.join(baseUrl, "/v1/api/replays/" + Urls.encodePathSegment(replayId));
        HttpResponse response = send("GET", url, null);
        Map<String, Object> obj = parseObject(response);
        return ModelMapper.toReplayMetadata(obj);
    }

    /**
     * Creates a custom timeline marker on a replay.
     *
     * <p>The request targets either an existing replay or a server's currently
     * active recording (see {@link TimelineEventRequest}). Requires an API key
     * with the {@code replays:write} scope.
     *
     * @param request the marker to create; must not be {@code null}
     * @return the created marker, as resolved and persisted by the server
     * @throws NotFoundException       if the target replay does not exist, or the
     *                                 server has no active recording (HTTP 404)
     * @throws AuthenticationException if the API key is missing, invalid, revoked
     *                                 or expired (HTTP 401)
     * @throws AuthorizationException  if the key lacks the {@code replays:write}
     *                                 scope (HTTP 403)
     * @throws RateLimitException      if the tenant's write rate limit is exceeded
     *                                 (HTTP 429)
     * @throws ReplayCoreApiException  for any other non-success status (for
     *                                 example {@code TOO_MANY_MARKERS}, HTTP 409)
     * @throws ReplayCoreTransportException if the server cannot be reached
     * @throws ReplayCoreException     base type for all of the above
     */
    public TimelineMarker createTimelineMarker(TimelineEventRequest request) throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String url = Urls.join(baseUrl, "/v1/api/timeline-events");
        String body = Json.write(request.toBody());
        HttpResponse response = send("POST", url, body);
        Map<String, Object> obj = parseObject(response);
        return ModelMapper.toTimelineMarker(obj);
    }

    private HttpResponse send(String method, String url, String body) throws ReplayCoreException {
        HttpRequest request = buildRequest(method, url, body);
        HttpResponse response;
        try {
            response = transport.execute(request);
        } catch (IOException e) {
            throw new ReplayCoreTransportException("request to ReplayCore failed: " + e.getMessage(), e);
        }
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            return response;
        }
        throw toApiException(response);
    }

    private HttpRequest buildRequest(String method, String url, String body) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Accept", "application/json");
        headers.put("User-Agent", userAgent);
        if (body != null) {
            headers.put("Content-Type", "application/json");
        }
        return new HttpRequest(method, url, headers, body);
    }

    private Map<String, Object> parseObject(HttpResponse response) throws ReplayCoreException {
        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            throw new ReplayCoreTransportException(
                    "ReplayCore returned an empty body for a successful response",
                    new IllegalStateException("empty body"));
        }
        try {
            return Json.parseObject(body);
        } catch (JsonParseException e) {
            throw new ReplayCoreTransportException("could not parse ReplayCore response: " + e.getMessage(), e);
        }
    }

    private ReplayCoreApiException toApiException(HttpResponse response) {
        int status = response.getStatusCode();
        String code = null;
        String detail = null;
        String body = response.getBody();
        if (body != null && !body.isEmpty()) {
            try {
                Map<String, Object> problem = Json.parseObject(body);
                Object c = problem.get("code");
                Object d = problem.get("detail");
                if (c instanceof String) {
                    code = (String) c;
                }
                if (d instanceof String) {
                    detail = (String) d;
                }
            } catch (JsonParseException ignored) {
                // A non-problem-json error body (for example an HTML 502 from an
                // intermediary) leaves code/detail null; the status still drives
                // the right exception type.
            }
        }
        switch (status) {
            case 401:
                return new AuthenticationException(status, code, detail);
            case 403:
                return new AuthorizationException(status, code, detail);
            case 404:
                return new NotFoundException(status, code, detail);
            case 429:
                return new RateLimitException(status, code, detail, parseRetryAfter(response));
            default:
                return new ReplayCoreApiException(status, code, detail);
        }
    }

    private static Duration parseRetryAfter(HttpResponse response) {
        String header = response.getHeader("Retry-After");
        if (header == null) {
            return null;
        }
        try {
            long seconds = Long.parseLong(header.trim());
            return seconds >= 0 ? Duration.ofSeconds(seconds) : null;
        } catch (NumberFormatException e) {
            // The server sends whole seconds; a non-numeric value (an HTTP-date
            // form) is simply not parsed rather than failing the call.
            return null;
        }
    }
}
