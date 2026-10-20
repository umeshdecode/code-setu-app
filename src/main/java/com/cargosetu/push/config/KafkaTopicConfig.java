package com.cargosetu.push.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
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
