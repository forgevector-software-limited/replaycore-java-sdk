/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

/**
 * The kind of in-game moment an {@link AssetKind#EVENT_CLIP} was produced from.
 *
 * <p>Applies only to event clips; a {@link AssetKind#FULL_MATCH}, {@link AssetKind#CUSTOM_CLIP} or
 * {@link AssetKind#HIGHLIGHT} asset carries no event kind. {@link #CUSTOM} covers any moment an
 * integration recognises that does not fit the four named kinds, so a network's own event taxonomy never
 * needs a new enum constant here; tag the specific meaning through namespaced metadata instead.
 */
public enum EventKind {
    /** The moment a participant eliminated another. */
    KILL,
    /** The moment a participant was eliminated. */
    DEATH,
    /** The moment a round or phase concluded. */
    ROUND_END,
    /** The moment a match, round or phase was won. */
    WIN,
    /** A moment outside the four named kinds; the specific meaning belongs in namespaced metadata. */
    CUSTOM
}
