package io.confluent.devrel.consumer;

import io.confluent.devrel.common.CommandLineArguments;
import io.confluent.devrel.common.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main application for the consumer module.
 */
public class ConsumerApp {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerApp.class);
    
    public static void main(String[] args) {
        // Parse command line arguments
        CommandLineArguments cmdArgs = CommandLineArguments.parse(args);
        
        // Get the number of shared consumers to create
        int numSharedConsumers = cmdArgs.getNumConsumersAsInt();
        
        logger.info("Starting Kafka Event Consumers with {} shared consumers (KIP-932 Queue mode)", numSharedConsumers);
        logger.info("Using GROUP_PROTOCOL_CONFIG=CONSUMER for queue semantics");
        
        // Create a thread pool with named threads
        ExecutorService executorService = Executors.newFixedThreadPool(1 + numSharedConsumers, 
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
            });
        
        try {
//            // Create and start the traditional consumer
//            EventConsumer traditionalConsumer = new EventConsumer(
//                    KafkaConfig.BOOTSTRAP_SERVERS,
//                    KafkaConfig.SCHEMA_REGISTRY_URL,
//                    KafkaConfig.CONSUMER_GROUP,
//                    KafkaConfig.TOPIC,
//                    (key, event) -> {
//                        logger.info("Traditional consumer processing event: id={}, type={}, content={}, thread: {}",
//                                event.getId(), event.getType(), event.getContent(),
//                                Thread.currentThread().getName());
//                    }
//            );
            
            // Create and start multiple shared consumers in the same group
            List<SharedEventConsumer> sharedConsumers = new ArrayList<>();
            for (int i = 1; i <= numSharedConsumers; i++) {
                final String consumerId = "shared-consumer-" + i;
                final int processingTime = 500; // + (i * 100); // Significantly increase processing time
                
                SharedEventConsumer consumer = new SharedEventConsumer(
                        KafkaConfig.BOOTSTRAP_SERVERS,
                        KafkaConfig.SCHEMA_REGISTRY_URL,
                        KafkaConfig.SHARED_CONSUMER_GROUP,
                        KafkaConfig.TOPIC,
                        consumerId,
                        (key, event) -> {
                            // Simulate different processing times
                            try {
                                logger.info("{} starting processing of event: id={}, thread: {}",
                                    consumerId, event.getId(), Thread.currentThread().getName());
                                Thread.sleep(processingTime); // Variable processing time
                                logger.info("{} completed processing of event: id={}, thread: {}", 
                                    consumerId, event.getId(), Thread.currentThread().getName());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                );
                
                sharedConsumers.add(consumer);
                executorService.submit(consumer);
            }
            
            // Start the traditional consumer
//            executorService.submit(traditionalConsumer);
            
            logger.info("All consumers started and running");
            
            // Add a shutdown hook to gracefully terminate
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, stopping consumers...");
//                traditionalConsumer.shutdown();
                
                // Shutdown all shared consumers
                for (SharedEventConsumer consumer : sharedConsumers) {
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
        
        logger.info("Consumers completed");
    }
} 