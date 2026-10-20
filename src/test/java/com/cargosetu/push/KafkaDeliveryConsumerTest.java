package com.cargosetu.push;

import com.cargosetu.push.config.PushProperties;
import com.cargosetu.push.kafka.KafkaDeliveryConsumer;
import com.cargosetu.push.kafka.KafkaDeliveryMessage;
import com.cargosetu.push.kafka.KafkaProducer;
import com.cargosetu.push.model.PushDeliveryAttempt;
import com.cargosetu.push.model.PushDeliveryStatus;
import com.cargosetu.push.repository.PushDeliveryAttemptRepository;
import com.cargosetu.push.service.FcmPushSender;
import com.cargosetu.push.service.PushTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers plan §13: an invalid/unregistered token is deactivated and does not
 * affect other devices; a transient failure retries a bounded number of times
 * and then stops (no infinite loop); a missing Firebase config fails cleanly.
 */
@ExtendWith(MockitoExtension.class)
class KafkaDeliveryConsumerTest {

    @Mock private FcmPushSender fcmPushSender;
    @Mock private PushTokenService pushTokenService;
    @Mock private PushDeliveryAttemptRepository attemptRepository;
    @Mock private KafkaProducer producer;

    private PushProperties properties;
    private KafkaDeliveryConsumer consumer;

    @BeforeEach
    void setUp() {
        properties = new PushProperties();
        properties.setMaxDeliveryAttempts(3);
        consumer = new KafkaDeliveryConsumer(fcmPushSender, pushTokenService, attemptRepository, producer, properties);
        when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private KafkaDeliveryMessage message(int attempt) {
        return new KafkaDeliveryMessage(10L, 1L, "fcm-token", "Title", "Body", Map.of(), attempt);
    }

    @Test
    void successWritesOneSentAttemptAndDoesNotTouchTheToken() {
        when(fcmPushSender.send(eq("fcm-token"), any(), any(), any()))
                .thenReturn(new FcmPushSender.Result(FcmPushSender.Outcome.SENT, "msg-1", null));

        consumer.onMessage(message(1));

        ArgumentCaptor<PushDeliveryAttempt> captor = ArgumentCaptor.forClass(PushDeliveryAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PushDeliveryStatus.SENT);
        verifyNoInteractions(pushTokenService);
        verifyNoInteractions(producer);
    }

    @Test
    void invalidTokenIsDeactivatedAndMarkedTerminalNotRetried() {
        when(fcmPushSender.send(any(), any(), any(), any()))
                .thenReturn(new FcmPushSender.Result(FcmPushSender.Outcome.TOKEN_INVALID, null, "UNREGISTERED"));

        consumer.onMessage(message(1));

        ArgumentCaptor<PushDeliveryAttempt> captor = ArgumentCaptor.forClass(PushDeliveryAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PushDeliveryStatus.TOKEN_INVALID);
        verify(pushTokenService).deactivateByFcmToken("fcm-token");
        verifyNoInteractions(producer); // never retried
    }

    @Test
    void transientFailureRetriesThenStopsAtTheBound() {
        when(fcmPushSender.send(any(), any(), any(), any()))
                .thenReturn(new FcmPushSender.Result(FcmPushSender.Outcome.TRANSIENT_FAILURE, null, "UNAVAILABLE"));

        consumer.onMessage(message(1));
        verify(producer).publish(message(2));

        consumer.onMessage(message(2));
        verify(producer).publish(message(3));

        // Attempt 3 is the configured max - no further republish, terminal FAILED instead.
        consumer.onMessage(message(3));
        verify(producer, times(2)).publish(any());
        verifyNoInteractions(pushTokenService);

        ArgumentCaptor<PushDeliveryAttempt> captor = ArgumentCaptor.forClass(PushDeliveryAttempt.class);
        verify(attemptRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(PushDeliveryStatus.FAILED);
    }

    @Test
    void missingFirebaseConfigFailsCleanlyWithoutRetrying() {
        when(fcmPushSender.send(any(), any(), any(), any()))
                .thenReturn(new FcmPushSender.Result(FcmPushSender.Outcome.NOT_CONFIGURED, null, "FIREBASE_NOT_CONFIGURED"));

        consumer.onMessage(message(1));

        ArgumentCaptor<PushDeliveryAttempt> captor = ArgumentCaptor.forClass(PushDeliveryAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PushDeliveryStatus.NOT_CONFIGURED);
        verifyNoInteractions(producer, pushTokenService);
    }
}
