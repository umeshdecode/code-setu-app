package com.cargosetu.push;

import com.cargosetu.notification.model.NotificationAudit;
import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationStatus;
import com.cargosetu.notification.repository.NotificationAuditRepository;
import com.cargosetu.push.infrastructure.PushChannelSender;
import com.cargosetu.push.kafka.KafkaDeliveryMessage;
import com.cargosetu.push.kafka.KafkaProducer;
import com.cargosetu.push.model.Platform;
import com.cargosetu.push.model.PrincipalType;
import com.cargosetu.push.model.UserPushToken;
import com.cargosetu.push.repository.UserPushTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers plan §13: "one user, three devices" fans out to exactly those three
 * tokens; a different user's tokens are never touched; a user with no active
 * devices sends nothing and is not treated as an error.
 */
@ExtendWith(MockitoExtension.class)
class PushChannelSenderTest {

    @Mock private UserPushTokenRepository tokenRepository;
    @Mock private KafkaProducer producer;
    @Mock private NotificationAuditRepository auditRepository;

    private PushChannelSender sender;

    @BeforeEach
    void setUp() {
        sender = new PushChannelSender(tokenRepository, producer, auditRepository);
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private UserPushToken token(long id, String fcmToken) {
        UserPushToken t = new UserPushToken();
        t.setId(id);
        t.setFcmToken(fcmToken);
        t.setPlatform(Platform.WEB);
        return t;
    }

    private NotificationAudit audit(long id, String recipient) {
        NotificationAudit a = new NotificationAudit();
        a.setId(id);
        a.setEventId(UUID.randomUUID());
        a.setRecipient(recipient);
        a.setChannel(NotificationChannel.PUSH);
        a.setTemplateCode("TRIP_ASSIGNED_STAFF");
        a.setSubject("Trip assigned");
        a.setMessageBody("Body");
        a.setStatus(NotificationStatus.PENDING);
        a.setRetryCount(0);
        return a;
    }

    @Test
    void fansOutToEveryActiveDeviceOfThatPrincipalOnly() {
        when(tokenRepository.findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(PrincipalType.STAFF, 105L))
                .thenReturn(List.of(token(1, "t1"), token(2, "t2"), token(3, "t3")));

        NotificationAudit audit = audit(10, "STAFF:105");
        sender.send(audit);

        ArgumentCaptor<KafkaDeliveryMessage> captor = ArgumentCaptor.forClass(KafkaDeliveryMessage.class);
        verify(producer, times(3)).publish(captor.capture());
        assertThat(captor.getAllValues()).extracting(KafkaDeliveryMessage::tokenRecordId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(captor.getAllValues()).allMatch(m -> m.auditId().equals(10L) && m.attempt() == 1);
        assertThat(audit.getStatus()).isEqualTo(NotificationStatus.SENT);

        verify(tokenRepository, never()).findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(eq(PrincipalType.CUSTOMER), any());
    }

    @Test
    void noActiveDevicesIsNotAFailure() {
        when(tokenRepository.findByPrincipalTypeAndPrincipalIdAndIsActiveTrue(PrincipalType.STAFF, 205L))
                .thenReturn(List.of());

        NotificationAudit audit = audit(11, "STAFF:205");
        sender.send(audit);

        verifyNoInteractions(producer);
        assertThat(audit.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void malformedRecipientKeyFailsTheAuditInsteadOfThrowing() {
        NotificationAudit audit = audit(12, "not-a-valid-key");

        sender.send(audit);

        assertThat(audit.getStatus()).isEqualTo(NotificationStatus.FAILED);
        verifyNoInteractions(producer);
    }
}
