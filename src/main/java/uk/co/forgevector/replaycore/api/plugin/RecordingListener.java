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
 * {@link ReplayCoreApi#unregisterListener(RecordingListener)}. The recorder
 * invokes start and stop callbacks on the recorder lifecycle thread. Asset
 * callbacks run on a finalisation worker. No callback establishes ownership of
 * a Bukkit world or entity. On Folia, use the scheduler that owns the target
 * region or entity before reading its live state; on Bukkit, schedule live
 * platform access on the server thread. Use the supplied immutable snapshots
 * for correlation. Callbacks must return quickly and must not block.
 *
 * <p>Keep the listener instance and unregister it when the addon disables.
 * Set an addon-owned active flag to false before unregistering so a callback
 * already present in a dispatch snapshot does not run addon work after unload.
 * Cancel addon-owned scheduled work as part of the same teardown.
 *
 * <p>Callbacks are observational. They report what the recorder did; they cannot
 * veto or alter the recording. All methods are {@code default} no-ops so an addon
 * overrides only the boundaries it cares about.
 *
 * <p>Forward-looking contract: see the package documentation for status.
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
     * <p>Default no-op so an existing {@link RecordingListener} implementation remains source and binary
     * compatible without overriding it.
     *
     * @param result the operation result describing what failed; never {@code null}
     */
    default void onAssetFailed(ReplayOperationResult result) {
    }
}
