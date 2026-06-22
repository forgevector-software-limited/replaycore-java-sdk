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
     * Returns the stable session identifier for this recording, matching the
     * RFC-0006 {@code session_id} that segmented replays expose through the REST
     * API. For an addon, this is the join key between an in-process session and
     * the replay metadata it can later fetch with the REST client.
     *
     * @return the session id; never {@code null}
     */
    String sessionId();

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
