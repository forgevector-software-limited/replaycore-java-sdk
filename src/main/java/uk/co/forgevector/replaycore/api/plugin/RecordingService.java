/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;

/**
 * The recorder's live capture surface, as projected to an addon.
 *
 * <p>This is the public face of the recorder's internal capture sink: it reports
 * whether recording is currently active and at which tick, and exposes a handle to
 * the active session.
 *
 * @deprecated Superseded by {@link RecordingControlApi}, obtained from
 *             {@link ReplayCoreApi#recordingControl()}, which carries the same read methods (and the
 *             roadmap control verbs). To annotate the live recording, use
 *             {@link ReplayCoreTimelineApi#tagTimelineEvent(IntegrationBookmark)} through
 *             {@link ReplayCoreApi#timeline()}. This type is retained only for source compatibility and
 *             will be removed in a future major version.
 */
@Deprecated
public interface RecordingService {

    /**
     * Returns whether a recording is currently active on this server instance.
     *
     * @return {@code true} if recording is live
     */
    boolean isRecording();

    /**
     * Returns the tick the active recording is currently on, if recording.
     *
     * <p>This is the value to remember at the moment of an in-game event so a
     * later REST {@link uk.co.forgevector.replaycore.api.model.TimelineEventRequest}
     * can pin a marker to the same point, or simply to correlate addon state with
     * the recording timeline.
     *
     * @return the current tick, or an empty optional if not recording
     */
    Optional<Long> currentTick();

    /**
     * Returns a handle to the active recording session, if any.
     *
     * @return the active session, or an empty optional if not recording
     */
    Optional<RecordingSession> currentSession();

    /**
     * Asks the recorder to place a bookmark on the active recording at the current
     * tick.
     *
     * <p>This mirrors the REST timeline-event write, but from inside the tick loop
     * and against whatever recording is live now. It is best-effort: the recorder
     * may drop the bookmark if recording is not active, if the per-recording
     * bookmark budget is exhausted, or if the capture policy disallows it. The
     * return value reports whether the bookmark was accepted; it never throws for
     * an ordinary rejection.
     *
     * @param bookmark the bookmark to add; must not be {@code null}
     * @return {@code true} if the recorder accepted the bookmark, {@code false}
     *         if it was dropped
     * @deprecated Use {@link ReplayCoreTimelineApi#tagTimelineEvent(IntegrationBookmark)} instead, which
     *             is the canonical timeline-annotation surface and writes the same event the bundled
     *             integrations and the viewer already understand. Reach it through
     *             {@link ReplayCoreApi#timeline()}. This method is retained only for source
     *             compatibility and will be removed in a future major version.
     */
    @Deprecated
    boolean addBookmark(Bookmark bookmark);
}
