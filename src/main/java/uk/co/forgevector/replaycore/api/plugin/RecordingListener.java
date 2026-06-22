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
 * invokes the callbacks on the server's main thread at the corresponding
 * session boundary, so implementations must return quickly and must not block;
 * offload any heavy or network work (for example a REST call) to another thread.
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
}
