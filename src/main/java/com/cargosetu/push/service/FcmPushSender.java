package com.cargosetu.push.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class FcmPushSender {

    public enum Outcome { SENT, TOKEN_INVALID, TRANSIENT_FAILURE, NOT_CONFIGURED }

    public record Result(Outcome outcome, String providerMessageId, String errorCode) {
    }

    /**
     * Minimal payload: title/body plus a small data map (e.g. type + entityId) so
     * the client can navigate on tap without putting business details in
     * lock-screen text (plan §10).
     */
    public Result send(String fcmToken, String title, String body, Map<String, String> data) {
        if (FirebaseApp.getApps().isEmpty()) {
            return new Result(Outcome.NOT_CONFIGURED, null, "FIREBASE_NOT_CONFIGURED");
        }
        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Map.of() : data)
                .build();
        try {
            String providerMessageId = FirebaseMessaging.getInstance().send(message);
            return new Result(Outcome.SENT, providerMessageId, null);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (isTerminal(code)) {
                log.info("FCM reported a terminal error for a token: {}", code);
                return new Result(Outcome.TOKEN_INVALID, null, String.valueOf(code));
            }
            log.warn("FCM reported a transient error: {}", code);
            return new Result(Outcome.TRANSIENT_FAILURE, null, String.valueOf(code));
        }
    }

    private static boolean isTerminal(MessagingErrorCode code) {
        return code == MessagingErrorCode.UNREGISTERED
                || code == MessagingErrorCode.INVALID_ARGUMENT
                || code == MessagingErrorCode.SENDER_ID_MISMATCH;
    }
}
