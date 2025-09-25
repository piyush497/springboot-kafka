package com.courier.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${kafka.topics.incoming-orders}")
    private String incomingOrdersTopic;
    
    @Value("${kafka.topics.abc-transport}")
    private String abcTransportTopic;
    
    @Value("${kafka.topics.tracking-events}")
    private String trackingEventsTopic;
    
    @Value("${kafka.topics.abc-transport-responses:abc-transport-responses}")
    private String abcTransportResponsesTopic;
    
    @Value("${kafka.topics.internal-events:courier-internal-events}")
    private String internalEventsTopic;
    
    // Producer Configuration
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    // Consumer Configuration
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
    
    // Topic Beans
    @Bean
    public NewTopic incomingOrdersTopic() {
        return TopicBuilder.name(incomingOrdersTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    @Bean
    public NewTopic abcTransportTopic() {
        return TopicBuilder.name(abcTransportTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    @Bean
    public NewTopic trackingEventsTopic() {
        return TopicBuilder.name(trackingEventsTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    @Bean
    public NewTopic abcTransportResponsesTopic() {
        return TopicBuilder.name(abcTransportResponsesTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
    
    @Bean
    public NewTopic internalEventsTopic() {
        return TopicBuilder.name(internalEventsTopic)
                .partitions(3)
                .replicas(1)
                .compact()
                .build();
    }
}
