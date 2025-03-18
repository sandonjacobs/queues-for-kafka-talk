package io.confluent.devrel.producer;

import io.confluent.devrel.proto.Event;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProducerTest {

    @Mock
    private KafkaProducer<String, Event> mockProducer;

    private EventProducer eventProducer;
    private static final String TOPIC = "test-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";

    @BeforeEach
    void setup() {
        // Create a test instance with the mocked KafkaProducer
        eventProducer = new EventProducer(BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, TOPIC) {
            @Override
            protected KafkaProducer<String, Event> createProducer(String bootstrapServers, String schemaRegistryUrl) {
                return mockProducer;
            }
        };
    }

    @Test
    void sendEventShouldSendTheCorrectMessageToKafka() {
        // Arrange
        String key = "test-key";
        Event event = Event.newBuilder()
                .setId("test-id")
                .setContent("test-content")
                .setTimestamp(System.currentTimeMillis())
                .setType(Event.EventType.CREATE)
                .build();

        // Mock the behavior of producer.send() to return a mock Future
        RecordMetadata recordMetadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 
                0, 0, 0, 0, 0);
        
        CompletableFuture<RecordMetadata> future = CompletableFuture.completedFuture(recordMetadata);
        
        when(mockProducer.send(any(ProducerRecord.class), any())).thenReturn(future);

        // Act
        eventProducer.sendEvent(key, event);

        // Assert
        // Capture the ProducerRecord that was sent
        ArgumentCaptor<ProducerRecord<String, Event>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(mockProducer).send(captor.capture(), any());
        
        ProducerRecord<String, Event> capturedRecord = captor.getValue();
        assertEquals(TOPIC, capturedRecord.topic());
        assertEquals(key, capturedRecord.key());
        assertEquals(event, capturedRecord.value());
    }

    @Test
    void closeShouldFlushAndCloseTheProducer() {
        // Act
        eventProducer.close();
        
        // Assert
        verify(mockProducer).flush();
        verify(mockProducer).close();
    }
} 