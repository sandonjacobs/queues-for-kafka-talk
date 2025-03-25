package io.confluent.devrel.producer;

import io.confluent.devrel.common.StringEvent;
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
    
    private final Producer<String, String> producer;
    private final String topic;
    
    public EventProducer(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        
        logger.info("Event producer initialized for topic: {}", topic);
    }
    
    public void sendEvent(String key, StringEvent event) {
        // Format: "id:type:content"
        String eventString = String.format("%s:%s:%s", event.getId(), event.getType(), event.getContent());
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, eventString);
        
        try {
            // Send record and block until we get confirmation
            Future<?> future = producer.send(record);
            future.get(10, TimeUnit.SECONDS);
            logger.info("Sent event with id: {} and type: {}", event.getId(), event.getType());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error sending event: {}", e.getMessage(), e);
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