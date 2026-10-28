/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * One value of a catalogue facet and the number of collections that carry it.
 *
 * Immutable. Every facet uses the same shape: boolean facets report {@code "true"} and {@code
 * "false"}, and the team-size facet reports integers as text.
 */
public final class FacetCount {

    private final String value;
    private final long count;

    /**
     * Constructs a facet count. Intended for internal deserialisation and tests.
     *
     * @param value the facet value
     * @param count the number of matching rows
     */
    public FacetCount(String value, long count) {
        this.value = value != null ? value : "";
        this.count = count;
    }

    /**
     * Returns the facet value.
     *
     * @return the facet value; never {@code null}
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the number of matching rows.
     *
     * @return the number of matching rows
     */
    public long getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FacetCount)) {
            return false;
        }
        FacetCount that = (FacetCount) o;
        return Objects.equals(value, that.value)
                && count == that.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, count);
    }

    @Override
    public String toString() {
        return "FacetCount{value=" + value
                + ", count=" + count + '}';
    }
}
