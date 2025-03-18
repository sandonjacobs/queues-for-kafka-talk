package io.confluent.devrel.producer;

import io.confluent.devrel.proto.Event;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EventProducer implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);
    
    private final Producer<String, Event> producer;
    private final String topic;
    
    public EventProducer(String bootstrapServers, String schemaRegistryUrl, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class.getName());
        props.put("schema.registry.url", schemaRegistryUrl);
        
        // Additional producer configs for reliability
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        
        logger.info("Event producer initialized for topic: {}", topic);
    }
    
    public void sendEvent(String key, Event event) {
        ProducerRecord<String, Event> record = new ProducerRecord<>(topic, key, event);
        
        try {
            Future<?> future = producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.error("Failed to send event: {}", exception.getMessage());
                } else {
                    logger.debug("Event sent to partition {} with offset {}", 
                            metadata.partition(), metadata.offset());
                }
            });
            
            // Wait for the result to ensure it was sent (optional, can be removed for async operation)
            future.get(10, TimeUnit.SECONDS);
            logger.info("Sent event with id: {} and type: {}", event.getId(), event.getType());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error while sending event: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void close() {
        producer.flush();
        producer.close();
        logger.info("Event producer closed");
    }
} 