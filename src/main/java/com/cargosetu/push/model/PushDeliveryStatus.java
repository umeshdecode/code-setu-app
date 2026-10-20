package com.cargosetu.push.model;

public enum PushDeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    RETRYING,
    /** Firebase rejected the token itself (unregistered/sender mismatch) - terminal, no retry. */
    TOKEN_INVALID,
    /** No Firebase service-account configured - terminal for this attempt. */
    NOT_CONFIGURED
}
