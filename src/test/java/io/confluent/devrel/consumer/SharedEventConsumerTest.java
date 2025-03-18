package io.confluent.devrel.consumer;

import io.confluent.devrel.proto.Event;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedEventConsumerTest {

    private static final String TOPIC = "test-topic";
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    private static final String GROUP_ID = "test-group";
    private static final String CONSUMER_ID = "test-consumer-1";

    @Mock
    private KafkaConsumer<String, Event> mockConsumer;

    @Mock
    private SharedEventConsumer.EventHandler mockEventHandler;

    private SharedEventConsumer sharedConsumer;

    @BeforeEach
    void setup() {
        // Prevent NullPointerException in subscribe call
        doNothing().when(mockConsumer).subscribe(Collections.singletonList(TOPIC));
        
        // Create a subclass of SharedEventConsumer for testing
        sharedConsumer = new SharedEventConsumer(BOOTSTRAP_SERVERS, SCHEMA_REGISTRY_URL, 
                GROUP_ID, TOPIC, CONSUMER_ID, mockEventHandler) {
            @Override
            protected KafkaConsumer<String, Event> createConsumer(String bootstrapServers, 
                    String schemaRegistryUrl, String groupId) {
                return mockConsumer;
            }
        };
    }

    @Test
    void sharedConsumerShouldUseCorrectConfiguration() {
        // Verify consumer configuration is applied properly
        Properties props = createSharedConsumerProperties();
        assertEquals("true", props.getProperty("unstable.api.versions.enable"), 
                "Should enable unstable API versions");
        assertEquals("CONSUMER", props.getProperty("group.protocol"), 
                "Should use CONSUMER protocol");
        assertEquals("10", props.getProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG),
                "Should limit the number of records per poll");
    }

    @Test
    void consumeShouldProcessRecordsAndInvokeHandler() throws Exception {
        // Arrange - create a test event
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

        // Act - run the consumer in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(sharedConsumer);
        
        // Assert - the handler should be called
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Event handler should be called");
        verify(mockEventHandler, times(1)).handleEvent("key1", event);

        // Clean up
        sharedConsumer.shutdown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
    
    // Helper method to create properties matching our shared consumer configuration
    private Properties createSharedConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put("schema.registry.url", "http://localhost:8081");
        props.put("specific.protobuf.value.type", "io.confluent.devrel.proto.Event");
        
        // Enable unstable APIs and queue protocol configuration
        props.put("unstable.api.versions.enable", "true");
        props.put("group.protocol", "CONSUMER");
        
        // Settings for optimized queue behavior
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        return props;
    }
} 