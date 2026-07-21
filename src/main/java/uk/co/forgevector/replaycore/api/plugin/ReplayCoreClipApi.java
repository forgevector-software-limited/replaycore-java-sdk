/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.time.Duration;
import java.util.UUID;

/**
 * Programmatic surface a server owner's plugin can call to save an on-demand clip from code, the
 * in-process twin of the {@code /replaycore save} command. A plugin can, for example, auto-save a clip of
 * a reported player from a reports GUI, or clip both fighters at the end of a duel, without parsing
 * commands.
 *
 * <p>Obtain the instance from the running recorder, either through the umbrella
 * {@link ReplayCoreApi#clips()} or directly from Bukkit's services manager. The service is registered
 * only when clips are enabled and cloud upload is configured, so an absent registration (or an empty
 * {@link java.util.Optional} from the umbrella) means clips are unavailable on this server:
 * <pre>{@code
 * RegisteredServiceProvider<ReplayCoreClipApi> rsp =
 *     getServer().getServicesManager().getRegistration(ReplayCoreClipApi.class);
 * if (rsp != null) {
 *     ReplayCoreClipApi.SaveResult result =
 *         rsp.getProvider().saveClip(staffUuid, reportedPlayerUuid, Duration.ofSeconds(30));
 *     // SAVING means the watch link will be delivered to staffUuid shortly.
 * }
 * }</pre>
 *
 * <p>Every call is non-blocking and main-thread-safe: it validates, clamps the window to
 * {@code min(requested, online this session, server cap)}, honours the same per-requester cooldown as the
 * command (a plugin cannot bypass the limits), enqueues the work, and returns immediately. The cloud mint
 * and the clickable link delivery happen off the main thread on the clip worker. Both recorder lanes
 * (modern 1.21 and above, and legacy 1.8.8) register the identical service.
 *
 * <p>For an open-ended window (for example a round of unknown length), call {@link #startClip} at the
 * round start and {@link #stopClip} at the end instead of {@link #saveClip}: the same in-marker mechanism
 * as {@code /replaycore start | stop}.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface ReplayCoreClipApi {

    /**
     * Saves the last {@code window} of {@code target}'s gameplay as a clip, attributed to {@code requester}
     * (the player the watch link is delivered to, typically the staff member, or the target for a
     * self-clip). The window is clamped to the server's cap and the target's online-this-session time, and
     * the requester's clip cooldown is enforced.
     *
     * @param requester the clip's attribution; the watch link is delivered to this player if online (else
     *                  logged). Must not be {@code null}.
     * @param target    the player whose recent gameplay is clipped. Must be online so the recorder can
     *                  resolve their session window; an unresolved target yields {@link SaveResult#UNAVAILABLE}.
     * @param window    the requested clip length, clamped down to the server cap and the target's online
     *                  time. A {@code null} or non-positive window is treated as the smallest valid request.
     * @return the outcome: {@link SaveResult#SAVING} on success, otherwise the reason it was refused
     */
    SaveResult saveClip(UUID requester, UUID target, Duration window);

    /**
     * Opens an in-marker for {@code target} at the current moment over the continuous recording (no new
     * recording starts). Pair with {@link #stopClip} to close the window and mint the clip.
     *
     * @param requester the clip's attribution; the watch link from the matching {@link #stopClip} is
     *                  delivered to this player. Must not be {@code null}.
     * @param target    the player whose gameplay the marker tracks. Must be online; an unresolved target
     *                  yields {@link StartResult#UNAVAILABLE}.
     * @return the outcome: {@link StartResult#STARTED} on success, otherwise the reason it was refused
     */
    StartResult startClip(UUID requester, UUID target);

    /**
     * Stops the open clip for {@code target} previously started via {@link #startClip} and mints it,
     * delivering the watch link to the original requester.
     *
     * @param requester the requester whose open marker is being closed. Must not be {@code null}.
     * @param target    the player whose open marker is closed. Must match the {@link #startClip} target.
     * @return the outcome: {@link StopResult#SAVING} on success, otherwise the reason it was refused
     */
    StopResult stopClip(UUID requester, UUID target);

    /** The outcome of {@link #saveClip(UUID, UUID, Duration)}. */
    enum SaveResult {
        /** Accepted: the clip is being minted and the watch link will be delivered to the requester. */
        SAVING,
        /** The requester is on cooldown for the save action. */
        ON_COOLDOWN,
        /** The clamped window was below the minimum floor (not enough gameplay yet). */
        TOO_SHORT,
        /** Recording is currently stopped on this server (emergency switch or cloud kill-switch). */
        NO_RECORDING,
        /** The clip queue is saturated (backpressure) or the requester is at their in-flight cap. */
        BUSY,
        /** The target could not be resolved, or clips are otherwise unavailable. */
        UNAVAILABLE
    }

    /** The outcome of {@link #startClip(UUID, UUID)}. */
    enum StartResult {
        /** Accepted: an in-marker is now open for the requester and target; call stop to save it. */
        STARTED,
        /** A clip was already in progress for this requester and target; the open marker is untouched. */
        ALREADY_OPEN,
        /** The requester is on cooldown for the start action. */
        ON_COOLDOWN,
        /** Recording is currently stopped on this server (emergency switch or cloud kill-switch). */
        NO_RECORDING,
        /** The target could not be resolved, or clips are otherwise unavailable. */
        UNAVAILABLE
    }

    /** The outcome of {@link #stopClip(UUID, UUID)}. */
    enum StopResult {
        /** Accepted: the clip is being minted and the watch link will be delivered to the requester. */
        SAVING,
        /** No clip was in progress for this requester and target; call start first. */
        NO_MARKER,
        /** The requester is on cooldown for the stop action. */
        ON_COOLDOWN,
        /** The clamped window was below the minimum floor (the clip was started too recently). */
        TOO_SHORT,
        /** Recording is currently stopped on this server (emergency switch or cloud kill-switch). */
        NO_RECORDING,
        /** The clip queue is saturated (backpressure) or the requester is at their in-flight cap. */
        BUSY,
        /** The target could not be resolved, or clips are otherwise unavailable. */
        UNAVAILABLE
    }
}
