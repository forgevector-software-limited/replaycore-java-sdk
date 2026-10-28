/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Instant;
import java.util.Optional;

/**
 * A read-only handle to a recording the host recorder is currently capturing, or
 * has just finished capturing.
 *
 * <p>An addon receives these from {@link RecordingListener} callbacks and from
 * {@link RecordingService#currentSession()}. A session is a snapshot of immutable
 * facts about the recording; it is not a control handle and exposes no way to
 * start, stop, download or delete a recording. Live, mutable state (such as the
 * current tick) is read through {@link RecordingService}, not here.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface RecordingSession {

    /**
     * Returns the {@code session_id} the REST API reports for this recording.
     *
     * <ul>
     *   <li>Until the recording first rotates into a second segment, this equals {@link #replayId()}. An
     *       archive that never rotates is its own one-segment session, and REST reports its replay id as
     *       its {@code session_id}.</li>
     *   <li>From the first rotation on, this is the RFC-0006 session id shared by every segment. The value
     *       therefore changes once, at the first rotation, and never again for that recording.</li>
     * </ul>
     *
     * <p>The value read from the session passed to {@link RecordingListener#onRecordingStopped} is final.
     * Pass it to {@code GET /v1/api/replays?session_id=} to find the replay this recording was published
     * as. To hold one key from the moment recording starts, store {@link #replayId()} instead.
     *
     * @return the session id as described above; never {@code null}
     */
    String sessionId();

    /**
     * Returns the replay id of the archive segment being written when this snapshot was taken. It is the
     * {@code id} that {@code GET /v1/api/replays/{id}} resolves, and that response carries the segment's
     * {@code session_id} and {@code segment_seq}. The id from {@link RecordingListener#onRecordingStarted}
     * is the first segment's, which is also the {@code id} of the entry the recording is listed under.
     *
     * <p>The default returns {@link #sessionId()}, which is the same value for a recording that has not
     * rotated, so existing implementations stay source and binary compatible. The recorder overrides it.
     *
     * @return the current segment's replay id; never {@code null}
     */
    default String replayId() {
        return sessionId();
    }

    /**
     * Returns the id of the server instance that owns the recording, as the
     * recorder registered it with the cloud.
     *
     * @return the server id; never {@code null}
     */
    String serverId();

    /**
     * Returns the integration or game mode the recording was started under (for
     * example a BedWars or Duels mode), when the host attributed one.
     *
     * @return the integration label, or an empty optional
     */
    Optional<String> integration();

    /**
     * Returns when the recording started.
     *
     * @return the start instant; never {@code null}
     */
    Instant startedAt();
}
