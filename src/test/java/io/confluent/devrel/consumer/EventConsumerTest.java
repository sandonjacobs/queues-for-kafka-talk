package io.confluent.devrel.consumer;

import io.confluent.devrel.proto.Event;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    private static final String TOPIC = "test-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String GROUP_ID = "test-group";

    @Mock
    private KafkaConsumer<String, Event> mockConsumer;

    @Mock
    private EventConsumer.EventHandler mockEventHandler;

    private EventConsumer eventConsumer;

    @BeforeEach
    void setup() {
        // Create a test instance with the mocked KafkaConsumer
        eventConsumer = new EventConsumer(BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, GROUP_ID, TOPIC, mockEventHandler) {
            @Override
            protected KafkaConsumer<String, Event> createConsumer(String bootstrapServers, String schemaRegistryUrl, String groupId) {
                return mockConsumer;
            }
        };
    }

    @Test
    void consumeShouldProcessRecordsAndInvokeHandler() throws Exception {
        // Arrange
        // Create a test event
        Event event = Event.newBuilder()
                .setId("test-id")
                .setContent("test-content")
                .setTimestamp(System.currentTimeMillis())
                .setType(Event.EventType.CREATE)
                .build();

        // Create consumer records
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        List<ConsumerRecord<String, Event>> records = new ArrayList<>();
        records.add(new ConsumerRecord<>(TOPIC, 0, 0, "key1", event));
        
        Map<TopicPartition, List<ConsumerRecord<String, Event>>> recordsMap = new HashMap<>();
        recordsMap.put(topicPartition, records);
        
        ConsumerRecords<String, Event> consumerRecords = new ConsumerRecords<>(recordsMap);

        // Configure mock consumer to return our test records once, then wake up
        AtomicBoolean firstCall = new AtomicBoolean(true);
        when(mockConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            if (firstCall.getAndSet(false)) {
                return consumerRecords;
            } else {
                throw new WakeupException();
            }
        });

        // Use a countdown latch to wait for the handler to be called
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(mockEventHandler).handleEvent("key1", event);

        // Act
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(eventConsumer);
        
        // Assert
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event handler should be called");
        verify(mockEventHandler, times(1)).handleEvent("key1", event);

        // Clean up
        eventConsumer.shutdown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shutdownShouldStopConsumer() {
        // Act
        eventConsumer.shutdown();
        
        // Assert
        verify(mockConsumer).wakeup();
    }

    @Test
    void consumerShouldHandleExceptionsInEventHandler() throws Exception {
        // Arrange
        // Create a test event
        Event event = Event.newBuilder()
                .setId("test-id")
                .setContent("test-content")
                .setTimestamp(System.currentTimeMillis())
                .setType(Event.EventType.CREATE)
                .build();

        // Create consumer records with one record
        TopicPartition topicPartition = new TopicPartition(TOPIC, 0);
        List<ConsumerRecord<String, Event>> records = new ArrayList<>();
        records.add(new ConsumerRecord<>(TOPIC, 0, 0, "key1", event));
        
        Map<TopicPartition, List<ConsumerRecord<String, Event>>> recordsMap = new HashMap<>();
        recordsMap.put(topicPartition, records);
        
        ConsumerRecords<String, Event> consumerRecords = new ConsumerRecords<>(recordsMap);

        // Configure mock consumer to return our test records once, then wake up
        AtomicBoolean firstCall = new AtomicBoolean(true);
        when(mockConsumer.poll(any(Duration.class))).thenAnswer(invocation -> {
            if (firstCall.getAndSet(false)) {
                return consumerRecords;
            } else {
                throw new WakeupException();
            }
        });

        // Make the event handler throw an exception
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            throw new RuntimeException("Test exception");
        }).when(mockEventHandler).handleEvent("key1", event);

        // Act
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(eventConsumer);
        
        // Assert - the consumer should continue running despite the exception
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event handler should be called");
        verify(mockEventHandler, times(1)).handleEvent("key1", event);

        // Clean up
        eventConsumer.shutdown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
} 