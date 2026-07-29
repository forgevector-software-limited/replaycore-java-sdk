/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
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
import uk.co.forgevector.replaycore.api.model.ApiResponse;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;
import uk.co.forgevector.replaycore.api.model.ServerInstance;
import uk.co.forgevector.replaycore.api.model.ServerSetup;
import uk.co.forgevector.replaycore.api.model.ServerSetupRequest;
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;

/**
 * The synchronous entry point to ReplayCore's public developer API.
 *
 * <p>A client is configured once, with a base URL and an account-scoped API
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
 * <h2>Authentication and account scoping</h2>
 * Every request is sent with {@code Authorization: Bearer <apiKey>}. The API key
 * binds the request to exactly one ReplayCore account on the server side; this client cannot
 * widen that scope, name another account, or reach an admin endpoint. The key is
 * the only secret the SDK holds, and it is never logged or echoed.
 *
 * <h2>Errors</h2>
 * A non-success HTTP status raises a {@link ReplayCoreApiException} (or one of its
 * dedicated subtypes: {@link AuthenticationException},
 * {@link AuthorizationException}, {@link NotFoundException},
 * {@link RateLimitException}). A failure to reach the server raises a
 * {@link ReplayCoreTransportException}. Both extend {@link ReplayCoreException},
 * so a single catch suffices.
 *
 * <h2>Coverage</h2>
 * This client wraps the endpoints ReplayCore exposes to API-key holders today:
 * listing and reading replay metadata, managing connected server setups,
 * writing custom timeline markers, and reading or changing the workspace setup
 * resources exposed by ReplayCore. The generic JSON call keeps new public
 * endpoints accessible without forcing applications to wait for a new typed
 * SDK model.
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
     * Lists replays in the caller's ReplayCore account, newest first, applying the supplied
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
     * @throws RateLimitException      if the account's read rate limit is exceeded
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
        try {
            return ModelMapper.toReplayPage(obj);
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Fetches the metadata for a single replay by id.
     *
     * @param replayId the replay's UUID; must not be {@code null} or blank
     * @return the replay metadata
     * @throws NotFoundException       if no such replay exists in the caller's
     *                                 account (HTTP 404)
     * @throws AuthenticationException if the API key is missing, invalid, revoked
     *                                 or expired (HTTP 401)
     * @throws AuthorizationException  if the key lacks the {@code replays:read}
     *                                 scope (HTTP 403)
     * @throws RateLimitException      if the account's read rate limit is exceeded
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
        try {
            return ModelMapper.toReplayMetadata(obj);
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Lists the Minecraft server instances connected to the caller's ReplayCore
     * account.
     *
     * @return an unmodifiable list of connected server instances
     * @throws AuthenticationException if the API key is missing, invalid, revoked
     *                                 or expired (HTTP 401)
     * @throws AuthorizationException  if the key lacks the {@code servers:read}
     *                                 scope (HTTP 403)
     * @throws RateLimitException      if the account's read rate limit is exceeded
     *                                 (HTTP 429)
     * @throws ReplayCoreApiException  for any other non-success status
     * @throws ReplayCoreTransportException if the server cannot be reached
     * @throws ReplayCoreException     base type for all of the above
     */
    public List<ServerInstance> listServers() throws ReplayCoreException {
        String url = Urls.join(baseUrl, "/v1/api/servers");
        HttpResponse response = send("GET", url, null);
        try {
            return ModelMapper.toServerInstances(parseObject(response));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Creates a pending server setup and reserves its plan slot.
     *
     * @param request the server name and optional placement/template
     * @return the new server id and cleaned name
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ServerSetup createServer(ServerSetupRequest request) throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        ApiResponse response = requestJson("POST", "/v1/api/servers", request.toBody());
        Map<String, Object> body = requireObject(response);
        Object serverId = body.get("serverId");
        Object name = body.get("name");
        if (!(serverId instanceof String) || !(name instanceof String)) {
            throw invalidResponse(new JsonParseException(
                    "server setup response is missing 'serverId' or 'name'"));
        }
        return new ServerSetup((String) serverId, (String) name);
    }

    /**
     * Renames a connected server.
     *
     * @param serverId the connected server UUID
     * @param name     the new display name
     * @return the updated server record
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ServerInstance renameServer(String serverId, String name) throws ReplayCoreException {
        Map<String, Object> body = new java.util.LinkedHashMap<String, Object>();
        body.put("name", requireValue(name, "name"));
        ApiResponse response = requestJson(
                "PATCH",
                "/v1/api/servers/" + Urls.encodePathSegment(requireValue(serverId, "serverId")),
                body);
        try {
            return ModelMapper.toServerInstance(requireObject(response));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Deauthorises a connected server through the reversible compatibility route.
     *
     * @param serverId the connected server UUID
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public void deauthoriseServer(String serverId) throws ReplayCoreException {
        requestJson("DELETE", "/v1/api/servers/"
                + Urls.encodePathSegment(requireValue(serverId, "serverId")), null);
    }

    /**
     * Permanently removes a connected server record.
     *
     * @param serverId the connected server UUID
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public void permanentlyRemoveServer(String serverId) throws ReplayCoreException {
        requestJson("DELETE", "/v1/api/servers/permanent/"
                + Urls.encodePathSegment(requireValue(serverId, "serverId")), null);
    }

    /**
     * Cancels a server setup that has not connected yet.
     *
     * @param serverId the pending server id
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public void cancelServerSetup(String serverId) throws ReplayCoreException {
        requestJson("DELETE", "/v1/api/servers/pending/"
                + Urls.encodePathSegment(requireValue(serverId, "serverId")), null);
    }

    /**
     * Reads one allowlisted workspace setup resource.
     *
     * @param path the setup path without {@code /v1/api/setup/}
     * @return the successful status and immutable JSON response tree
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ApiResponse getSetup(String path) throws ReplayCoreException {
        return requestSetup("GET", path, null);
    }

    /**
     * Calls one allowlisted workspace setup resource.
     *
     * <p>Use GET with {@code setup:read}; POST, PUT, PATCH and DELETE require
     * {@code setup:write}. The server applies the same actor permissions,
     * selected-network access, plan checks and validation as the dashboard.
     *
     * @param method   GET, POST, PUT, PATCH or DELETE
     * @param path     the setup path without {@code /v1/api/setup/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @return the successful status and immutable JSON response tree
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ApiResponse requestSetup(String method, String path, Object jsonBody)
            throws ReplayCoreException {
        String clean = requireValue(path, "path");
        if (clean.startsWith("/") || clean.contains("..") || clean.contains("\\")
                || clean.contains("#") || clean.contains("://")) {
            throw new IllegalArgumentException("path must be a relative setup resource path");
        }
        return requestJson(method, "/v1/api/setup/" + clean, jsonBody);
    }

    /**
     * Calls any JSON endpoint on the configured ReplayCore API origin.
     *
     * <p>This future-proofs integrations while typed convenience methods are
     * added. The path cannot change the configured origin, and normal server-side
     * API-key scopes and tenant isolation still apply.
     *
     * @param method   GET, POST, PUT, PATCH or DELETE
     * @param path     an absolute API path beginning {@code /v1/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @return the successful status and immutable JSON response tree
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ApiResponse requestJson(String method, String path, Object jsonBody)
            throws ReplayCoreException {
        String verb = requireValue(method, "method").toUpperCase(java.util.Locale.ROOT);
        if (!verb.equals("GET") && !verb.equals("POST") && !verb.equals("PUT")
                && !verb.equals("PATCH") && !verb.equals("DELETE")) {
            throw new IllegalArgumentException("method must be GET, POST, PUT, PATCH or DELETE");
        }
        String cleanPath = requireValue(path, "path");
        if (!cleanPath.startsWith("/v1/") || cleanPath.startsWith("//")
                || cleanPath.contains("://") || cleanPath.contains("\\")
                || cleanPath.contains("..") || cleanPath.contains("#")) {
            throw new IllegalArgumentException("path must be a safe absolute /v1/ API path");
        }
        String encodedBody = jsonBody == null ? null : Json.write(jsonBody);
        HttpResponse response = send(verb, Urls.join(baseUrl, cleanPath), encodedBody);
        String raw = response.getBody();
        if (raw == null || raw.trim().isEmpty()) {
            return new ApiResponse(response.getStatusCode(), null);
        }
        try {
            return new ApiResponse(response.getStatusCode(), Json.parse(raw));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
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
     * @throws RateLimitException      if the account's write rate limit is exceeded
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
        try {
            return ModelMapper.toTimelineMarker(obj);
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
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

    private static Map<String, Object> requireObject(ApiResponse response)
            throws ReplayCoreTransportException {
        if (!response.getObject().isPresent()) {
            throw new ReplayCoreTransportException(
                    "ReplayCore returned a successful response that was not a JSON object",
                    new IllegalStateException("expected JSON object"));
        }
        return response.getObject().get();
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static ReplayCoreTransportException invalidResponse(JsonParseException cause) {
        return new ReplayCoreTransportException(
                "could not parse ReplayCore response: " + cause.getMessage(), cause);
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
