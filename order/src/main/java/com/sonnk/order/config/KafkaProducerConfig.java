package com.sonnk.order.config;

import com.sonnk.order.infrastructure.messaging.SagaTopics;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình Kafka producer cho Order service (Tuần 4 – Kafka intro).
 * JsonSerializer serialize OrderCreatedEvent sang JSON để consumer (product, v.v.) đọc được.
 * NewTopic: đảm bảo topic tồn tại khi app start (broker phải cho phép auto-create hoặc admin tạo topic).
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /** Quản lý topic: đảm bảo topic "order.created" tồn tại khi app start (partitions=2 để scale consumer). */
    @Bean
    public NewTopic orderCreatedTopic(
            @Value("${petboby.order.kafka.topic-order-created:order.created}") String topicName) {
        return TopicBuilder.name(topicName).partitions(2).replicas(1).build();
    }

    /** Topic Saga choreography skeleton (event + state + compensation). */
    @Bean
    public NewTopic sagaOrderCreatedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_ORDER_CREATED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaInventoryReservedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_INVENTORY_RESERVED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaInventoryFailedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_INVENTORY_FAILED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaPaymentRequestedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_PAYMENT_REQUESTED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaPaymentCompletedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_PAYMENT_COMPLETED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaPaymentFailedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_PAYMENT_FAILED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaInventoryReleaseRequestedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_INVENTORY_RELEASE_REQUESTED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaOrderConfirmedTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_ORDER_CONFIRMED).partitions(2).replicas(1).build();
    }

    @Bean
    public NewTopic sagaOrderCancelledTopic() {
        return TopicBuilder.name(SagaTopics.SAGA_ORDER_CANCELLED).partitions(2).replicas(1).build();
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
