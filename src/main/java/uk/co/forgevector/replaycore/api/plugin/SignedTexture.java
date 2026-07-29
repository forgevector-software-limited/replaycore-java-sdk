/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.plugin;

import java.util.Objects;

/** Captured Mojang texture property value and signature. No live profile lookup is implied. */
public final class SignedTexture {
    public static final int MAX_VALUE_LENGTH = 32768;
    public static final int MAX_SIGNATURE_LENGTH = 8192;

    private final String value;
    private final String signature;

    public SignedTexture(String value, String signature) {
        this.value = required(value, "value", MAX_VALUE_LENGTH);
        this.signature = required(signature, "signature", MAX_SIGNATURE_LENGTH);
    }

    public String value() { return value; }
    public String signature() { return signature; }

    private static String required(String value, String name, int maximum) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (value.length() > maximum) {
            throw new IllegalArgumentException(name + " must be at most " + maximum + " characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new IllegalArgumentException(name + " must not contain control characters");
            }
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SignedTexture)) return false;
        SignedTexture that = (SignedTexture) other;
        return value.equals(that.value) && signature.equals(that.signature);
    }

    @Override
    public int hashCode() { return Objects.hash(value, signature); }
}
