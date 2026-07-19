/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;

/**
 * A handle to a logical match scope opened with {@link ReplayCoreMatchApi#beginScope}: a tick-window over
 * the one continuous recording, independent of any other scope open at the same time.
 *
 * <p>{@link #scopeId()} is the value to pass back into {@link ReplayCoreMatchApi#updateScope} and
 * {@link ReplayCoreMatchApi#endScope}; {@link #collectionId()} is the id of the logical
 * {@code ReplayCollection} this scope backs, the join key an integration uses against the REST catalogue
 * once the collection is queryable there. {@link #externalMatchId()} echoes back the id the caller supplied
 * at {@link BeginScopeRequest#externalMatchId()}. {@link #startTick()} is the tick, on the shared
 * recording, that this scope began at.
 *
 * <p>This is a read-only snapshot, not a live view: {@link #state()} reflects the collection's processing
 * state at the moment the scope was returned or last observed, not a value that updates itself. Opening,
 * updating and ending a scope never starts, stops, rotates or cuts the physical recording; only the logical
 * window this handle describes changes.
 *
 * <p>Immutable and thread-safe.
 */
public final class ReplayScope {

    private final String scopeId;
    private final String collectionId;
    private final String externalMatchId;
    private final long startTick;
    private final ProcessingState state;

    /**
     * Creates a scope handle.
     *
     * @param scopeId         the scope id; must not be {@code null}
     * @param collectionId    the backing collection's id; must not be {@code null}
     * @param externalMatchId the external match id the scope was opened with; must not be {@code null}
     * @param startTick       the tick, on the shared recording, that this scope began at
     * @param state           the collection's processing state at the moment of this snapshot; must not be
     *                        {@code null}
     */
    public ReplayScope(String scopeId, String collectionId, String externalMatchId, long startTick,
                        ProcessingState state) {
        this.scopeId = Objects.requireNonNull(scopeId, "scopeId must not be null");
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId must not be null");
        this.externalMatchId = Objects.requireNonNull(externalMatchId, "externalMatchId must not be null");
        this.startTick = startTick;
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    /** @return the scope id to pass to {@link ReplayCoreMatchApi#updateScope} and {@code #endScope}; never {@code null} */
    public String scopeId() { return scopeId; }
    /** @return the backing collection's id; never {@code null} */
    public String collectionId() { return collectionId; }
    /** @return the external match id this scope was opened with; never {@code null} */
    public String externalMatchId() { return externalMatchId; }
    /** @return the tick, on the shared recording, that this scope began at */
    public long startTick() { return startTick; }
    /** @return the collection's processing state at the moment of this snapshot; never {@code null} */
    public ProcessingState state() { return state; }
}
