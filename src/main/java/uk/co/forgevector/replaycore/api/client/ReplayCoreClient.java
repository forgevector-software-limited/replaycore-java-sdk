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
import uk.co.forgevector.replaycore.api.internal.Headers;
import uk.co.forgevector.replaycore.api.internal.HttpRequest;
import uk.co.forgevector.replaycore.api.internal.HttpResponse;
import uk.co.forgevector.replaycore.api.internal.HttpTransport;
import uk.co.forgevector.replaycore.api.internal.Json;
import uk.co.forgevector.replaycore.api.internal.JsonParseException;
import uk.co.forgevector.replaycore.api.internal.ModelMapper;
import uk.co.forgevector.replaycore.api.internal.Urls;
import uk.co.forgevector.replaycore.api.internal.Validation;
import uk.co.forgevector.replaycore.api.model.ApiResponse;
import uk.co.forgevector.replaycore.api.model.AssetFinalizeRequest;
import uk.co.forgevector.replaycore.api.model.AssetQuery;
import uk.co.forgevector.replaycore.api.model.AssetRequest;
import uk.co.forgevector.replaycore.api.model.BatchEntryResult;
import uk.co.forgevector.replaycore.api.model.CatalogFacets;
import uk.co.forgevector.replaycore.api.model.CatalogPage;
import uk.co.forgevector.replaycore.api.model.CollectionFinalizeRequest;
import uk.co.forgevector.replaycore.api.model.CollectionQuery;
import uk.co.forgevector.replaycore.api.model.CollectionRequest;
import uk.co.forgevector.replaycore.api.model.CollectionUpdateRequest;
import uk.co.forgevector.replaycore.api.model.PlayerReplayQuery;
import uk.co.forgevector.replaycore.api.model.ReplayAsset;
import uk.co.forgevector.replaycore.api.model.ReplayCollection;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;
import uk.co.forgevector.replaycore.api.model.ScopeClipEntry;
import uk.co.forgevector.replaycore.api.model.ScopeMarker;
import uk.co.forgevector.replaycore.api.model.ScopeMarkerEntry;
import uk.co.forgevector.replaycore.api.model.ScopeOperation;
import uk.co.forgevector.replaycore.api.model.ServerInstance;
import uk.co.forgevector.replaycore.api.model.ServerSetup;
import uk.co.forgevector.replaycore.api.model.ServerSetupRequest;
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;
import uk.co.forgevector.replaycore.api.model.WatchLink;
import uk.co.forgevector.replaycore.api.model.WatchTicket;
import uk.co.forgevector.replaycore.api.model.WatchTicketRedemption;
import uk.co.forgevector.replaycore.api.model.WebhookDeliveryReplay;

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
 * writing custom timeline markers, reading or changing the workspace setup
 * resources exposed by ReplayCore, and the network integration surface: opening,
 * updating, finalising and embargoing replay collections, creating their assets,
 * reading the catalogue, and issuing watch tickets and watch links. The generic
 * JSON call keeps new public endpoints accessible without forcing applications to
 * wait for a new typed SDK model.
 *
 * <h2>Idempotency</h2>
 * Every call that creates or changes a collection or asset requires an idempotency
 * key, which the client sends as the {@code Idempotency-Key} header. The key comes
 * from the caller, on the request object or as an explicit argument, and the client
 * never invents one: a key generated per attempt would defeat the point. Reuse the
 * same key when retrying the same request; the server then returns the original
 * outcome instead of applying it twice.
 */
public final class ReplayCoreClient {

    /** The default production base URL for the ReplayCore public API. */
    public static final String DEFAULT_BASE_URL = "https://api.replaycore.com";

    /** The most entries a bulk lookup may name across match ids and collection ids. */
    public static final int MAX_BULK_LOOKUP = 100;

    /** The most entries one batch write may carry. */
    public static final int MAX_BATCH_ENTRIES = 256;

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
        return requestJson(method, path, jsonBody, null);
    }

    /**
     * Calls any JSON endpoint on the configured ReplayCore API origin, adding
     * request headers.
     *
     * <p>The headers are for protocol headers an endpoint reads, chiefly
     * {@code Idempotency-Key}, which makes a mutating call safe to retry, and
     * {@code If-Match}, which makes an update conditional on the version the caller
     * last read. The client owns the headers that identify the caller and describe
     * the request, so {@code Authorization}, {@code Host}, {@code User-Agent},
     * {@code Accept} and {@code Content-Type} cannot be supplied, in any letter
     * case. Names must be valid HTTP tokens, and neither names nor values may
     * contain CR, LF or NUL. The path rules of
     * {@link #requestJson(String, String, Object)} apply unchanged.
     *
     * @param method   GET, POST, PUT, PATCH or DELETE
     * @param path     an absolute API path beginning {@code /v1/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @param headers  extra request headers, or {@code null} for none
     * @return the successful status and immutable JSON response tree
     * @throws IllegalArgumentException if the method, the path or a header is not acceptable
     * @throws ReplayCoreException      if ReplayCore refuses or cannot complete the request
     */
    public ApiResponse requestJson(String method, String path, Object jsonBody, Map<String, String> headers)
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
        Map<String, String> extra = Headers.validate(headers);
        String encodedBody = jsonBody == null ? null : Json.write(jsonBody);
        HttpResponse response = send(verb, Urls.join(baseUrl, cleanPath), encodedBody, extra);
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

    /**
     * Opens a replay collection, the logical window over a recording that an
     * integration begins at the start of a match.
     *
     * <p>Requires an API key with the {@code collections:write} scope. The request
     * carries the idempotency key, which the client sends as the
     * {@code Idempotency-Key} header: a resend with the same key returns the
     * collection the first call created.
     *
     * @param request the collection to open, with its idempotency key
     * @return the stored collection
     * @throws AuthorizationException if the key lacks the {@code collections:write}
     *                                scope (HTTP 403)
     * @throws ReplayCoreApiException for any other non-success status (for example
     *                                {@code INVALID_REQUEST}, HTTP 400)
     * @throws ReplayCoreException    base type for all failures
     */
    public ReplayCollection createCollection(CollectionRequest request) throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return toCollection(postJson("/v1/replay-collections", request.toBody(), request.getIdempotencyKey()));
    }

    /**
     * Updates an open replay collection: players who joined or left, new teams and
     * extra metadata.
     *
     * <p>Requires an API key with the {@code collections:write} scope and sends the
     * request's idempotency key as the {@code Idempotency-Key} header.
     *
     * @param collectionId the collection UUID
     * @param request      the update, with its idempotency key
     * @return the operation outcome
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ScopeOperation updateCollection(String collectionId, CollectionUpdateRequest request)
            throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return toOperation(postJson(collectionPath(collectionId), request.toBody(), request.getIdempotencyKey()));
    }

    /**
     * Finalises a replay collection, ending the match's scope.
     *
     * <p>Requires an API key with the {@code collections:write} scope and sends the
     * request's idempotency key as the {@code Idempotency-Key} header. The
     * collection moves to {@code finalizing}, not {@code ready}; poll
     * {@link #getCollection(String)} for its processing state.
     *
     * @param collectionId the collection UUID
     * @param request      the finalisation, with its idempotency key
     * @return the operation outcome, including the full-match asset id
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ScopeOperation finalizeCollection(String collectionId, CollectionFinalizeRequest request)
            throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return toOperation(postJson(collectionPath(collectionId) + "/finalize", request.toBody(),
                request.getIdempotencyKey()));
    }

    /**
     * Holds a replay collection, embargoing it and every asset cut from it.
     *
     * <p>Requires an API key with the {@code release:write} scope. The call has no
     * body; the client sends the idempotency key as the {@code Idempotency-Key}
     * header.
     *
     * @param collectionId   the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return the updated collection
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayCollection holdCollection(String collectionId, String idempotencyKey)
            throws ReplayCoreException {
        return toCollection(postJson(collectionPath(collectionId) + "/hold", null, idempotencyKey));
    }

    /**
     * Releases a replay collection manually, ahead of its policy's own trigger.
     *
     * <p>Requires an API key with the {@code release:write} scope. The call has no
     * body; the client sends the idempotency key as the {@code Idempotency-Key}
     * header.
     *
     * @param collectionId   the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return the updated collection
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayCollection releaseCollection(String collectionId, String idempotencyKey)
            throws ReplayCoreException {
        return toCollection(postJson(collectionPath(collectionId) + "/release", null, idempotencyKey));
    }

    /**
     * Revokes a replay collection. Revoking is permanent: nothing can move a
     * revoked collection or its assets out of that state.
     *
     * <p>Requires an API key with the {@code release:write} scope. The call has no
     * body; the client sends the idempotency key as the {@code Idempotency-Key}
     * header.
     *
     * @param collectionId   the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return the updated collection
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayCollection revokeCollection(String collectionId, String idempotencyKey)
            throws ReplayCoreException {
        return toCollection(postJson(collectionPath(collectionId) + "/revoke", null, idempotencyKey));
    }

    /**
     * Lists replay collections, newest started first, applying the supplied filters.
     *
     * <p>Requires an API key with the {@code catalog:read} scope. A held or revoked
     * collection the key may not see is never returned.
     *
     * @param query the filters and page settings; must not be {@code null}
     * @return one page of collections
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public CatalogPage<ReplayCollection> listCollections(CollectionQuery query) throws ReplayCoreException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        String url = Urls.withQuery(Urls.join(baseUrl, "/v1/replay-collections"), query.toQueryParameters());
        try {
            return ModelMapper.toCollectionPage(parseObject(send("GET", url, null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Fetches one replay collection with its roster and playback flags.
     *
     * @param collectionId the collection UUID
     * @return the collection
     * @throws NotFoundException if the collection does not exist or is not visible
     *                           to the key (HTTP 404)
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayCollection getCollection(String collectionId) throws ReplayCoreException {
        return getCollection(collectionId, null);
    }

    /**
     * Fetches one replay collection, resolving a player's relation to it.
     *
     * @param collectionId the collection UUID
     * @param playerUuid   a player UUID whose relation to the collection's assets is
     *                     returned as {@link ReplayCollection#getViewerRelation()}, or
     *                     {@code null} for none
     * @return the collection
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayCollection getCollection(String collectionId, String playerUuid) throws ReplayCoreException {
        String url = Urls.join(baseUrl, collectionPath(collectionId));
        if (playerUuid != null) {
            Map<String, String> params = new java.util.LinkedHashMap<String, String>();
            params.put("player_uuid", Validation.requireUuid(playerUuid, "playerUuid"));
            url = Urls.withQuery(url, params);
        }
        return toCollection(parseObject(send("GET", url, null)));
    }

    /**
     * Lists the assets of a replay collection.
     *
     * <p>Requires an API key with the {@code catalog:read} scope.
     *
     * @param collectionId the collection UUID
     * @param query        the filters and page settings; must not be {@code null}
     * @return one page of assets
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public CatalogPage<ReplayAsset> listCollectionAssets(String collectionId, AssetQuery query)
            throws ReplayCoreException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        String url = Urls.withQuery(Urls.join(baseUrl, collectionPath(collectionId) + "/assets"),
                query.toQueryParameters());
        try {
            return ModelMapper.toAssetPage(parseObject(send("GET", url, null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Lists the timeline markers written against a replay collection.
     *
     * <p>Requires an API key with the {@code catalog:read} scope. The list is not
     * paginated.
     *
     * @param collectionId the collection UUID
     * @return the markers in timeline order
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public List<ScopeMarker> listCollectionTimelineEvents(String collectionId) throws ReplayCoreException {
        String url = Urls.join(baseUrl, collectionPath(collectionId) + "/timeline-events");
        try {
            return ModelMapper.toScopeMarkers(parseObject(send("GET", url, null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Counts the catalogue's collections along each facet dimension.
     *
     * <p>Requires an API key with the {@code catalog:read} scope.
     *
     * @return the counts per dimension
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public CatalogFacets getCollectionFacets() throws ReplayCoreException {
        String url = Urls.join(baseUrl, "/v1/replay-collections/facets");
        try {
            return ModelMapper.toCatalogFacets(parseObject(send("GET", url, null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Looks up many collections at once, by the integration's match ids, by
     * collection ids, or both.
     *
     * <p>This is a read, so it needs only the {@code catalog:read} scope. A
     * collection that does not exist or is not visible to the key is simply absent
     * from the result.
     *
     * @param externalMatchIds integration match ids; may be {@code null}
     * @param collectionIds    collection UUIDs; may be {@code null}
     * @return the collections found, with rosters and playback flags
     * @throws IllegalArgumentException if both lists are empty or together hold more
     *                                  than {@value #MAX_BULK_LOOKUP} entries
     * @throws ReplayCoreException      if ReplayCore refuses or cannot complete the request
     */
    public List<ReplayCollection> bulkLookupCollections(List<String> externalMatchIds,
                                                         List<String> collectionIds)
            throws ReplayCoreException {
        List<Object> matches = new java.util.ArrayList<Object>();
        if (externalMatchIds != null) {
            for (String id : externalMatchIds) {
                matches.add(Validation.requireText(id, "externalMatchId", 200));
            }
        }
        List<Object> ids = new java.util.ArrayList<Object>();
        if (collectionIds != null) {
            for (String id : collectionIds) {
                ids.add(Validation.requireUuid(id, "collectionId"));
            }
        }
        if (matches.isEmpty() && ids.isEmpty()) {
            throw new IllegalArgumentException("supply at least one external match id or collection id");
        }
        if (matches.size() + ids.size() > MAX_BULK_LOOKUP) {
            throw new IllegalArgumentException("a bulk lookup may name at most " + MAX_BULK_LOOKUP + " entries");
        }
        Map<String, Object> body = new java.util.LinkedHashMap<String, Object>();
        if (!matches.isEmpty()) {
            body.put("external_match_ids", matches);
        }
        if (!ids.isEmpty()) {
            body.put("collection_ids", ids);
        }
        try {
            return ModelMapper.toReplayCollections(postJson("/v1/replay-collections/bulk-lookup", body, null));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Creates a clip or highlight from a replay collection.
     *
     * <p>Requires an API key with the {@code clips:write} scope and sends the
     * request's idempotency key as the {@code Idempotency-Key} header, so a resend
     * never creates a second asset for the same moment.
     *
     * @param collectionId the collection UUID
     * @param request      the asset to create, with its idempotency key
     * @return the operation outcome, including the new asset id
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ScopeOperation createAsset(String collectionId, AssetRequest request) throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return toOperation(postJson(collectionPath(collectionId) + "/assets", request.toBody(),
                request.getIdempotencyKey()));
    }

    /**
     * Reports that a replay asset's media has finished processing, or has failed.
     *
     * <p>Requires an API key with the {@code clips:write} scope and sends the
     * request's idempotency key as the {@code Idempotency-Key} header.
     *
     * @param assetId the asset UUID
     * @param request the report, with its idempotency key
     * @return the operation outcome
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ScopeOperation finalizeAsset(String assetId, AssetFinalizeRequest request) throws ReplayCoreException {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        return toOperation(postJson(assetPath(assetId) + "/finalize", request.toBody(),
                request.getIdempotencyKey()));
    }

    /**
     * Creates many event clips in one call.
     *
     * <p>Requires an API key with the {@code clips:write} scope. The batch as a
     * whole carries one idempotency key, sent as the {@code Idempotency-Key}
     * header; each entry also carries its own client event id, which is what keeps
     * a resend from duplicating a clip. The call succeeds even when some entries
     * are refused: inspect each {@link BatchEntryResult}.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @param entries        1 to {@value #MAX_BATCH_ENTRIES} clips
     * @return one result per entry, in request order
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public List<BatchEntryResult> createAssetsBatch(String idempotencyKey, List<ScopeClipEntry> entries)
            throws ReplayCoreException {
        List<Object> bodies = new java.util.ArrayList<Object>();
        if (entries != null) {
            for (ScopeClipEntry entry : entries) {
                bodies.add(java.util.Objects.requireNonNull(entry, "entry").toBody());
            }
        }
        return postBatch("/v1/replay-assets/batch", idempotencyKey, bodies);
    }

    /**
     * Creates many collection timeline markers in one call.
     *
     * <p>Requires an API key with the {@code collections:write} scope. The batch as
     * a whole carries one idempotency key, sent as the {@code Idempotency-Key}
     * header; each entry also carries its own client event id, which is what keeps
     * a resend from duplicating a marker. The call succeeds even when some entries
     * are refused: inspect each {@link BatchEntryResult}.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @param entries        1 to {@value #MAX_BATCH_ENTRIES} markers
     * @return one result per entry, in request order
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public List<BatchEntryResult> createTimelineEventsBatch(String idempotencyKey,
                                                             List<ScopeMarkerEntry> entries)
            throws ReplayCoreException {
        List<Object> bodies = new java.util.ArrayList<Object>();
        if (entries != null) {
            for (ScopeMarkerEntry entry : entries) {
                bodies.add(java.util.Objects.requireNonNull(entry, "entry").toBody());
            }
        }
        return postBatch("/v1/timeline-events/batch", idempotencyKey, bodies);
    }

    /**
     * Fetches one replay asset.
     *
     * <p>Requires an API key with the {@code catalog:read} scope. The asset carries
     * its player relationships.
     *
     * @param assetId the asset UUID
     * @return the asset
     * @throws NotFoundException if the asset does not exist or is not visible to the
     *                           key (HTTP 404)
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public ReplayAsset getAsset(String assetId) throws ReplayCoreException {
        try {
            return ModelMapper.toReplayAsset(parseObject(send("GET", Urls.join(baseUrl, assetPath(assetId)), null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Lists the replay assets a player appears in, newest first.
     *
     * <p>Requires an API key with the {@code catalog:read} scope. Each asset names
     * its full participant set, and carries a watch link when it is released and
     * public or unlisted.
     *
     * @param playerUuid the player UUID
     * @param query      the filters and page settings; must not be {@code null}
     * @return one page of assets
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public CatalogPage<ReplayAsset> listPlayerReplays(String playerUuid, PlayerReplayQuery query)
            throws ReplayCoreException {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }
        String path = "/v1/players/" + Urls.encodePathSegment(Validation.requireUuid(playerUuid, "playerUuid"))
                + "/replays";
        String url = Urls.withQuery(Urls.join(baseUrl, path), query.toQueryParameters());
        try {
            return ModelMapper.toAssetPage(parseObject(send("GET", url, null)));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Issues a single-use watch ticket for a replay asset.
     *
     * <p>Requires an API key with the {@code playback:issue} scope. The call needs no
     * idempotency key. When the asset's media is not ready the server answers
     * HTTP 202 and the result reports {@link WatchTicket#isPreparing()}; repeat the
     * call after {@link WatchTicket#getRetryAfterSeconds()}.
     *
     * @param assetId          the asset UUID
     * @param viewerPlayerUuid the UUID of the player who will watch, used for the
     *                         participant check, or {@code null}
     * @return the ticket, or a not-ready notice
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public WatchTicket issueWatchTicket(String assetId, String viewerPlayerUuid) throws ReplayCoreException {
        try {
            return ModelMapper.toWatchTicket(postJson(assetPath(assetId) + "/watch-ticket",
                    viewerBody(viewerPlayerUuid, null), null));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Redeems a watch ticket for short-lived segment URLs.
     *
     * <p>Requires an API key with the {@code catalog:read} scope; the ticket itself
     * is what authorises the redemption. A ticket redeems once. The full
     * authorisation decision is re-run against fresh state, so a revoked or newly
     * held asset is refused.
     *
     * @param assetId          the asset UUID
     * @param ticket           the ticket secret returned by {@link #issueWatchTicket}
     * @param viewerPlayerUuid the UUID of the player who is watching, or {@code null}
     * @return the segments to play
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public WatchTicketRedemption redeemWatchTicket(String assetId, String ticket, String viewerPlayerUuid)
            throws ReplayCoreException {
        Map<String, Object> body = viewerBody(viewerPlayerUuid, requireValue(ticket, "ticket"));
        try {
            return ModelMapper.toWatchTicketRedemption(
                    postJson(assetPath(assetId) + "/watch-ticket/redeem", body, null));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Mints a durable browser watch link for a replay asset.
     *
     * <p>Requires an API key with the {@code playback:issue} scope. Hosted watch
     * links are a paid-plan feature; a free plan receives HTTP 403
     * {@code FORBIDDEN_TIER}. The call needs no idempotency key and has no body.
     *
     * @param assetId the asset UUID
     * @return the link
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public WatchLink createWatchLink(String assetId) throws ReplayCoreException {
        try {
            return ModelMapper.toWatchLink(postJson(assetPath(assetId) + "/watch-link", null, null));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    /**
     * Replays a webhook delivery that did not succeed.
     *
     * <p>Requires an API key with the {@code collections:write} scope. A delivery
     * that already succeeded is refused with HTTP 409
     * {@code DELIVERY_NOT_REPLAYABLE}. The call needs no idempotency key and has no
     * body.
     *
     * @param deliveryId the delivery UUID
     * @return the delivery status after the replay
     * @throws ReplayCoreException if ReplayCore refuses or cannot complete the request
     */
    public WebhookDeliveryReplay replayWebhookDelivery(String deliveryId) throws ReplayCoreException {
        String path = "/v1/webhooks/deliveries/"
                + Urls.encodePathSegment(Validation.requireUuid(deliveryId, "deliveryId")) + "/replay";
        try {
            return ModelMapper.toWebhookDeliveryReplay(postJson(path, null, null));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    private static String collectionPath(String collectionId) {
        return "/v1/replay-collections/" + Urls.encodePathSegment(Validation.requireUuid(collectionId, "collectionId"));
    }

    private static String assetPath(String assetId) {
        return "/v1/replay-assets/" + Urls.encodePathSegment(Validation.requireUuid(assetId, "assetId"));
    }

    private static Map<String, Object> viewerBody(String viewerPlayerUuid, String ticket) {
        Map<String, Object> body = new java.util.LinkedHashMap<String, Object>();
        if (ticket != null) {
            body.put("ticket", ticket);
        }
        if (viewerPlayerUuid != null) {
            body.put("viewer_player_uuid", Validation.requireUuid(viewerPlayerUuid, "viewerPlayerUuid"));
        }
        return body.isEmpty() ? null : body;
    }

    private Map<String, Object> postJson(String path, Map<String, Object> body, String idempotencyKey)
            throws ReplayCoreException {
        Map<String, String> headers = new java.util.LinkedHashMap<String, String>();
        if (idempotencyKey != null) {
            headers.put(Headers.IDEMPOTENCY_KEY, Validation.requireIdempotencyKey(idempotencyKey));
        }
        String encoded = body == null ? null : Json.write(body);
        return parseObject(send("POST", Urls.join(baseUrl, path), encoded, headers));
    }

    private List<BatchEntryResult> postBatch(String path, String idempotencyKey, List<Object> entries)
            throws ReplayCoreException {
        Validation.requireIdempotencyKey(idempotencyKey);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("entries must not be empty");
        }
        if (entries.size() > MAX_BATCH_ENTRIES) {
            throw new IllegalArgumentException("entries must not exceed " + MAX_BATCH_ENTRIES);
        }
        Map<String, Object> body = new java.util.LinkedHashMap<String, Object>();
        body.put("entries", entries);
        try {
            return ModelMapper.toBatchResults(postJson(path, body, idempotencyKey));
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    private static ReplayCollection toCollection(Map<String, Object> obj) throws ReplayCoreTransportException {
        try {
            return ModelMapper.toReplayCollection(obj);
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    private static ScopeOperation toOperation(Map<String, Object> obj) throws ReplayCoreTransportException {
        try {
            return ModelMapper.toScopeOperation(obj);
        } catch (JsonParseException e) {
            throw invalidResponse(e);
        }
    }

    private HttpResponse send(String method, String url, String body) throws ReplayCoreException {
        return send(method, url, body, java.util.Collections.<String, String>emptyMap());
    }

    private HttpResponse send(String method, String url, String body, Map<String, String> extraHeaders)
            throws ReplayCoreException {
        HttpRequest request = buildRequest(method, url, body, extraHeaders);
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

    private HttpRequest buildRequest(String method, String url, String body, Map<String, String> extraHeaders) {
        java.util.Map<String, String> headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Accept", "application/json");
        headers.put("User-Agent", userAgent);
        if (body != null) {
            headers.put("Content-Type", "application/json");
        }
        headers.putAll(extraHeaders);
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
