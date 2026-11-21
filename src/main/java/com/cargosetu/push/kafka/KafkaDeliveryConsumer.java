package com.cargosetu.push.kafka;

import com.cargosetu.push.config.PushProperties;
import com.cargosetu.push.model.PushDeliveryAttempt;
import com.cargosetu.push.model.PushDeliveryStatus;
import com.cargosetu.push.repository.PushDeliveryAttemptRepository;
import com.cargosetu.push.service.FcmPushSender;
import com.cargosetu.push.service.PushTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Does the actual FCM network call per target device, decoupled from the
 * business transaction by Kafka. Retries transient failures a bounded number
 * of times by re-publishing with an incremented attempt count - never an
 * infinite loop (plan §13 "Transient FCM failure"), and never retries a
 * terminal token error.
 *
 * This decouples the fast "resolve tokens and enqueue" step from the slower
 * FCM network calls for throughput; it is not a transactional outbox - a
 * crash between the DB commit and the Kafka publish (in the existing
 * AFTER_COMMIT async listener) still loses that one notification, the same
 * risk the current Email/SMS pipeline already accepts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.push.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaDeliveryConsumer {

    private final FcmPushSender fcmPushSender;
    private final PushTokenService pushTokenService;
    private final PushDeliveryAttemptRepository attemptRepository;
    private final KafkaProducer producer;
    private final PushProperties properties;

    @KafkaListener(topics = "${app.push.kafka.delivery-topic}", groupId = "cargosetu-push")
    public void onMessage(KafkaDeliveryMessage message) {
        FcmPushSender.Result result = fcmPushSender.send(message.fcmToken(), message.title(), message.body(), message.data());

        PushDeliveryAttempt attempt = new PushDeliveryAttempt();
        attempt.setAuditId(message.auditId());
        attempt.setTokenRecordId(message.tokenRecordId());
        attempt.setAttemptCount(message.attempt());
        attempt.setAttemptedAt(LocalDateTime.now());

        switch (result.outcome()) {
            case SENT -> {
                attempt.setStatus(PushDeliveryStatus.SENT);
                attempt.setProviderMessageId(result.providerMessageId());
                attemptRepository.save(attempt);
            }
            case TOKEN_INVALID -> {
                attempt.setStatus(PushDeliveryStatus.TOKEN_INVALID);
                attempt.setErrorCode(result.errorCode());
                attemptRepository.save(attempt);
                pushTokenService.deactivateByFcmToken(message.fcmToken());
            }
            case NOT_CONFIGURED -> {
                attempt.setStatus(PushDeliveryStatus.NOT_CONFIGURED);
                attempt.setErrorCode(result.errorCode());
                attemptRepository.save(attempt);
            }
            case TRANSIENT_FAILURE -> {
                if (message.attempt() < properties.getMaxDeliveryAttempts()) {
                    attempt.setStatus(PushDeliveryStatus.RETRYING);
                    attempt.setErrorCode(result.errorCode());
                    attemptRepository.save(attempt);
                    producer.publish(message.withAttempt(message.attempt() + 1));
                } else {
                    log.warn("Push delivery to token record {} exhausted {} attempts", message.tokenRecordId(), message.attempt());
                    attempt.setStatus(PushDeliveryStatus.FAILED);
                    attempt.setErrorCode(result.errorCode());
                    attemptRepository.save(attempt);
                }
            }
        }
    }
}
