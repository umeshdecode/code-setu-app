package com.cargosetu.maps.client;

import lombok.Getter;

/**
 * Sanitized provider failure. The message is safe to log; it never contains the
 * API key or the raw provider payload.
 */
@Getter
public class MapsProviderException extends RuntimeException {

    public enum Reason {
        NOT_CONFIGURED,
        INVALID_REQUEST,
        ACCESS_DENIED,
        RATE_LIMITED,
        TIMEOUT,
        UNAVAILABLE
    }

    private final Reason reason;
    private final Integer providerStatus;

    public MapsProviderException(Reason reason, Integer providerStatus, String message) {
        super(message);
        this.reason = reason;
        this.providerStatus = providerStatus;
    }

    public boolean isTransient() {
        return reason == Reason.TIMEOUT || reason == Reason.UNAVAILABLE;
    }
}
