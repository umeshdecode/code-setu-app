package com.cargosetu.push.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.push")
public class PushProperties {

    private final Firebase firebase = new Firebase();
    private final Kafka kafka = new Kafka();
    private int maxDeliveryAttempts = 3;

    @Data
    public static class Firebase {
        /** Base64-encoded service-account JSON. Blank means push sends fail as NOT_CONFIGURED, never crash the app. */
        private String serviceAccountBase64 = "";
    }

    @Data
    public static class Kafka {
        private boolean enabled = true;
        private String deliveryTopic = "cargosetu.push.delivery";
        private int deliveryTopicPartitions = 3;
    }
}
