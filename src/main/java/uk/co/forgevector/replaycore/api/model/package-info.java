/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

/**
 * Immutable model types mirroring ReplayCore's public API wire shapes.
 *
 * <p>Responses become immutable value objects
 * ({@link uk.co.forgevector.replaycore.api.model.ReplayMetadata},
 * {@link uk.co.forgevector.replaycore.api.model.ReplayPage},
 * {@link uk.co.forgevector.replaycore.api.model.TimelineMarker}); requests are
 * assembled with validating builders
 * ({@link uk.co.forgevector.replaycore.api.model.ReplayQuery},
 * {@link uk.co.forgevector.replaycore.api.model.TimelineEventRequest}). Nullable
 * fields are exposed as {@link java.util.Optional}, and enum-valued fields tolerate
 * unknown future values by mapping them to an {@code UNKNOWN} constant rather than
 * failing to deserialise.
 */
package uk.co.forgevector.replaycore.api.model;
