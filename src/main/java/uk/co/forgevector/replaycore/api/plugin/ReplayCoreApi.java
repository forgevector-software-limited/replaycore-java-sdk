/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;

/**
 * The umbrella entry point an addon obtains from a running ReplayCore recorder.
 *
 * <p>Resolve it through {@link ReplayCoreProvider#get()} (or Bukkit's services manager) after the recorder
 * plugin has enabled, for example from your own plugin's enable step, ordered after ReplayCore. From here
 * an addon reaches the timeline, recording-state, clip and kill-replay surfaces, and registers lifecycle
 * listeners.
 *
 * <p>Each sub-API is handed back so capability negotiation is built in: the always-present surfaces return
 * directly, while the optional ones return an {@link Optional} that is empty when this recorder build or
 * server config does not offer that capability. An addon guards its features cleanly rather than catching
 * an exception or parsing a version string.
 *
 * <pre>{@code
 * ReplayCoreApi api = ReplayCoreProvider.get()
 *         .orElseThrow(() -> new IllegalStateException("ReplayCore not present"));
 *
 * api.registerListener(new RecordingListener() {
 *     public void onRecordingStarted(RecordingSession s) {
 *         getLogger().info("recording started: " + s.sessionId());
 *     }
 * });
 *
 * // Mark a moment on the live recording (the timeline surface is always present):
 * api.timeline().tagTimelineEvent(IntegrationBookmark.builder("MyGameMode", "objective")
 *         .message("Captured the flag")
 *         .build());
 *
 * // Save a clip, if the server has clips enabled:
 * api.clips().ifPresent(clips -> clips.saveClip(staffUuid, reportedUuid, Duration.ofSeconds(30)));
 * }</pre>
 *
 * <p>The API version string ({@link #apiVersion()}) lets an addon assert it is talking to a compatible
 * recorder before using newer capabilities.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface ReplayCoreApi {

    /**
     * Returns the version of this in-process contract the running recorder implements, as a
     * {@code major.minor} string (for example {@code "1.0"}). The major component changes only on an
     * incompatible change, and it tracks the contract, not the product release train.
     *
     * @return the API version; never {@code null}
     */
    String apiVersion();

    /**
     * Returns the timeline-annotation surface for tagging custom events onto the live recording. Always
     * present while the recorder is enabled.
     *
     * @return the timeline API; never {@code null}
     */
    ReplayCoreTimelineApi timeline();

    /**
     * Returns the recording-state surface (whether recording is active, the current tick, and the active
     * session). Always present while the recorder is enabled.
     *
     * @return the recording-control API; never {@code null}
     */
    RecordingControlApi recordingControl();

    /**
     * Returns the on-demand clip surface, present only when this server has clips enabled and cloud upload
     * configured.
     *
     * @return the clip API, or an empty optional when clips are unavailable on this server
     */
    Optional<ReplayCoreClipApi> clips();

    /**
     * Returns the kill-replay surface for resolving a player's most recent death replay, present only when
     * the death-cam feature is enabled.
     *
     * @return the kill-replay API, or an empty optional when the feature is disabled
     */
    Optional<KillReplayApi> killReplay();

    /**
     * Registers a lifecycle listener. Registering the same instance twice has no additional effect.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    void registerListener(RecordingListener listener);

    /**
     * Removes a previously registered lifecycle listener. Removing a listener that was never registered is
     * a no-op.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    void unregisterListener(RecordingListener listener);
}
