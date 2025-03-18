package io.confluent.devrel.consumer;

import io.confluent.devrel.common.CommandLineArguments;
import io.confluent.devrel.common.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main application for the consumer module.
 */
public class ConsumerApp {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);
    
    public static void main(String[] args) {
        // Parse command line arguments
        CommandLineArguments cmdArgs = CommandLineArguments.parse(args);
        
        logger.info("Starting Kafka Event Consumers");
        
        // Create a thread pool for multiple consumers
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        
        try {
            // Create and start the traditional consumer
            EventConsumer traditionalConsumer = new EventConsumer(
                    KafkaConfig.BOOTSTRAP_SERVERS, 
                    KafkaConfig.SCHEMA_REGISTRY_URL,
                    KafkaConfig.CONSUMER_GROUP,
                    KafkaConfig.TOPIC, 
                    (key, event) -> {
                        logger.info("Traditional consumer processing event: id={}, type={}, content={}", 
                                event.getId(), event.getType(), event.getContent());
                    }
            );
            
            // Create and start two shared queue consumers in the same group
            // Both will receive different messages from the same topic (queue semantics)
            SharedEventConsumer sharedConsumer1 = new SharedEventConsumer(
                    KafkaConfig.BOOTSTRAP_SERVERS,
                    KafkaConfig.SCHEMA_REGISTRY_URL,
                    KafkaConfig.SHARED_CONSUMER_GROUP,
                    KafkaConfig.TOPIC,
                    "shared-consumer-1",
                    (key, event) -> {
                        // Simulate different processing times
                        try {
                            logger.info("Shared consumer 1 starting processing of event: id={}", event.getId());
                            Thread.sleep(1500); // Longer processing time
                            logger.info("Shared consumer 1 completed processing of event: id={}", event.getId());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
            );
            
            SharedEventConsumer sharedConsumer2 = new SharedEventConsumer(
                    KafkaConfig.BOOTSTRAP_SERVERS,
                    KafkaConfig.SCHEMA_REGISTRY_URL,
                    KafkaConfig.SHARED_CONSUMER_GROUP,
                    KafkaConfig.TOPIC,
                    "shared-consumer-2",
                    (key, event) -> {
                        // Simulate different processing times
                        try {
                            logger.info("Shared consumer 2 starting processing of event: id={}", event.getId());
                            Thread.sleep(800); // Shorter processing time
                            logger.info("Shared consumer 2 completed processing of event: id={}", event.getId());
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
            );
            
            // Start all consumers
            executorService.submit(traditionalConsumer);
            executorService.submit(sharedConsumer1);
            executorService.submit(sharedConsumer2);
            
            logger.info("All consumers started and running");
            
            // Add a shutdown hook to gracefully terminate
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, stopping consumers...");
                traditionalConsumer.shutdown();
                sharedConsumer1.shutdown();
                sharedConsumer2.shutdown();
                
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
        
        logger.info("Consumers completed");
    }
} 