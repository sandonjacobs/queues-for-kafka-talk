package io.confluent.devrel.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ProducerFactory {
    private static final Logger logger = LoggerFactory.getLogger(ProducerFactory.class);

    private static final String DEFAULT_PRODUCER_CONFIG = "default-producer.properties";

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

    /**
     * First attempts to load the file from the classpath, then falls back to using the provided path as an absolute path.
     * @param path The path to the file, either in classpath or filesystem
     * @return InputStream from the location specified
     * @throws IOException if the configuration file cannot be found or read
     */
    private static InputStream streamFromFile(String path) throws IOException {
        logger.info("Loading configuration from file: {}", path);
        
        // First try loading from classpath
        InputStream is = ProducerFactory.class.getClassLoader().getResourceAsStream(path);
        if (is != null) {
            logger.info("Found configuration file in classpath: {}", path);
            return is;
        }
        
        // If not found in classpath, try the filesystem
        File file = new File(path);
        if (file.exists()) {
            logger.info("Found configuration file at: {}", file.getAbsolutePath());
            return new FileInputStream(file);
        }
        
        throw new IOException("Configuration file not found in classpath or at path: " + path);
    }
}
