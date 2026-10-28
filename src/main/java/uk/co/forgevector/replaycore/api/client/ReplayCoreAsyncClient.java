/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import uk.co.forgevector.replaycore.api.exception.ReplayCoreException;
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
 * An asynchronous façade over {@link ReplayCoreClient} that returns
 * {@link CompletableFuture}s, so calls never block the calling thread.
 *
 * <p>This is ideal inside a Minecraft plugin, where blocking the main server
 * thread on network I/O would stall the whole server. Each method submits the
 * corresponding synchronous call to an {@link Executor} (the common fork-join
 * pool by default) and completes the future with the result, or completes it
 * exceptionally with a {@link ReplayCoreException} on failure.
 *
 * <pre>{@code
 * ReplayCoreAsyncClient client = ReplayCoreClient.builder()
 *         .apiKey(key)
 *         .buildAsync();
 *
 * client.getReplay(replayId)
 *       .thenAccept(replay -> getLogger().info("ready=" + replay.isReady()))
 *       .exceptionally(err -> { getLogger().warning(err.getMessage()); return null; });
 * }</pre>
 *
 * <p>Build one with {@link ReplayCoreClientBuilder#buildAsync()}, or wrap an
 * existing synchronous client to share its configuration.
 */
public final class ReplayCoreAsyncClient {

    private final ReplayCoreClient delegate;
    private final Executor executor;

    /**
     * Wraps a synchronous client, dispatching calls on the common fork-join pool.
     *
     * @param delegate the synchronous client to delegate to; must not be {@code null}
     */
    public ReplayCoreAsyncClient(ReplayCoreClient delegate) {
        this(delegate, ForkJoinPool.commonPool());
    }

    /**
     * Wraps a synchronous client, dispatching calls on a caller-supplied executor.
     *
     * <p>Supplying a dedicated executor is recommended for high-throughput use, so
     * ReplayCore calls do not compete with other work on the common pool.
     *
     * @param delegate the synchronous client to delegate to; must not be {@code null}
     * @param executor the executor to run calls on; must not be {@code null}
     */
    public ReplayCoreAsyncClient(ReplayCoreClient delegate, Executor executor) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.delegate = delegate;
        this.executor = executor;
    }

    /**
     * Asynchronously lists replays. See {@link ReplayCoreClient#listReplays(ReplayQuery)}.
     *
     * @param query the filters and page settings; must not be {@code null}
     * @return a future that completes with one page of replays, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayPage> listReplays(ReplayQuery query) {
        return supply(new ThrowingSupplier<ReplayPage>() {
            @Override
            public ReplayPage get() throws ReplayCoreException {
                return delegate.listReplays(query);
            }
        });
    }

    /**
     * Asynchronously fetches one replay. See {@link ReplayCoreClient#getReplay(String)}.
     *
     * @param replayId the replay UUID; must not be {@code null} or blank
     * @return a future that completes with the replay metadata, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayMetadata> getReplay(final String replayId) {
        return supply(new ThrowingSupplier<ReplayMetadata>() {
            @Override
            public ReplayMetadata get() throws ReplayCoreException {
                return delegate.getReplay(replayId);
            }
        });
    }

    /**
     * Asynchronously lists connected servers. See {@link ReplayCoreClient#listServers()}.
     *
     * @return a future that completes with the connected server instances, or
     *         completes exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<List<ServerInstance>> listServers() {
        return supply(new ThrowingSupplier<List<ServerInstance>>() {
            @Override
            public List<ServerInstance> get() throws ReplayCoreException {
                return delegate.listServers();
            }
        });
    }

    /**
     * Asynchronously creates a pending server setup.
     *
     * @param request the server name and optional placement/template
     * @return a future that completes with the pending setup
     */
    public CompletableFuture<ServerSetup> createServer(final ServerSetupRequest request) {
        return supply(new ThrowingSupplier<ServerSetup>() {
            @Override
            public ServerSetup get() throws ReplayCoreException {
                return delegate.createServer(request);
            }
        });
    }

    /**
     * Asynchronously renames a connected server.
     *
     * @param serverId the connected server UUID
     * @param name the new display name
     * @return a future that completes with the updated server
     */
    public CompletableFuture<ServerInstance> renameServer(final String serverId, final String name) {
        return supply(new ThrowingSupplier<ServerInstance>() {
            @Override
            public ServerInstance get() throws ReplayCoreException {
                return delegate.renameServer(serverId, name);
            }
        });
    }

    /**
     * Asynchronously deauthorises a connected server.
     *
     * @param serverId the connected server UUID
     * @return a future that completes when the server is deauthorised
     */
    public CompletableFuture<Void> deauthoriseServer(final String serverId) {
        return supply(new ThrowingSupplier<Void>() {
            @Override
            public Void get() throws ReplayCoreException {
                delegate.deauthoriseServer(serverId);
                return null;
            }
        });
    }

    /**
     * Asynchronously permanently removes a connected server.
     *
     * @param serverId the connected server UUID
     * @return a future that completes when the server is removed
     */
    public CompletableFuture<Void> permanentlyRemoveServer(final String serverId) {
        return supply(new ThrowingSupplier<Void>() {
            @Override
            public Void get() throws ReplayCoreException {
                delegate.permanentlyRemoveServer(serverId);
                return null;
            }
        });
    }

    /**
     * Asynchronously cancels a pending server setup.
     *
     * @param serverId the pending server id
     * @return a future that completes when the pending setup is cancelled
     */
    public CompletableFuture<Void> cancelServerSetup(final String serverId) {
        return supply(new ThrowingSupplier<Void>() {
            @Override
            public Void get() throws ReplayCoreException {
                delegate.cancelServerSetup(serverId);
                return null;
            }
        });
    }

    /**
     * Asynchronously reads one workspace setup resource.
     *
     * @param path the setup path without {@code /v1/api/setup/}
     * @return a future that completes with the successful JSON response
     */
    public CompletableFuture<ApiResponse> getSetup(final String path) {
        return supply(new ThrowingSupplier<ApiResponse>() {
            @Override
            public ApiResponse get() throws ReplayCoreException {
                return delegate.getSetup(path);
            }
        });
    }

    /**
     * Asynchronously calls one workspace setup resource.
     *
     * @param method {@code GET}, {@code POST}, {@code PUT}, {@code PATCH}, or {@code DELETE}
     * @param path the setup path without {@code /v1/api/setup/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @return a future that completes with the successful JSON response
     */
    public CompletableFuture<ApiResponse> requestSetup(
            final String method, final String path, final Object jsonBody) {
        return supply(new ThrowingSupplier<ApiResponse>() {
            @Override
            public ApiResponse get() throws ReplayCoreException {
                return delegate.requestSetup(method, path, jsonBody);
            }
        });
    }

    /**
     * Asynchronously calls any JSON endpoint on the configured ReplayCore origin.
     *
     * @param method {@code GET}, {@code POST}, {@code PUT}, {@code PATCH}, or {@code DELETE}
     * @param path an absolute API path beginning with {@code /v1/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @return a future that completes with the successful JSON response
     */
    public CompletableFuture<ApiResponse> requestJson(
            final String method, final String path, final Object jsonBody) {
        return supply(new ThrowingSupplier<ApiResponse>() {
            @Override
            public ApiResponse get() throws ReplayCoreException {
                return delegate.requestJson(method, path, jsonBody);
            }
        });
    }

    /**
     * Asynchronously calls any JSON endpoint on the configured ReplayCore origin,
     * adding request headers.
     * See {@link ReplayCoreClient#requestJson(String, String, Object, Map)} for the
     * headers that are refused.
     *
     * @param method {@code GET}, {@code POST}, {@code PUT}, {@code PATCH}, or {@code DELETE}
     * @param path an absolute API path beginning with {@code /v1/}
     * @param jsonBody a JSON-compatible tree, or {@code null} for no body
     * @param headers extra request headers such as {@code Idempotency-Key} or
     *                {@code If-Match}, or {@code null} for none
     * @return a future that completes with the successful JSON response
     */
    public CompletableFuture<ApiResponse> requestJson(
            final String method, final String path, final Object jsonBody,
            final Map<String, String> headers) {
        return supply(new ThrowingSupplier<ApiResponse>() {
            @Override
            public ApiResponse get() throws ReplayCoreException {
                return delegate.requestJson(method, path, jsonBody, headers);
            }
        });
    }

    /**
     * Asynchronously creates a timeline marker.
     * See {@link ReplayCoreClient#createTimelineMarker(TimelineEventRequest)}.
     *
     * @param request the marker to create; must not be {@code null}
     * @return a future that completes with the created marker, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<TimelineMarker> createTimelineMarker(final TimelineEventRequest request) {
        return supply(new ThrowingSupplier<TimelineMarker>() {
            @Override
            public TimelineMarker get() throws ReplayCoreException {
                return delegate.createTimelineMarker(request);
            }
        });
    }

    /**
     * Asynchronously opens a replay collection.
     * See {@link ReplayCoreClient#createCollection(CollectionRequest)}.
     *
     * @param request the collection to open, with its idempotency key
     * @return a future that completes with the stored collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> createCollection(final CollectionRequest request) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.createCollection(request);
            }
        });
    }

    /**
     * Asynchronously updates a replay collection.
     * See {@link ReplayCoreClient#updateCollection(String, CollectionUpdateRequest)}.
     *
     * @param collectionId the collection UUID
     * @param request the update, with its idempotency key
     * @return a future that completes with the operation outcome, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ScopeOperation> updateCollection(
            final String collectionId, final CollectionUpdateRequest request) {
        return supply(new ThrowingSupplier<ScopeOperation>() {
            @Override
            public ScopeOperation get() throws ReplayCoreException {
                return delegate.updateCollection(collectionId, request);
            }
        });
    }

    /**
     * Asynchronously finalises a replay collection.
     * See {@link ReplayCoreClient#finalizeCollection(String, CollectionFinalizeRequest)}.
     *
     * @param collectionId the collection UUID
     * @param request the finalisation, with its idempotency key
     * @return a future that completes with the operation outcome, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ScopeOperation> finalizeCollection(
            final String collectionId, final CollectionFinalizeRequest request) {
        return supply(new ThrowingSupplier<ScopeOperation>() {
            @Override
            public ScopeOperation get() throws ReplayCoreException {
                return delegate.finalizeCollection(collectionId, request);
            }
        });
    }

    /**
     * Asynchronously holds a replay collection.
     * See {@link ReplayCoreClient#holdCollection(String, String)}.
     *
     * @param collectionId the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return a future that completes with the updated collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> holdCollection(
            final String collectionId, final String idempotencyKey) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.holdCollection(collectionId, idempotencyKey);
            }
        });
    }

    /**
     * Asynchronously releases a replay collection.
     * See {@link ReplayCoreClient#releaseCollection(String, String)}.
     *
     * @param collectionId the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return a future that completes with the updated collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> releaseCollection(
            final String collectionId, final String idempotencyKey) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.releaseCollection(collectionId, idempotencyKey);
            }
        });
    }

    /**
     * Asynchronously revokes a replay collection.
     * See {@link ReplayCoreClient#revokeCollection(String, String)}.
     *
     * @param collectionId the collection UUID
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @return a future that completes with the updated collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> revokeCollection(
            final String collectionId, final String idempotencyKey) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.revokeCollection(collectionId, idempotencyKey);
            }
        });
    }

    /**
     * Asynchronously lists replay collections.
     * See {@link ReplayCoreClient#listCollections(CollectionQuery)}.
     *
     * @param query the filters and page settings; must not be {@code null}
     * @return a future that completes with one page of collections, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<CatalogPage<ReplayCollection>> listCollections(
            final CollectionQuery query) {
        return supply(new ThrowingSupplier<CatalogPage<ReplayCollection>>() {
            @Override
            public CatalogPage<ReplayCollection> get() throws ReplayCoreException {
                return delegate.listCollections(query);
            }
        });
    }

    /**
     * Asynchronously fetches one replay collection.
     * See {@link ReplayCoreClient#getCollection(String)}.
     *
     * @param collectionId the collection UUID
     * @return a future that completes with the collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> getCollection(final String collectionId) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.getCollection(collectionId);
            }
        });
    }

    /**
     * Asynchronously fetches one replay collection, resolving a player's relation to it.
     * See {@link ReplayCoreClient#getCollection(String, String)}.
     *
     * @param collectionId the collection UUID
     * @param playerUuid a player UUID whose relation is resolved, or {@code null}
     * @return a future that completes with the collection, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayCollection> getCollection(
            final String collectionId, final String playerUuid) {
        return supply(new ThrowingSupplier<ReplayCollection>() {
            @Override
            public ReplayCollection get() throws ReplayCoreException {
                return delegate.getCollection(collectionId, playerUuid);
            }
        });
    }

    /**
     * Asynchronously lists the assets of a replay collection.
     * See {@link ReplayCoreClient#listCollectionAssets(String, AssetQuery)}.
     *
     * @param collectionId the collection UUID
     * @param query the filters and page settings; must not be {@code null}
     * @return a future that completes with one page of assets, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<CatalogPage<ReplayAsset>> listCollectionAssets(
            final String collectionId, final AssetQuery query) {
        return supply(new ThrowingSupplier<CatalogPage<ReplayAsset>>() {
            @Override
            public CatalogPage<ReplayAsset> get() throws ReplayCoreException {
                return delegate.listCollectionAssets(collectionId, query);
            }
        });
    }

    /**
     * Asynchronously lists the timeline markers of a replay collection.
     * See {@link ReplayCoreClient#listCollectionTimelineEvents(String)}.
     *
     * @param collectionId the collection UUID
     * @return a future that completes with the markers in timeline order, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<List<ScopeMarker>> listCollectionTimelineEvents(
            final String collectionId) {
        return supply(new ThrowingSupplier<List<ScopeMarker>>() {
            @Override
            public List<ScopeMarker> get() throws ReplayCoreException {
                return delegate.listCollectionTimelineEvents(collectionId);
            }
        });
    }

    /**
     * Asynchronously counts the catalogue along each facet dimension.
     * See {@link ReplayCoreClient#getCollectionFacets()}.
     *
     * @return a future that completes with the counts per dimension, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<CatalogFacets> getCollectionFacets() {
        return supply(new ThrowingSupplier<CatalogFacets>() {
            @Override
            public CatalogFacets get() throws ReplayCoreException {
                return delegate.getCollectionFacets();
            }
        });
    }

    /**
     * Asynchronously looks up many collections at once.
     * See {@link ReplayCoreClient#bulkLookupCollections(List, List)}.
     *
     * @param externalMatchIds integration match ids; may be {@code null}
     * @param collectionIds collection UUIDs; may be {@code null}
     * @return a future that completes with the collections found, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<List<ReplayCollection>> bulkLookupCollections(
            final List<String> externalMatchIds, final List<String> collectionIds) {
        return supply(new ThrowingSupplier<List<ReplayCollection>>() {
            @Override
            public List<ReplayCollection> get() throws ReplayCoreException {
                return delegate.bulkLookupCollections(externalMatchIds, collectionIds);
            }
        });
    }

    /**
     * Asynchronously creates a clip or highlight from a replay collection.
     * See {@link ReplayCoreClient#createAsset(String, AssetRequest)}.
     *
     * @param collectionId the collection UUID
     * @param request the asset to create, with its idempotency key
     * @return a future that completes with the operation outcome, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ScopeOperation> createAsset(
            final String collectionId, final AssetRequest request) {
        return supply(new ThrowingSupplier<ScopeOperation>() {
            @Override
            public ScopeOperation get() throws ReplayCoreException {
                return delegate.createAsset(collectionId, request);
            }
        });
    }

    /**
     * Asynchronously reports that a replay asset's media is ready or has failed.
     * See {@link ReplayCoreClient#finalizeAsset(String, AssetFinalizeRequest)}.
     *
     * @param assetId the asset UUID
     * @param request the report, with its idempotency key
     * @return a future that completes with the operation outcome, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ScopeOperation> finalizeAsset(
            final String assetId, final AssetFinalizeRequest request) {
        return supply(new ThrowingSupplier<ScopeOperation>() {
            @Override
            public ScopeOperation get() throws ReplayCoreException {
                return delegate.finalizeAsset(assetId, request);
            }
        });
    }

    /**
     * Asynchronously creates many event clips in one call.
     * See {@link ReplayCoreClient#createAssetsBatch(String, List)}.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @param entries 1 to 256 clips
     * @return a future that completes with one result per entry, in request order, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<List<BatchEntryResult>> createAssetsBatch(
            final String idempotencyKey, final List<ScopeClipEntry> entries) {
        return supply(new ThrowingSupplier<List<BatchEntryResult>>() {
            @Override
            public List<BatchEntryResult> get() throws ReplayCoreException {
                return delegate.createAssetsBatch(idempotencyKey, entries);
            }
        });
    }

    /**
     * Asynchronously creates many collection timeline markers in one call.
     * See {@link ReplayCoreClient#createTimelineEventsBatch(String, List)}.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters
     * @param entries 1 to 256 markers
     * @return a future that completes with one result per entry, in request order, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<List<BatchEntryResult>> createTimelineEventsBatch(
            final String idempotencyKey, final List<ScopeMarkerEntry> entries) {
        return supply(new ThrowingSupplier<List<BatchEntryResult>>() {
            @Override
            public List<BatchEntryResult> get() throws ReplayCoreException {
                return delegate.createTimelineEventsBatch(idempotencyKey, entries);
            }
        });
    }

    /**
     * Asynchronously fetches one replay asset.
     * See {@link ReplayCoreClient#getAsset(String)}.
     *
     * @param assetId the asset UUID
     * @return a future that completes with the asset, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<ReplayAsset> getAsset(final String assetId) {
        return supply(new ThrowingSupplier<ReplayAsset>() {
            @Override
            public ReplayAsset get() throws ReplayCoreException {
                return delegate.getAsset(assetId);
            }
        });
    }

    /**
     * Asynchronously lists the replay assets a player appears in.
     * See {@link ReplayCoreClient#listPlayerReplays(String, PlayerReplayQuery)}.
     *
     * @param playerUuid the player UUID
     * @param query the filters and page settings; must not be {@code null}
     * @return a future that completes with one page of assets, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<CatalogPage<ReplayAsset>> listPlayerReplays(
            final String playerUuid, final PlayerReplayQuery query) {
        return supply(new ThrowingSupplier<CatalogPage<ReplayAsset>>() {
            @Override
            public CatalogPage<ReplayAsset> get() throws ReplayCoreException {
                return delegate.listPlayerReplays(playerUuid, query);
            }
        });
    }

    /**
     * Asynchronously issues a watch ticket for a replay asset.
     * See {@link ReplayCoreClient#issueWatchTicket(String, String)}.
     *
     * @param assetId the asset UUID
     * @param viewerPlayerUuid the UUID of the player who will watch, or {@code null}
     * @return a future that completes with the ticket, or a not-ready notice, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<WatchTicket> issueWatchTicket(
            final String assetId, final String viewerPlayerUuid) {
        return supply(new ThrowingSupplier<WatchTicket>() {
            @Override
            public WatchTicket get() throws ReplayCoreException {
                return delegate.issueWatchTicket(assetId, viewerPlayerUuid);
            }
        });
    }

    /**
     * Asynchronously redeems a watch ticket.
     * See {@link ReplayCoreClient#redeemWatchTicket(String, String, String)}.
     *
     * @param assetId the asset UUID
     * @param ticket the ticket secret
     * @param viewerPlayerUuid the UUID of the player who is watching, or {@code null}
     * @return a future that completes with the segments to play, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<WatchTicketRedemption> redeemWatchTicket(
            final String assetId, final String ticket, final String viewerPlayerUuid) {
        return supply(new ThrowingSupplier<WatchTicketRedemption>() {
            @Override
            public WatchTicketRedemption get() throws ReplayCoreException {
                return delegate.redeemWatchTicket(assetId, ticket, viewerPlayerUuid);
            }
        });
    }

    /**
     * Asynchronously mints a durable watch link for a replay asset.
     * See {@link ReplayCoreClient#createWatchLink(String)}.
     *
     * @param assetId the asset UUID
     * @return a future that completes with the link, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<WatchLink> createWatchLink(final String assetId) {
        return supply(new ThrowingSupplier<WatchLink>() {
            @Override
            public WatchLink get() throws ReplayCoreException {
                return delegate.createWatchLink(assetId);
            }
        });
    }

    /**
     * Asynchronously replays a webhook delivery.
     * See {@link ReplayCoreClient#replayWebhookDelivery(String)}.
     *
     * @param deliveryId the delivery UUID
     * @return a future that completes with the delivery status after the replay, or completes
     *         exceptionally with a {@link ReplayCoreException}
     */
    public CompletableFuture<WebhookDeliveryReplay> replayWebhookDelivery(final String deliveryId) {
        return supply(new ThrowingSupplier<WebhookDeliveryReplay>() {
            @Override
            public WebhookDeliveryReplay get() throws ReplayCoreException {
                return delegate.replayWebhookDelivery(deliveryId);
            }
        });
    }

    /**
     * Returns the synchronous client this async client delegates to, for callers
     * that occasionally need a blocking call.
     *
     * @return the underlying synchronous client
     */
    public ReplayCoreClient blocking() {
        return delegate;
    }

    private <T> CompletableFuture<T> supply(final ThrowingSupplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<T>();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    future.complete(supplier.get());
                } catch (ReplayCoreException e) {
                    future.completeExceptionally(e);
                } catch (RuntimeException e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    /** A supplier whose {@code get} may throw the SDK's checked exception. */
    private interface ThrowingSupplier<T> {
        T get() throws ReplayCoreException;
    }
}
