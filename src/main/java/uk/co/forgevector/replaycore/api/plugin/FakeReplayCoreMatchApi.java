/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An in-memory {@link ReplayCoreMatchApi} an integration compiles and runs its own tests against, without a
 * live recorder or cloud backend. This type ships in the SDK's main source tree, not only in its tests, so a
 * consuming project can depend on it directly.
 *
 * <p>{@link #beginScope} and {@link #updateScope} complete their returned stage immediately, matching the
 * real recorder's fail-open, non-blocking contract. {@link #endScope}, by contrast, returns a stage that
 * stays pending until the test drives it to a terminal outcome with {@link #completeReady},
 * {@link #completeFailed} or the more general {@link #completeFinalize}: this is the fixture's central
 * purpose, letting a test exercise both delivery paths a real integration must handle, the returned
 * {@link CompletionStage} and the {@link RecordingListener#onAssetReady}/{@link RecordingListener#onAssetFailed}
 * callback, from a single deterministic driver call. Register a listener with {@link #registerListener} to
 * observe the latter.
 *
 * <p>Every call to {@link #beginScope}, {@link #updateScope} and {@link #endScope} is recorded verbatim and
 * available afterwards through {@link #beginScopeCalls()}, {@link #updateScopeCalls()} and
 * {@link #endScopeCalls()}, so a test can assert on exactly what an integration sent without standing up
 * any network or persistence layer. Idempotency is honoured exactly as the real backend documents it: a
 * repeated {@link BeginScopeRequest#idempotencyKey()} returns the original {@link ReplayScope}, and a
 * repeated {@link EndScopeRequest#idempotencyKey()} for the same scope returns the original (pending or
 * completed) finalize stage, rather than doing the work twice.
 *
 * <p>{@link #setCurrentTick(long)} controls the tick a newly opened scope reports as its
 * {@link ReplayScope#startTick()}, since a fake has no real tick loop to read from.
 *
 * <pre>{@code
 * FakeReplayCoreMatchApi fake = new FakeReplayCoreMatchApi();
 * List<ReplayOperationResult> ready = new ArrayList<>();
 * fake.registerListener(new RecordingListener() {
 *     public void onAssetReady(ReplayOperationResult result) {
 *         ready.add(result);
 *     }
 * });
 *
 * ReplayScope scope = fake.beginScope(BeginScopeRequest.builder(
 *         "key-1", "ext-1", "duels", "ranked-1v1", "post-match").build())
 *         .toCompletableFuture().join();
 *
 * CompletionStage<FinalizeResult> finalizing = fake.endScope(scope.scopeId(),
 *         EndScopeRequest.builder("key-1-end").build());
 * fake.completeReady(scope.scopeId(), "asset-1", "https://api.example/replay-assets/asset-1", null);
 *
 * assertTrue(ready.size() == 1);
 * assertTrue(finalizing.toCompletableFuture().isDone());
 * }</pre>
 *
 * <p>Thread-safe: recorded calls use concurrent collections and state mutation is synchronized, so it can
 * be driven from a test thread while a system under test calls it from another.
 */
public final class FakeReplayCoreMatchApi implements ReplayCoreMatchApi {

    private final List<BeginScopeRequest> beginCalls = new CopyOnWriteArrayList<BeginScopeRequest>();
    private final List<RecordedUpdate> updateCalls = new CopyOnWriteArrayList<RecordedUpdate>();
    private final List<RecordedEnd> endCalls = new CopyOnWriteArrayList<RecordedEnd>();
    private final List<RecordingListener> listeners = new CopyOnWriteArrayList<RecordingListener>();

    private final ConcurrentHashMap<String, ReplayScope> scopesById = new ConcurrentHashMap<String, ReplayScope>();
    private final ConcurrentHashMap<String, String> scopeIdByBeginKey = new ConcurrentHashMap<String, String>();
    private final ConcurrentHashMap<String, EndOperation> endOperationsByScopeId = new ConcurrentHashMap<String, EndOperation>();

    private final AtomicLong scopeSequence = new AtomicLong();
    private final AtomicLong operationSequence = new AtomicLong();
    private volatile long currentTick;

    @Override
    public CompletionStage<ReplayScope> beginScope(BeginScopeRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        beginCalls.add(request);
        synchronized (this) {
            String existingScopeId = scopeIdByBeginKey.get(request.idempotencyKey());
            if (existingScopeId != null) {
                ReplayScope existing = scopesById.get(existingScopeId);
                if (existing != null) {
                    return CompletableFuture.completedFuture(existing);
                }
            }
            String scopeId = "scope-" + scopeSequence.incrementAndGet();
            String collectionId = "collection-" + scopeId;
            ReplayScope scope = new ReplayScope(scopeId, collectionId, request.externalMatchId(),
                    currentTick, ProcessingState.RECORDING);
            scopesById.put(scopeId, scope);
            scopeIdByBeginKey.put(request.idempotencyKey(), scopeId);
            return CompletableFuture.completedFuture(scope);
        }
    }

    @Override
    public CompletionStage<Void> updateScope(String scopeId, ScopeUpdate update) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(update, "update must not be null");
        updateCalls.add(new RecordedUpdate(scopeId, update));
        synchronized (this) {
            if (!scopesById.containsKey(scopeId)) {
                return failedStage(new NoSuchElementException("unknown scope: " + scopeId));
            }
            if (endOperationsByScopeId.containsKey(scopeId)) {
                return failedStage(new IllegalStateException("scope already ended: " + scopeId));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public CompletionStage<FinalizeResult> endScope(String scopeId, EndScopeRequest request) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(request, "request must not be null");
        endCalls.add(new RecordedEnd(scopeId, request));
        synchronized (this) {
            ReplayScope scope = scopesById.get(scopeId);
            if (scope == null) {
                return failedStage(new NoSuchElementException("unknown scope: " + scopeId));
            }
            EndOperation existing = endOperationsByScopeId.get(scopeId);
            if (existing != null) {
                if (existing.idempotencyKey.equals(request.idempotencyKey())) {
                    return existing.future;
                }
                return failedStage(new IllegalStateException(
                        "scope already ended with a different idempotency key: " + scopeId));
            }
            String operationId = "op-" + operationSequence.incrementAndGet();
            CompletableFuture<FinalizeResult> future = new CompletableFuture<FinalizeResult>();
            endOperationsByScopeId.put(scopeId,
                    new EndOperation(operationId, request.idempotencyKey(), scope.collectionId(), future));
            return future;
        }
    }

    /**
     * Drives the pending {@link #endScope} call for {@code scopeId} to a {@link ProcessingState#READY}
     * outcome, completing its returned {@link CompletionStage} and notifying every registered listener's
     * {@link RecordingListener#onAssetReady}.
     *
     * @param scopeId     the scope whose finalize is pending; must have an in-flight {@link #endScope} call
     * @param assetId     the produced full-match asset id, or {@code null} if none
     * @param resourceUrl the API resource URL, or {@code null} if not applicable
     * @param watchUrl    the browser watch URL, or {@code null} if not applicable; when non-null the result
     *                    reports watch-ticket eligibility
     * @throws IllegalStateException if no {@link #endScope} call is pending for {@code scopeId}, or its
     *                                finalize has already been completed
     */
    public void completeReady(String scopeId, String assetId, String resourceUrl, String watchUrl) {
        EndOperation op = requireEndOperation(scopeId);
        FinalizeResult result = FinalizeResult.builder(op.operationId, op.collectionId)
                .assetId(assetId)
                .processingState(ProcessingState.READY)
                .retryable(false)
                .playableAt(Instant.now())
                .resourceUrl(resourceUrl)
                .watchUrl(watchUrl)
                .watchTicketEligible(watchUrl != null)
                .build();
        completeFinalize(scopeId, result);
    }

    /**
     * Drives the pending {@link #endScope} call for {@code scopeId} to a {@link ProcessingState#FAILED}
     * outcome, completing its returned {@link CompletionStage} and notifying every registered listener's
     * {@link RecordingListener#onAssetFailed}.
     *
     * @param scopeId     the scope whose finalize is pending; must have an in-flight {@link #endScope} call
     * @param failureCode a machine-readable failure code; must not be blank
     * @param retryable   whether re-sending the same end-scope request may succeed after this failure
     * @throws IllegalStateException if no {@link #endScope} call is pending for {@code scopeId}, or its
     *                                finalize has already been completed
     */
    public void completeFailed(String scopeId, String failureCode, boolean retryable) {
        EndOperation op = requireEndOperation(scopeId);
        FinalizeResult result = FinalizeResult.builder(op.operationId, op.collectionId)
                .processingState(ProcessingState.FAILED)
                .failureCode(failureCode)
                .retryable(retryable)
                .build();
        completeFinalize(scopeId, result);
    }

    /**
     * Drives the pending {@link #endScope} call for {@code scopeId} to an arbitrary, caller-built outcome,
     * for a test that needs a {@link ProcessingState} or field combination the {@link #completeReady} and
     * {@link #completeFailed} conveniences do not cover. Notifies registered listeners only when
     * {@code result}'s {@link FinalizeResult#processingState()} is {@link ProcessingState#READY} or
     * {@link ProcessingState#FAILED}, matching what {@link RecordingListener} observes from the real
     * backend.
     *
     * @param scopeId the scope whose finalize is pending; must have an in-flight {@link #endScope} call
     * @param result  the outcome to complete the pending stage with; must not be {@code null}
     * @throws IllegalStateException if no {@link #endScope} call is pending for {@code scopeId}, or its
     *                                finalize has already been completed
     */
    public void completeFinalize(String scopeId, FinalizeResult result) {
        Objects.requireNonNull(result, "result must not be null");
        EndOperation op = requireEndOperation(scopeId);
        synchronized (this) {
            if (op.future.isDone()) {
                throw new IllegalStateException("finalize already completed for scope: " + scopeId);
            }
            op.future.complete(result);
        }
        if (result.processingState() == ProcessingState.READY
                || result.processingState() == ProcessingState.FAILED) {
            ReplayOperationResult asOperationResult = ReplayOperationResult
                    .builder(result.operationId(), result.collectionId())
                    .assetId(result.assetId().orElse(null))
                    .processingState(result.processingState())
                    .failureCode(result.failureCode().orElse(null))
                    .retryable(result.retryable())
                    .playableAt(result.playableAt().orElse(null))
                    .resourceUrl(result.resourceUrl().orElse(null))
                    .watchUrl(result.watchUrl().orElse(null))
                    .watchTicketEligible(result.watchTicketEligible())
                    .build();
            notifyListeners(asOperationResult);
        }
    }

    /**
     * Registers a listener to receive {@link RecordingListener#onAssetReady} and
     * {@link RecordingListener#onAssetFailed} callbacks driven by {@link #completeReady},
     * {@link #completeFailed} and {@link #completeFinalize}. Registering the same instance twice has no
     * additional effect.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    public void registerListener(RecordingListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a previously registered listener. Removing a listener that was never registered is a no-op.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    public void unregisterListener(RecordingListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.remove(listener);
    }

    /**
     * Sets the tick a newly opened scope reports as {@link ReplayScope#startTick()}. A fake has no real
     * tick loop to read from, so a test that cares about tick values sets one explicitly before calling
     * {@link #beginScope}.
     *
     * @param tick the tick to report for the next opened scope onwards
     */
    public void setCurrentTick(long tick) {
        this.currentTick = tick;
    }

    /** @return the tick that will be reported as {@link ReplayScope#startTick()} for the next opened scope */
    public long currentTick() {
        return currentTick;
    }

    /** @return every {@link #beginScope} request received so far, in call order; never {@code null} */
    public List<BeginScopeRequest> beginScopeCalls() {
        return Collections.unmodifiableList(new ArrayList<BeginScopeRequest>(beginCalls));
    }

    /** @return every {@link #updateScope} call received so far, in call order; never {@code null} */
    public List<RecordedUpdate> updateScopeCalls() {
        return Collections.unmodifiableList(new ArrayList<RecordedUpdate>(updateCalls));
    }

    /** @return every {@link #endScope} call received so far, in call order; never {@code null} */
    public List<RecordedEnd> endScopeCalls() {
        return Collections.unmodifiableList(new ArrayList<RecordedEnd>(endCalls));
    }

    /**
     * Looks up a previously opened scope by id.
     *
     * @param scopeId the scope id to look up; must not be {@code null}
     * @return the scope, or an empty optional if {@code scopeId} is unknown
     */
    public Optional<ReplayScope> scope(String scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        return Optional.ofNullable(scopesById.get(scopeId));
    }

    /**
     * Reports whether {@code scopeId} has an {@link #endScope} call awaiting {@link #completeReady},
     * {@link #completeFailed} or {@link #completeFinalize}.
     *
     * @param scopeId the scope id to check; must not be {@code null}
     * @return {@code true} if a finalize is pending for this scope
     */
    public boolean hasPendingFinalize(String scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        EndOperation op = endOperationsByScopeId.get(scopeId);
        return op != null && !op.future.isDone();
    }

    private EndOperation requireEndOperation(String scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        EndOperation op = endOperationsByScopeId.get(scopeId);
        if (op == null) {
            throw new IllegalStateException("no pending finalize for scope: " + scopeId);
        }
        return op;
    }

    private void notifyListeners(ReplayOperationResult result) {
        if (result.processingState() == ProcessingState.READY) {
            for (RecordingListener listener : listeners) {
                listener.onAssetReady(result);
            }
        } else if (result.processingState() == ProcessingState.FAILED) {
            for (RecordingListener listener : listeners) {
                listener.onAssetFailed(result);
            }
        }
    }

    private static <T> CompletionStage<T> failedStage(Throwable failure) {
        CompletableFuture<T> future = new CompletableFuture<T>();
        future.completeExceptionally(failure);
        return future;
    }

    /** One recorded {@link ReplayCoreMatchApi#updateScope} call. */
    public static final class RecordedUpdate {
        private final String scopeId;
        private final ScopeUpdate update;

        private RecordedUpdate(String scopeId, ScopeUpdate update) {
            this.scopeId = scopeId;
            this.update = update;
        }

        /** @return the scope id the call targeted; never {@code null} */
        public String scopeId() { return scopeId; }
        /** @return the update that was sent; never {@code null} */
        public ScopeUpdate update() { return update; }
    }

    /** One recorded {@link ReplayCoreMatchApi#endScope} call. */
    public static final class RecordedEnd {
        private final String scopeId;
        private final EndScopeRequest request;

        private RecordedEnd(String scopeId, EndScopeRequest request) {
            this.scopeId = scopeId;
            this.request = request;
        }

        /** @return the scope id the call targeted; never {@code null} */
        public String scopeId() { return scopeId; }
        /** @return the request that was sent; never {@code null} */
        public EndScopeRequest request() { return request; }
    }

    /** Internal bookkeeping for a pending or completed {@link #endScope} call. */
    private static final class EndOperation {
        private final String operationId;
        private final String idempotencyKey;
        private final String collectionId;
        private final CompletableFuture<FinalizeResult> future;

        private EndOperation(String operationId, String idempotencyKey, String collectionId,
                              CompletableFuture<FinalizeResult> future) {
            this.operationId = operationId;
            this.idempotencyKey = idempotencyKey;
            this.collectionId = collectionId;
            this.future = future;
        }
    }
}
