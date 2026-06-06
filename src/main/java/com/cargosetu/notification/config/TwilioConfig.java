package com.cargosetu.notification.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TwilioConfig {

    @Value("${app.notification.twilio.account-sid}")
    private String accountSid;

    @Value("${app.notification.twilio.auth-token}")
    private String authToken;

    @Value("${app.notification.twilio.mock}")
    private boolean mockEnabled;

    @PostConstruct
    public void initTwilio() {
        if (mockEnabled) {
            log.info("Twilio SMS is configured in MOCK mode. SDK initialization bypassed.");
            return;
        }

        if ("your-twilio-account-sid".equals(accountSid) || accountSid == null || accountSid.isBlank()) {
            log.warn("Twilio Account SID is not set to valid credentials. Physical SMS sending will fail.");
        } else {
            log.info("Initializing Twilio SDK with Account SID: {}", accountSid);
            Twilio.init(accountSid, authToken);
        }
    }
}
