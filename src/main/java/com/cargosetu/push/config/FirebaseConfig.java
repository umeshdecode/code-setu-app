package com.cargosetu.push.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Initializes the Firebase Admin SDK from a base64-encoded service-account JSON
 * supplied only via an env var (never committed). If it is missing, this logs a
 * warning and leaves Firebase uninitialized - FcmPushSender checks that and fails
 * a delivery attempt cleanly instead of the app failing to boot, matching how the
 * rest of this codebase degrades gracefully when optional third-party config is absent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseConfig {

    private final PushProperties properties;

    @PostConstruct
    public void init() {
        String encoded = properties.getFirebase().getServiceAccountBase64();
        if (encoded == null || encoded.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_B64 is not set - push notifications will report NOT_CONFIGURED.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encoded);
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized for push notifications.");
        } catch (Exception e) {
            // Never log the decoded credential - only that initialization failed.
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }
}
