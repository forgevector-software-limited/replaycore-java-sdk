/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Optional;

/** Bukkit-free captured item identity and optional bounded component/SNBT state. */
public final class ExternalSubjectItem {
    public static final int MAX_ITEM_KEY_LENGTH = 256;
    public static final int MAX_ENCODED_STATE_LENGTH = 32768;

    private final String itemKey;
    private final int count;
    private final String encodedState;

    public ExternalSubjectItem(String itemKey, int count, String encodedState) {
        if (itemKey == null || itemKey.trim().isEmpty()) throw new IllegalArgumentException("itemKey must not be empty");
        String trimmed = itemKey.trim();
        if (trimmed.length() > MAX_ITEM_KEY_LENGTH) throw new IllegalArgumentException("itemKey is too long");
        if (count < 1 || count > 127) throw new IllegalArgumentException("count must be between 1 and 127");
        if (encodedState != null && encodedState.length() > MAX_ENCODED_STATE_LENGTH) {
            throw new IllegalArgumentException("encodedState is too long");
        }
        this.itemKey = trimmed;
        this.count = count;
        this.encodedState = encodedState;
    }

    public String itemKey() { return itemKey; }
    public int count() { return count; }
    public Optional<String> encodedState() { return Optional.ofNullable(encodedState); }
}
