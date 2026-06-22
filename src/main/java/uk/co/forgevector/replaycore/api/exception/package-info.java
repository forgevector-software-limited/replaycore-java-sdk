/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

/**
 * The SDK's checked exception hierarchy.
 *
 * <p>All failures extend
 * {@link uk.co.forgevector.replaycore.api.exception.ReplayCoreException}. A server
 * error (4xx/5xx) becomes a
 * {@link uk.co.forgevector.replaycore.api.exception.ReplayCoreApiException} or one
 * of its dedicated subtypes
 * ({@link uk.co.forgevector.replaycore.api.exception.AuthenticationException},
 * {@link uk.co.forgevector.replaycore.api.exception.AuthorizationException},
 * {@link uk.co.forgevector.replaycore.api.exception.NotFoundException},
 * {@link uk.co.forgevector.replaycore.api.exception.RateLimitException}); a
 * failure to reach the server becomes a
 * {@link uk.co.forgevector.replaycore.api.exception.ReplayCoreTransportException}.
 */
package uk.co.forgevector.replaycore.api.exception;
