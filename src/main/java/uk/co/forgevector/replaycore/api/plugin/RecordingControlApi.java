/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * The recorder's recording-state surface: whether a recording is live, which tick it is on, and a handle
 * to the active session. Obtain it from {@link ReplayCoreApi#recordingControl()}.
 *
 * <p>The read methods below are live now. Control verbs that would change capture policy (for example
 * pausing capture, excluding a player, forcing a segment rotation, or triggering a death-cam) are on the
 * roadmap and will arrive in a later release, each gated behind server config or permission so a
 * third-party plugin cannot silently change capture or billing behaviour. Until then, use the read methods
 * to correlate your plugin's state with the live recording, and {@link ReplayCoreTimelineApi} to annotate
 * it.
 *
 * <p>Implementations are provided by the recorder and are safe to call from the server's main thread:
 * calls are non-blocking and return promptly.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface RecordingControlApi {

    /**
     * Returns whether a recording is currently active on this server instance.
     *
     * @return {@code true} if recording is live
     */
    boolean isRecording();

    /**
     * Returns the tick the active recording is currently on, if recording.
     *
     * <p>This is the value to remember at the moment of an in-game event so a later action can correlate
     * with the same point on the recording timeline.
     *
     * @return the current tick, or an empty optional if not recording
     */
    OptionalLong currentTick();

    /**
     * Returns a handle to the active recording session, if any.
     *
     * @return the active session, or an empty optional if not recording
     */
    Optional<RecordingSession> currentSession();
}
