/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * The recorder's recording-state surface: whether a recording is live, which tick it is on, and a handle
 * to the active session. Obtain it from {@link ReplayCoreApi#recordingControl()}.
 *
 * <p>The read methods below are live now, and so is {@link #updateSubject}, the first of the control verbs
 * that change capture policy: a per-subject capture-visibility override for vanish/disguise redaction.
 * Every OTHER control verb that would change capture policy (for example pausing capture server-wide,
 * forcing a segment rotation, or triggering a death-cam) remains on the roadmap and will arrive in a later
 * release, each gated behind server config or permission so a third-party plugin cannot silently change
 * capture or billing behaviour. Until then, use the read methods to correlate your plugin's state with the
 * live recording, and {@link ReplayCoreTimelineApi} to annotate it.
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

    /**
     * Sets or clears a capture-time privacy override for one subject, keyed by their UUID, effective from
     * the moment this call returns for as long as the recorder keeps sampling that UUID.
     *
     * <p>This is the "excluding a player" / "changing capture policy" control verb the interface
     * documentation above forecasts, scoped to one subject rather than a server-wide config toggle:
     * passing {@link CaptureVisibility#HIDDEN} or {@link CaptureVisibility#REDACTED} gates what the
     * recorder captures and discloses for that UUID from this call onward, independently of every other
     * player's capture. Passing {@link CaptureVisibility#VISIBLE} (or never calling this for a UUID) leaves
     * capture unaffected. The override does not retroactively touch ticks already written before the call.
     *
     * <p>{@code canonicalName} and {@code displayName} let the caller record who the subject really is
     * alongside what should be disclosed while the override is active (for example a disguise plugin
     * passing the disguised name as {@code displayName} so it, not the real name, appears in the captured
     * record); either may be {@code null} when not applicable. {@code metadata} carries caller-defined
     * context (for example a case or ticket id) and may be {@code null} or empty.
     *
     * <p>Implemented as a {@code default} method for the same reason as {@link ReplayCoreApi#matches()}:
     * this interface is a published contract addons and test doubles already implement, so adding an
     * abstract method here would break them at compile and link time. The default is a no-op that reports
     * rejection, which is exactly correct for a recorder build that predates this capability.
     *
     * @param uuid          the subject's id; must not be {@code null}
     * @param canonicalName the subject's real/canonical name, or {@code null} when not supplied
     * @param displayName   the name to disclose while the override is active, or {@code null} to disclose
     *                      no override (the live name is captured unchanged)
     * @param visibility    how this subject should be captured and disclosed from now on; must not be
     *                      {@code null}
     * @param metadata      caller-defined context for this override, or {@code null}/empty for none
     * @return {@code true} if the override was accepted; {@code false} when this recorder build offers no
     *         capture-visibility control (the default implementation), or {@code uuid} / {@code visibility}
     *         was {@code null}
     */
    default boolean updateSubject(UUID uuid, String canonicalName, String displayName,
                                  CaptureVisibility visibility, Map<String, String> metadata) {
        return false;
    }
}
