package io.confluent.devrel.consumer;

import org.apache.kafka.clients.consumer.AcknowledgeType;
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
                    try {
                        eventHandler.handleEvent(record.key(), record.value(), record.partition(), record.offset());
                        // Acknowledge this event was handled without error.
                        consumer.acknowledge(record, AcknowledgeType.ACCEPT);
                    } catch (Exception e) {
                        logger.error("Consumer {}: Error handling event: {}", consumerId, e.getMessage(), e);
                        // NOTE: Choosing to RELEASE on any exception here.
                        // Likely would discern the specific Exception type to determine if the
                        // underlying issue was worthy of the retry logic of the share consumer.
                        consumer.acknowledge(record, AcknowledgeType.REJECT);
                    }
                    consumer.commitSync(); // the most EXPLICIT use of EXPLICIT!!!
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