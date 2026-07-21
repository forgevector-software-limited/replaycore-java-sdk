/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One page of results from {@link ReplayCatalogApi#listForPlayer}, mirroring the
 * {@code results}/{@code next_page_token} envelope every RFC-0009 list endpoint returns.
 *
 * <p>{@link #entries()} is already filtered and ordered by the cloud (newest first, embargo-gated); nothing
 * here re-filters or re-sorts it. Walk every page by re-issuing {@link ReplayCatalogApi#listForPlayer} with
 * {@link ReplayCatalogQuery.Builder#cursor(String)} set to {@link #nextCursor()} until it is absent.
 *
 * <p>Immutable and thread-safe.
 */
public final class ReplayCatalogPage {

    private final List<ReplayCatalogEntry> entries;
    private final String nextCursor;

    /**
     * @param entries    this page's entries, in the order returned by the cloud; must not be {@code null},
     *                   may be empty
     * @param nextCursor the opaque cursor to pass to {@link ReplayCatalogQuery.Builder#cursor(String)} for
     *                   the next page, or {@code null}/blank when this is the last page
     */
    public ReplayCatalogPage(List<ReplayCatalogEntry> entries, String nextCursor) {
        Objects.requireNonNull(entries, "entries must not be null");
        this.entries = Collections.unmodifiableList(new ArrayList<ReplayCatalogEntry>(entries));
        this.nextCursor = nextCursor == null || nextCursor.trim().isEmpty() ? null : nextCursor;
    }

    /** @return this page's entries, in the order returned by the cloud; never {@code null}, possibly empty */
    public List<ReplayCatalogEntry> entries() { return entries; }

    /** @return the cursor for the next page, or an empty optional when this is the last page */
    public Optional<String> nextCursor() { return Optional.ofNullable(nextCursor); }

    /** @return {@code true} if {@link #nextCursor()} is present, i.e. at least one more page is available */
    public boolean hasMore() { return nextCursor != null; }
}
