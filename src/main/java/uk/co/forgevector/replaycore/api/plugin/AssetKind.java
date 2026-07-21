/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * What a {@code ReplayAsset} represents: a logical, playable window over a collection's segments.
 *
 * <p>An asset is never a duplicated archive; it is a window definition resolved against the parent
 * collection's segments at playback time. Two assets of different kinds (for example a
 * {@link #FULL_MATCH} and an {@link #EVENT_CLIP} covering the same moments) may overlap freely without any
 * bytes being copied.
 *
 * <p><strong>Reachability.</strong> No method on {@link ReplayCoreMatchApi}, or anywhere else in this
 * package, currently accepts or returns this type, so a plugin using only the published in-process
 * surface cannot construct or observe one. It is here as the shared vocabulary of the REST network
 * integration API. Read it as reference values for the {@code kind} field of that API's asset-creation
 * endpoint, and do not write an in-process code path that expects to be handed one. Note that
 * {@link #FULL_MATCH} is produced only by finalising a collection and is rejected if sent to the
 * asset-creation endpoint.
 */
public enum AssetKind {
    /** The whole collection, start to end, across every contributing segment. */
    FULL_MATCH,
    /** A short window around a recorder-recognised in-game event; see {@link EventKind}. */
    EVENT_CLIP,
    /** A window an operator or integration chose explicitly, outside any recognised event. */
    CUSTOM_CLIP,
    /** An operator- or automation-curated highlight window. */
    HIGHLIGHT
}
