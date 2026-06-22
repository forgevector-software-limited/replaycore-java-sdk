/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import uk.co.forgevector.replaycore.api.exception.ReplayCoreException;
import uk.co.forgevector.replaycore.api.model.ReplayMetadata;
import uk.co.forgevector.replaycore.api.model.ReplayPage;
import uk.co.forgevector.replaycore.api.model.ReplayQuery;
import uk.co.forgevector.replaycore.api.model.TimelineEventRequest;
import uk.co.forgevector.replaycore.api.model.TimelineMarker;

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
