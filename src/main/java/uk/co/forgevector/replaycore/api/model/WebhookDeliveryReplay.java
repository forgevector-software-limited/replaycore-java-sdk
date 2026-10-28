/*
 * Copyright (c) ForgeVector Software Limited. All rights reserved.
 * Author: William Bowyer / ForgeVector Software Limited.
 */

package uk.co.forgevector.replaycore.api.model;

import java.util.Objects;

/**
 * The outcome of replaying a webhook delivery.
 *
 * Immutable.
 */
public final class WebhookDeliveryReplay {

    private final String deliveryId;
    private final String status;

    /**
     * Constructs a webhook delivery replay. Intended for internal deserialisation and tests.
     *
     * @param deliveryId the id of the delivery that was replayed
     * @param status the delivery status after the replay (for example {@code "delivered"})
     */
    public WebhookDeliveryReplay(String deliveryId, String status) {
        this.deliveryId = deliveryId != null ? deliveryId : "";
        this.status = status != null ? status : "";
    }

    /**
     * Returns the id of the delivery that was replayed.
     *
     * @return the id of the delivery that was replayed; never {@code null}
     */
    public String getDeliveryId() {
        return deliveryId;
    }

    /**
     * Returns the delivery status after the replay (for example {@code "delivered"}).
     *
     * @return the delivery status after the replay (for example {@code "delivered"}); never {@code
     *         null}
     */
    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebhookDeliveryReplay)) {
            return false;
        }
        WebhookDeliveryReplay that = (WebhookDeliveryReplay) o;
        return Objects.equals(deliveryId, that.deliveryId)
                && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deliveryId, status);
    }

    @Override
    public String toString() {
        return "WebhookDeliveryReplay{deliveryId=" + deliveryId
                + ", status=" + status + '}';
    }
}
