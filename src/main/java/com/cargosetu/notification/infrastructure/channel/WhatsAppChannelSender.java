package com.cargosetu.notification.infrastructure.channel;

import com.cargosetu.notification.model.NotificationChannel;
import com.cargosetu.notification.model.NotificationAudit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppChannelSender implements NotificationChannelSender {

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WHATSAPP;
    }

    @Override
    public void send(NotificationAudit audit) throws Exception {
        String recipient = audit.getRecipient();
        int currentAttempt = audit.getRetryCount();

        log.info("[WhatsApp Channel] Sending WhatsApp message to: {} (Attempt #{})", recipient, currentAttempt);
        
        // Mock WhatsApp sending
        log.info("[MOCK WHATSAPP] Dispatching WhatsApp to: {}. Message: '{}'", recipient, audit.getMessageBody());
        Thread.sleep(250); // Simulate network latency
        
        log.info("[WhatsApp Channel] WhatsApp message sent successfully to: {}", recipient);
    }
}
