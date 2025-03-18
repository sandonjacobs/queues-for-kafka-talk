package io.confluent.devrel.common;

/**
 * Shared Kafka configuration used across modules.
 */
public class KafkaConfig {
    // Kafka configuration
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String SCHEMA_REGISTRY_URL = "http://localhost:8081";
    public static final String TOPIC = "events";
    public static final String CONSUMER_GROUP = "event-processor";
    public static final String SHARED_CONSUMER_GROUP = "shared-event-processors";
} 