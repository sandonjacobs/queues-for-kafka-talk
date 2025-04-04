package io.confluent.devrel.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * Main application for the consumer module.
 */
public class ConsumerApp {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);
    
    public static void main(String[] args) {
        // Parse command line arguments
        ConsumerArgsParser cmdArgs = ConsumerArgsParser.parseOptions(args);
        
        Optional<String> kafkaPropsPath = Optional.ofNullable(cmdArgs.getKafkaPropsPath());

        final int numSharedConsumers = cmdArgs.getNumConsumers();
        logger.info("Starting {} Kafka Share Consumers (KIP-932 Queue mode)", numSharedConsumers);
        logger.info("Using GROUP_PROTOCOL_CONFIG=CONSUMER for queue semantics");
        
        // Create a thread pool with named threads
        try (ExecutorService executorService = newFixedThreadPool(1 + numSharedConsumers,
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "kafka-consumer-" + threadNumber.getAndIncrement());
                        if (t.isDaemon()) {
                            t.setDaemon(false);
                        }
                        if (t.getPriority() != Thread.NORM_PRIORITY) {
                            t.setPriority(Thread.NORM_PRIORITY);
                        }
                        return t;
                    }
                })) {

            try {
                List<SharedEventConsumer<String, String>> sharedConsumers = new ArrayList<>();
                for (int i = 1; i <= numSharedConsumers; i++) {
                    final String consumerId = "shared-consumer-" + i;
                    final int processingTime = 500; // + (i * 100); // Significantly increase processing time

                    SharedEventConsumer<String, String> consumer = new SharedEventConsumer<>(
                            consumerId,
                            createConsumer(kafkaPropsPath),
                            (key, value, partition, offset) -> {
                                try {
                                    logger.info("Consumer {} from partition: {} at offset: {} => received key {} value {}",
                                            consumerId, partition, offset, key, value);
                                    Thread.sleep(processingTime); // Variable processing time
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "events-string");
                    sharedConsumers.add(consumer);
                    executorService.submit(consumer);
                }

                logger.info("All consumers started and running");

                // Add a shutdown hook to gracefully terminate
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Shutdown signal received, stopping consumers...");

                    // Shutdown all shared consumers
                    for (SharedEventConsumer<String, String> consumer : sharedConsumers) {
                        consumer.shutdown();
                    }

                    executorService.shutdown();
                    try {
                        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                            executorService.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executorService.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }));

                // Keep the main thread alive indefinitely (until shutdown)
                Thread.currentThread().join();

            } catch (Exception e) {
                logger.error("Error in consumer: {}", e.getMessage(), e);
                executorService.shutdownNow();
            }
        }

        logger.info("Consumers completed");
    }

    static ShareConsumer<String, String> createConsumer(final Optional<String> consumerConfigPath) throws IOException {
        Properties propOverrides = new Properties();
        propOverrides.put(ConsumerConfig.GROUP_ID_CONFIG, ShareConsumerFactory.GROUP_ID_CONFIG);
        propOverrides.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        propOverrides.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        propOverrides.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        propOverrides.put("share.group." + ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Enable unstable APIs to access newer features like the queue protocol
        propOverrides.put("unstable.api.versions.enable", "true");
        // KIP-932 configuration for Kafka 4.0+
        // Set the GROUP_PROTOCOL_CONFIG to CONSUMER for queue semantics
        propOverrides.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, "CONSUMER");
        // Process fewer records at a time for better load balancing
        propOverrides.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "5");
        // Use shorter poll intervals
        propOverrides.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "5000");
        propOverrides.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        if (consumerConfigPath.isPresent()) {
            return ShareConsumerFactory.createConsumer(consumerConfigPath.get(), propOverrides);
        } else {
            return ShareConsumerFactory.createConsumer(propOverrides);
        }
    }
}