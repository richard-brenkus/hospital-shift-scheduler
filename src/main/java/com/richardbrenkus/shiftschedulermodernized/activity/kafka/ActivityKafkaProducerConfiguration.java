package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(ActivityKafkaProperties.class)
@ConditionalOnProperty(
        prefix = "application.kafka.activity",
        name = "enabled",
        havingValue = "true"
)
public class ActivityKafkaProducerConfiguration {

    @Bean
    public ProducerFactory<String, ActivityKafkaMessage> activityProducerFactory(
            ActivityKafkaProperties properties,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> producerProperties = new HashMap<>();

        producerProperties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.bootstrapServers()
        );
        producerProperties.put(
                ProducerConfig.CLIENT_ID_CONFIG,
                properties.clientId()
        );
        producerProperties.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );
        producerProperties.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );
        producerProperties.put(
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
                5
        );
        producerProperties.put(
                ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG,
                30_000
        );
        producerProperties.put(
                ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG,
                10_000
        );

        JsonSerializer<ActivityKafkaMessage> valueSerializer =
                new JsonSerializer<>(objectMapper);

        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(
                producerProperties,
                new StringSerializer(),
                valueSerializer
        );
    }

    @Bean
    public KafkaTemplate<String, ActivityKafkaMessage> activityKafkaTemplate(
            @Qualifier("activityProducerFactory")
            ProducerFactory<String, ActivityKafkaMessage> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
