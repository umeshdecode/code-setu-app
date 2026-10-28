package com.cargosetu.push.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.push.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final PushProperties properties;

    @Bean
    public NewTopic pushDeliveryTopic() {
        return TopicBuilder.name(properties.getKafka().getDeliveryTopic())
                .partitions(properties.getKafka().getDeliveryTopicPartitions())
                .replicas(1)
                .build();
    }
}
