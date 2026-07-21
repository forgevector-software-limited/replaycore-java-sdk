/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * Programmatic surface a server owner's plugin can call to tag a custom timeline event onto the live
 * recording from code, the in-process Dev API for "mark this moment" (a kill, a round start, an objective
 * taken, a border shrink). The event appears on the in-browser viewer's scrubber, so an owner can jump
 * straight to it. It is the in-process twin of the cloud REST timeline-event write (which external tools
 * use with an API key); a plugin running on the recording server should use this, with no network
 * round-trip and no key, tagging the live recording directly.
 *
 * <p>Obtain the instance from the running recorder, either through the umbrella
 * {@link ReplayCoreApi#timeline()} or directly from Bukkit's services manager (registered whenever the
 * recorder is running):
 * <pre>{@code
 * RegisteredServiceProvider<ReplayCoreTimelineApi> rsp =
 *     getServer().getServicesManager().getRegistration(ReplayCoreTimelineApi.class);
 * if (rsp != null) {
 *     rsp.getProvider().tagTimelineEvent(IntegrationBookmark.builder("MyGameMode", "objective")
 *         .severity(IntegrationBookmark.Severity.WARNING)
 *         .player(capturerUuid, capturerName)
 *         .arenaId(arenaId)
 *         .message("Captured the flag")
 *         .build());
 * }
 * }</pre>
 *
 * <p>Non-blocking and main-thread-safe: it buffers the bookmark onto the current tick of the live
 * recording and returns whether it was accepted ({@code false} when recording is inactive or the per-tick
 * bookmark buffer is momentarily full; the recorder never blocks the server to record a bookmark). The
 * bookmark fields are bounded and sanitised by {@link IntegrationBookmark} (source and type required,
 * at most 128 characters each, at most 16 metadata entries). Both recorder lanes (modern 1.21 and above,
 * and legacy 1.8.8) register the identical service and emit the same byte-format the bundled adapters
 * already use, so the viewer renders a plugin's custom events exactly like the built-in ones.
 *
 * <p>Note: a bookmark whose {@code type} is {@code arena_start}, {@code arena_end}, {@code duel_start} or
 * {@code duel_end} also drives session rotation in match mode, the same gate the bundled BedWars1058,
 * MBedwars and Duels adapters use, so a game-mode plugin can mark one of those four types to seal a
 * per-match archive. {@code category_start} and {@code category_end} are recorded onto the timeline like
 * any other bookmark but do not drive rotation through this method: the built-in category chat-trigger
 * designer rotates through a separate internal path that can see how many matching categories are
 * currently running, which a tagged bookmark cannot express.
 *
 * <p>Session rotation cuts the physical recording, so it cannot represent two matches running at the same
 * time on one server. For that, open a logical match scope instead of relying on rotation: see
 * {@link ReplayCoreMatchApi}, reached from the umbrella {@link ReplayCoreApi#matches()}, which is a
 * tick-window over the one continuous recording, so beginning or ending a scope never starts, stops or cuts
 * it.
 *
 * <p>Forward-looking contract: see the package documentation for status.
 */
public interface ReplayCoreTimelineApi {

    /**
     * Tags {@code bookmark} onto the live recording's timeline at the current tick.
     *
     * <p><strong>This marker is not addressed to any match.</strong> It is written onto the one shared
     * timeline of the recording itself, which is unambiguous when one match is in progress and is not when
     * several are. {@link IntegrationBookmark#arenaId()} does not disambiguate it: the recorder stores that
     * field as descriptive metadata and never routes on it. If this server runs concurrent matches and a
     * marker needs to belong to exactly one of them, use
     * {@link ReplayCoreMatchApi#tagScopeEvent(String, IntegrationBookmark)} with the scope id
     * {@link ReplayCoreMatchApi#beginScope} returned for that match, which is routed rather than shared.
     *
     * @param bookmark the event to record; must not be {@code null} (its required {@code source} and
     *                 {@code type} are validated when the {@link IntegrationBookmark} is built)
     * @return {@code true} if the bookmark was accepted onto the recording; {@code false} if recording is
     *         currently inactive (emergency switch or cloud kill-switch) or the per-tick bookmark buffer
     *         was momentarily full
     */
    boolean tagTimelineEvent(IntegrationBookmark bookmark);
}
