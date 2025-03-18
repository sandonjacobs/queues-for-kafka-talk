package io.confluent.devrel.consumer;

import io.confluent.devrel.proto.Event;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventConsumer implements Runnable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);
    
    private final Consumer<String, Event> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final EventHandler eventHandler;
    
    public EventConsumer(String bootstrapServers, String schemaRegistryUrl, 
                        String groupId, String topic,
                        EventHandler eventHandler) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, Event.class.getName());
        
        this.consumer = new KafkaConsumer<>(props);
        this.consumer.subscribe(Collections.singletonList(topic));
        this.eventHandler = eventHandler;
        
        logger.info("Event consumer initialized for topic: {} with group: {}", topic, groupId);
    }
    
    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ConsumerRecords<String, Event> records = consumer.poll(Duration.ofMillis(500));
                
                records.forEach(record -> {
                    logger.debug("Received event: key={}, value={}, partition={}, offset={}",
                            record.key(), record.value(), record.partition(), record.offset());
                    
                    try {
                        eventHandler.handleEvent(record.key(), record.value());
                    } catch (Exception e) {
                        logger.error("Error handling event: {}", e.getMessage(), e);
                    }
                });
            }
        } catch (WakeupException e) {
            // Ignore if closing
            if (!closed.get()) {
                throw e;
            }
        } finally {
            consumer.close();
            logger.info("Consumer closed");
        }
    }
    
    public void shutdown() {
        closed.set(true);
        consumer.wakeup();
    }
    
    @Override
    public void close() {
        shutdown();
    }
    
    public interface EventHandler {
        void handleEvent(String key, Event event);
    }
} 