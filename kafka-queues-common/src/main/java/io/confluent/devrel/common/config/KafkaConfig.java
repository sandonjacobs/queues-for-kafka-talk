package io.confluent.devrel.common.config;

import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Shared Kafka configuration used across modules.
 */
public class KafkaConfig {
    // Kafka configuration
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";
    public static final String TOPIC = "events-string";
    public static final String CONSUMER_GROUP = "event-processor";
    public static final String SHARED_CONSUMER_GROUP = "queue-event-processors";

    private static final String DEFAULT_PRODUCER_CONFIG = "default-producer.properties";
    private static final String DEFAULT_CONSUMER_CONFIG = "default-consumer.properties";


    public static <K, V> KafkaProducer<K, V> createProducer() throws IOException {
       return createProducer(DEFAULT_PRODUCER_CONFIG);
    }

    /**
     * Creates a Kafka producer using configuration from a file.
     *
     * @param configPath The path to the configuration file
     * @return A configured KafkaProducer instance
     * @throws IOException if the configuration file cannot be found or read
     */
    public static <K, V> KafkaProducer<K, V> createProducer(String configPath) throws IOException {
        try (InputStream inputStream = streamFromFile(configPath)) {
            return createProducer(inputStream);
        }
    }

    static <K, V> KafkaProducer<K, V> createProducer(InputStream propStream) throws IOException {
        Properties props = new Properties();
        props.load(propStream);
        return new KafkaProducer<>(props);
    }

    public static <K, V> ShareConsumer<K, V> createConsumer() throws IOException {
        return createConsumer(new Properties());
    }

    public static <K, V> ShareConsumer<K, V> createConsumer(final Properties overrides) throws IOException {
        return createConsumer(DEFAULT_CONSUMER_CONFIG, overrides);
    }

    /**
     * Creates a Kafka consumer using configuration from a file.
     *
     * @param configPath The path to the configuration file
     * @return A configured KafkaConsumer instance
     * @throws IOException if the configuration file cannot be found or read
     */
    public static <K, V> ShareConsumer<K, V> createConsumer(String configPath, Properties overrides) throws IOException {
        try (InputStream inputStream = streamFromFile(configPath)) {
            return createConsumer(inputStream, overrides);
        }
    }

    static <K, V> ShareConsumer<K, V> createConsumer(InputStream propStream, Properties overrides) throws IOException {
        Properties props = new Properties();
        props.load(propStream);
        props.putAll(overrides);
        return new KafkaShareConsumer<>(props);
    }

    /**
     * First attempts to load the file from the classpath, then falls back to using the provided path as an absolute path.
     * @param path
     * @return InputStream from the location specified
     * @throws IOException if the configuration file cannot be found or read
     */
    static InputStream streamFromFile(String path) throws IOException {
        // First try to load from classpath
        InputStream props = ClassLoader.getSystemResourceAsStream(path);

        // If not found in classpath, try as a file path
        if (props == null) {
            File configFile = new File(path);
            if (!configFile.exists()) {
                throw new IOException("Configuration file not found in classpath or at path: " + path);
            }
            props = new FileInputStream(configFile);
        }
        
        return props;
    }
} 