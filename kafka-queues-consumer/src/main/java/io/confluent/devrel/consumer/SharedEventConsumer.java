package io.confluent.devrel.consumer;

import io.confluent.devrel.common.StringEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Kafka consumer that demonstrates the shared consumer group feature in Apache Kafka 4.0 (KIP-932)
 * This implements "Queues for Kafka" where each message is delivered to exactly one consumer in the group
 */
public class SharedEventConsumer implements Runnable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SharedEventConsumer.class);
    
    private final ShareConsumer<String, String> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final EventHandler eventHandler;
    private final String consumerId;
    private final String topic;
    
    public SharedEventConsumer(String bootstrapServers, 
                        String groupId, String topic, String consumerId,
                        EventHandler eventHandler) {
        this.consumer = createConsumer(bootstrapServers, groupId);
        this.consumer.subscribe(Collections.singletonList(topic));
        this.eventHandler = eventHandler;
        this.topic = topic;
        this.consumerId = consumerId;
        
        logger.info("Shared event consumer {} initialized for topic: {} with group: {}", 
                   consumerId, topic, groupId);
    }
    
    // Protected method to allow overriding in tests for mocking
    protected ShareConsumer<String, String> createConsumer(String bootstrapServers, String groupId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Enable unstable APIs to access newer features like the queue protocol
        props.put("unstable.api.versions.enable", "true");
        
        // KIP-932 configuration for Kafka 4.0+
        // Set the GROUP_PROTOCOL_CONFIG to CONSUMER for queue semantics
        props.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "CONSUMER");
        
        // Process fewer records at a time for better load balancing
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        
        // Use shorter poll intervals
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        return new KafkaShareConsumer<>(props);
    }
    
    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                
                records.forEach(record -> {
                    logger.debug("Consumer {}: Received event: key={}, value={}, partition={}, offset={}",
                            consumerId, record.key(), record.value(), record.partition(), record.offset());
                    
                    try {
                        // Parse the string value into a StringEvent
                        StringEvent event = parseEvent(record.value());
                        eventHandler.handleEvent(record.key(), event, record.partition());
                        logger.debug("Consumer {}: Processed event: id={}, type={}, content={}, partition={}",
                               consumerId, event.getId(), event.getType(), event.getContent(), 
                               record.partition());
                    } catch (Exception e) {
                        logger.error("Consumer {}: Error handling event: {}", consumerId, e.getMessage(), e);
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
            logger.info("Consumer {} closed", consumerId);
        }
    }
    
    /**
     * Parse a string value into a StringEvent object
     * Format: "id:type:content"
     */
    private StringEvent parseEvent(String value) {
        String[] parts = value.split(":", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid event format: " + value);
        }
        return new StringEvent(parts[0], parts[1], parts[2]);
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
        void handleEvent(String key, StringEvent event, int partition);
    }
} 