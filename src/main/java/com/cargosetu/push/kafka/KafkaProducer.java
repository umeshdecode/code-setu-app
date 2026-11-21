package com.cargosetu.push.kafka;

import com.cargosetu.push.config.PushProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, KafkaDeliveryMessage> kafkaTemplate;
    private final PushProperties properties;

    /** Keyed by token record id so retries/redeliveries for the same device stay ordered. */
    public void publish(KafkaDeliveryMessage message) {
        kafkaTemplate.send(properties.getKafka().getDeliveryTopic(), String.valueOf(message.tokenRecordId()), message);
    }
}
