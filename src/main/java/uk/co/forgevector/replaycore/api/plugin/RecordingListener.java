/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * A callback an addon registers to observe recording lifecycle boundaries.
 *
 * <p>Register an implementation with
 * {@link ReplayCoreApi#registerListener(RecordingListener)} and remove it with
 * {@link ReplayCoreApi#unregisterListener(RecordingListener)}. Every callback
 * must return quickly and must not block; offload any heavy or network work (for
 * example a REST call) to another thread.
 *
 * <p>The four callbacks do not all run on the same thread.
 * {@link #onRecordingStarted} and {@link #onRecordingStopped} are invoked on the
 * server's main thread at the corresponding session boundary, so they may touch
 * the platform API directly. {@link #onAssetReady} and {@link #onAssetFailed} are
 * invoked on the scope finalisation worker instead, because that outcome only
 * becomes known once the cloud has confirmed it, long after the originating call
 * returned. An implementation that needs the platform API from those two must
 * schedule the work back onto the main thread itself.
 *
 * <p>Callbacks are observational. They report what the recorder did; they cannot
 * veto or alter the recording. All methods are {@code default} no-ops so an addon
 * overrides only the boundaries it cares about. A callback that throws is
 * isolated and cannot break delivery for other listeners.
 */
public interface RecordingListener {

    /**
     * Invoked just after a new recording session starts.
     *
     * @param session the session that started; never {@code null}
     */
    default void onRecordingStarted(RecordingSession session) {
    }

    /**
     * Invoked just after a recording session ends. After this returns, the
     * session's replay enters cloud finalisation; its metadata becomes available
     * through the REST API once {@link uk.co.forgevector.replaycore.api.model.ReplayMetadata#isReady()}
     * reports ready.
     *
     * @param session the session that ended; never {@code null}
     */
    default void onRecordingStopped(RecordingSession session) {
    }

    /**
     * Invoked when a collection or asset opened through {@link ReplayCoreMatchApi} reaches
     * {@link ProcessingState#READY}. This is how a network integration observes the eventual outcome of a
     * {@link ReplayCoreMatchApi#endScope} call in-process, since the {@link java.util.concurrent.CompletionStage}
     * that call returned may already have completed with an in-progress state (or the process that issued
     * it may have restarted) by the time the true outcome is known.
     *
     * <p>Invoked on the scope finalisation worker, not the server's main thread. Schedule any work that
     * touches the platform API back onto the main thread.
     *
     * <p>Default no-op so an existing {@link RecordingListener} implementation remains source and binary
     * compatible without overriding it.
     *
     * @param result the operation result describing what became ready; never {@code null}
     */
    default void onAssetReady(ReplayOperationResult result) {
    }

    /**
     * Invoked when a collection or asset opened through {@link ReplayCoreMatchApi} reaches
     * {@link ProcessingState#FAILED}. See {@link #onAssetReady} for why this in-process callback exists
     * alongside the {@link java.util.concurrent.CompletionStage} returned by the originating call.
     *
     * <p>Invoked on the scope finalisation worker, not the server's main thread; see
     * {@link #onAssetReady} for what that means for an implementation.
     *
     * <p>Default no-op so an existing {@link RecordingListener} implementation remains source and binary
     * compatible without overriding it.
     *
     * @param result the operation result describing what failed; never {@code null}
     */
    default void onAssetFailed(ReplayOperationResult result) {
    }
}
