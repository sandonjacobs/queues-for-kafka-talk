package io.confluent.devrel.consumer;

import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ShareConsumerFactory {

    private static final Logger logger = LoggerFactory.getLogger(ShareConsumerFactory.class);

    private static final String DEFAULT_CONSUMER_CONFIG = "default-consumer.properties";
    public static final String GROUP_ID_CONFIG = "share-consumer-group";

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
     * @param path The path to the file, either in classpath or filesystem
     * @return InputStream from the location specified
     * @throws IOException if the configuration file cannot be found or read
     */
    private static InputStream streamFromFile(String path) throws IOException {
        logger.info("Loading configuration from file: {}", path);

        // First try loading from classpath
        InputStream is = ShareConsumerFactory.class.getClassLoader().getResourceAsStream(path);
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
