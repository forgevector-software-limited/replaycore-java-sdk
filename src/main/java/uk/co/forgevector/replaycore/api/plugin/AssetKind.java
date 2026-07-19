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
