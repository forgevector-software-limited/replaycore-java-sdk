/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

/**
 * The REST client for ReplayCore's public developer API.
 *
 * <p>Start at {@link uk.co.forgevector.replaycore.api.client.ReplayCoreClient}:
 * configure it with {@link uk.co.forgevector.replaycore.api.client.ReplayCoreClient#builder()},
 * supplying your server-owner-scoped API key, then call the typed methods to list
 * and read replays and to write timeline markers. For non-blocking use (such as
 * inside a Minecraft plugin) build a
 * {@link uk.co.forgevector.replaycore.api.client.ReplayCoreAsyncClient} instead.
 *
 * <p>The client is tenant-scoped by construction: the API key the server resolves
 * decides which tenant every request runs against, and the SDK provides no way to
 * cross that boundary.
 */
package uk.co.forgevector.replaycore.api.client;
