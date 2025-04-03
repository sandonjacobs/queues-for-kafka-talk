package io.confluent.devrel.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Kafka consumer that demonstrates the shared consumer group feature in Apache Kafka 4.0 (KIP-932)
 * This implements "Queues for Kafka" where each message is delivered to exactly one consumer in the group
 */
public class SharedEventConsumer<Key, Value> implements Runnable, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SharedEventConsumer.class);

    private final String consumerId;
    private final ShareConsumer<Key, Value> consumer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final EventHandler<Key, Value> eventHandler;

    public SharedEventConsumer(String consumerId, ShareConsumer<Key, Value> consumer,
                               EventHandler<Key, Value> eventHandler,
                               String...topics) {
        this.consumerId = consumerId;
        this.consumer = consumer;
        this.consumer.subscribe(Arrays.asList(topics));
        this.eventHandler = eventHandler;
    }

    @Override
    public void run() {
        try {
            while (!closed.get()) {
                ConsumerRecords<Key, Value> records = consumer.poll(Duration.ofMillis(500));
                
                records.forEach(record -> {
                    logger.debug("Consumer {}: Received event: key={}, value={}, partition={}, offset={}",
                            consumerId, record.key(), record.value(), record.partition(), record.offset());
                    try {
                        eventHandler.handleEvent(record.key(), record.value(), record.partition(), record.offset());
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
    

    public void shutdown() {
        closed.set(true);
        consumer.wakeup();
    }
    
    @Override
    public void close() {
        shutdown();
    }
    
    public interface EventHandler<Key, Value> {
        void handleEvent(Key key, Value event, int partition, long offset);
    }
} 