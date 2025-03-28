package io.confluent.devrel.producer;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventProducer<Key, Value> implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(EventProducer.class);
    
    private final Producer<Key, Value> producer;
    private final String topic;
    
    public EventProducer(Producer<Key, Value> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
        logger.info("Event producer initialized for topic: {}", topic);
    }
    
    public void sendEvent(Key key, Value event) {
        ProducerRecord<Key, Value> record = new ProducerRecord<>(topic, key, event);
        
        // Send record
        producer.send(record, (metadata, exception) ->
                logger.info("Sent event with offset: {}, partition: {}",
                        metadata.offset(), metadata.partition()));

    }
    
    @Override
    public void close() {
        producer.flush();
        producer.close();
        logger.info("Event producer closed");
    }
} 