package com.cargosetu.push.kafka;

import java.util.Map;

/**
 * One target device for one logical notification. Published once per active
 * token so the consumer's FCM call, retry and audit stay single-token scoped -
 * one device failing never affects another (plan §6/§13).
 */
public record KafkaDeliveryMessage(
        Long auditId,
        Long tokenRecordId,
        String fcmToken,
        String title,
        String body,
        Map<String, String> data,
        int attempt
) {
    public KafkaDeliveryMessage withAttempt(int newAttempt) {
        return new KafkaDeliveryMessage(auditId, tokenRecordId, fcmToken, title, body, data, newAttempt);
    }
}
