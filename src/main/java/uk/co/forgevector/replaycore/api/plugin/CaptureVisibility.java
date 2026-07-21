/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * How a subject's live presence is captured and disclosed in the recording, from the moment
 * {@link RecordingControlApi#updateSubject} is called for their UUID onward.
 *
 * <p>This is a capture-time gate applied per subject, for a privacy situation the recorder cannot detect on
 * its own: a disguise plugin masking who a player really is, a moderation tool shadowing a report without
 * naming the reporter, or a vanish provider outside the {@code "vanished"} metadata convention the
 * recorder's own vanish detection already covers. It governs what the recorder WRITES for the affected
 * UUID going forward; it is not a redaction of ticks already written before the call. See
 * {@link RecordingControlApi#updateSubject} for exactly when a change takes effect.
 *
 * <p>Distinct from {@link ReplayVisibility}: that enum governs who a fully-recorded, released collection or
 * asset is reachable to afterwards, over the REST catalogue. This one governs what is captured for one
 * player, live, while the recording is still being written, and the two are never interchangeable.
 */
public enum CaptureVisibility {
    /**
     * Captured and disclosed normally. The default for every UUID {@link RecordingControlApi#updateSubject}
     * has never been called for, and the value to pass to restore normal capture for one that has.
     */
    VISIBLE,
    /**
     * Excluded from the captured output entirely: no player-state sample is written for the UUID while this
     * is set, the same despawn/leave semantics {@code recording.vanished-players: "exclude"} already gives
     * a metadata-detected vanish, but keyed per subject and driven by this call instead of server config.
     */
    HIDDEN,
    /**
     * Captured, but the identity-disclosing parts of the record are redacted rather than the subject being
     * omitted: the world keeps their presence and actions, but who they are does not appear in the regular
     * capture. Intended for a disguise: a plugin that knows a player is playing as someone or something
     * else can flag the real identity redacted without making them vanish from the replay outright.
     */
    REDACTED
}
