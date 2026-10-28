/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.co.forgevector.replaycore.api.internal.Validation;

/**
 * A request to report that a replay asset's media has finished processing, or
 * has failed.
 *
 * <p>Maps to {@code POST /v1/replay-assets/{id}/finalize}. The call needs an API
 * key with the {@link ApiScope#CLIPS_WRITE} scope and an idempotency key. A ready
 * asset is released at once if its collection's policy releases assets when they
 * are ready. Sending the same key again returns the original
 * {@link ScopeOperation}.
 */
public final class AssetFinalizeRequest {

    private final String idempotencyKey;
    private final ProcessingState state;
    private final String failureCode;

    private AssetFinalizeRequest(String idempotencyKey, ProcessingState state, String failureCode) {
        this.idempotencyKey = idempotencyKey;
        this.state = state;
        this.failureCode = failureCode;
    }

    /**
     * Returns a request that reports the asset's media as ready.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this report
     * @return the request
     * @throws IllegalArgumentException if the key is blank, too long or contains CR, LF or NUL
     */
    public static AssetFinalizeRequest ready(String idempotencyKey) {
        return new AssetFinalizeRequest(Validation.requireIdempotencyKey(idempotencyKey),
                ProcessingState.READY, null);
    }

    /**
     * Returns a request that reports the asset's media as failed.
     *
     * @param idempotencyKey a caller-chosen key, 1 to 200 characters, unique to this report
     * @param failureCode    a short code naming the failure, at most 64 characters
     * @return the request
     * @throws IllegalArgumentException if the key or the code is blank, too long or
     *                                  contains CR, LF or NUL
     */
    public static AssetFinalizeRequest failed(String idempotencyKey, String failureCode) {
        return new AssetFinalizeRequest(Validation.requireIdempotencyKey(idempotencyKey),
                ProcessingState.FAILED, Validation.requireText(failureCode, "failureCode", 64));
    }

    /**
     * Returns the idempotency key the client sends in the {@code Idempotency-Key}
     * header.
     *
     * @return the key; never {@code null}
     */
    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    /**
     * Renders this request as the JSON body map the endpoint expects. Used
     * internally by the client; exposed for testing and transparency.
     *
     * @return an ordered map of body fields (never {@code null})
     */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("processing_state", state.wireValue());
        if (failureCode != null) {
            body.put("failure_code", failureCode);
        }
        return body;
    }
}
