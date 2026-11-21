package com.cargosetu.push.infrastructure;

import com.cargosetu.notification.infrastructure.channel.NotificationChannelSender;
import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.push.kafka.KafkaDeliveryMessage;
import com.cargosetu.push.kafka.KafkaProducer;
import com.cargosetu.push.model.UserPushToken;
import com.cargosetu.push.repository.UserPushTokenRepository;
import com.cargosetu.push.util.PrincipalRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The PUSH leg of the existing notification pipeline. "Recipient" for this
 * channel is a "STAFF:12" / "CUSTOMER:7" key (see PrincipalRef), not an email
 * or phone number - NotificationProcessingServiceImpl, the audit table and the
 * AFTER_COMMIT listener are otherwise completely unchanged and shared with
 * Email/SMS.
 *
 * This sender's job ends at "resolve the account's active devices and hand
 * each one to Kafka" - the actual FCM call, per-device retry and per-device
 * audit happen in PushDeliveryConsumer, decoupled for throughput.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushChannelSender implements NotificationChannelSender {

    private final UserPushTokenRepository tokenRepository;
    private final KafkaProducer producer;
    private final NotificationAuditRepository auditRepository;

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }

    @Override
    public void send(NotificationAudit audit) {
        audit.setStatus(NotificationStatus.PROCESSING);
        auditRepository.save(audit);

        try {
            PrincipalRef principal = PrincipalRef.parse(audit.getRecipient());
            List<UserPushToken> tokens = tokenRepository
                    .findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(principal.type(), principal.id());

            if (tokens.isEmpty()) {
                log.info("No active push devices for {} - nothing to send.", audit.getRecipient());
            } else {
                Map<String, String> data = Map.of("type", audit.getTemplateCode());
                for (UserPushToken token : tokens) {
                    producer.publish(new KafkaDeliveryMessage(
                            audit.getId(), token.getId(), token.getFcmToken(),
                            audit.getSubject(), audit.getMessageBody(), data, 1));
                }
                log.info("Enqueued push delivery to {} device(s) for {}", tokens.size(), audit.getRecipient());
            }

            // "Sent" here means "handed to the delivery pipeline" - per-device outcomes
            // land in push_delivery_attempt, not on this shared audit row.
            audit.setStatus(NotificationStatus.SENT);
            audit.setSentAt(LocalDateTime.now());
            auditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to dispatch push notification for {}: {}", audit.getRecipient(), e.getMessage(), e);
            audit.setStatus(NotificationStatus.FAILED);
            audit.setFailureReason(e.getMessage());
            auditRepository.save(audit);
        }
    }
}
