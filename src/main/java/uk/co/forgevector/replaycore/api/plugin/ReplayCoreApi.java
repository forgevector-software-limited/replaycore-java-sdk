/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * The root in-process entry point an addon obtains from a running ReplayCore
 * recorder.
 *
 * <p>Resolve it through {@link ReplayCoreProvider#get()} after the recorder
 * plugin has enabled (for example from your own plugin's enable step, ordered
 * after ReplayCore). From here an addon reaches the live capture surface and
 * registers lifecycle listeners.
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
 * // Later, on a significant in-game moment:
 * api.recordingService().addBookmark(
 *         Bookmark.builder("Final Kill").category("combat").build());
 * }</pre>
 *
 * <p>The API version string ({@link #apiVersion()}) lets an addon assert it is
 * talking to a compatible recorder before using newer capabilities.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface ReplayCoreApi {

    /**
     * Returns the version of this extension contract the running recorder
     * implements, as a {@code major.minor} string (for example {@code "1.0"}).
     * The major component changes only on an incompatible change.
     *
     * @return the API version; never {@code null}
     */
    String apiVersion();

    /**
     * Returns the recorder's live capture surface.
     *
     * @return the recording service; never {@code null}
     */
    RecordingService recordingService();

    /**
     * Registers a lifecycle listener. Registering the same instance twice has no
     * additional effect.
     *
     * @param listener the listener to add; must not be {@code null}
     */
    void registerListener(RecordingListener listener);

    /**
     * Removes a previously registered lifecycle listener. Removing a listener that
     * was never registered is a no-op.
     *
     * @param listener the listener to remove; must not be {@code null}
     */
    void unregisterListener(RecordingListener listener);
}
