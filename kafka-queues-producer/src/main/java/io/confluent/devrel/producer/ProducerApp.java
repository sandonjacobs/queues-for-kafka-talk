package io.confluent.devrel.producer;

import io.confluent.devrel.common.CommandLineArguments;
import io.confluent.devrel.common.KafkaConfig;
import io.confluent.devrel.proto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application for the producer module.
 */
public class ProducerApp {
    private static final Logger logger = LoggerFactory.getLogger(ProducerApp.class);
    
    public static void main(String[] args) {
        // Parse command line arguments
        CommandLineArguments cmdArgs = CommandLineArguments.parse(args);
        
        // Get parameter values
        int producerDuration = Integer.parseInt(cmdArgs.getDuration());
        int producerInterval = Integer.parseInt(cmdArgs.getInterval());
        
        logger.info("Starting Kafka Event Producer");
        logger.info("Producer will run for {} seconds with {} ms interval between events", 
                producerDuration, producerInterval);
        
        try (EventProducer producer = new EventProducer(
                KafkaConfig.BOOTSTRAP_SERVERS, 
                KafkaConfig.SCHEMA_REGISTRY_URL,
                KafkaConfig.TOPIC)) {
            
            // Calculate end time based on duration parameter
            long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(producerDuration);
            int counter = 0;
            
            AtomicBoolean running = new AtomicBoolean(true);
            
            // Add a shutdown hook to gracefully terminate
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received, stopping producer...");
                running.set(false);
            }));
            
            // Produce events until the duration is reached or shutdown hook triggered
            logger.info("Starting to produce events...");
            while (System.currentTimeMillis() < endTime && running.get()) {
                String id = UUID.randomUUID().toString();
                
                Event event = Event.newBuilder()
                        .setId(id)
                        .setContent("Sample message " + counter++)
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setType(Event.EventType.CREATE)
                        .build();
                
                producer.sendEvent(id, event);
                
                // Wait between sends according to the interval parameter
                Thread.sleep(producerInterval);
            }
            
            logger.info("All events sent, producer shutting down");
            
        } catch (Exception e) {
            logger.error("Error in producer: {}", e.getMessage(), e);
        }
        
        logger.info("Producer completed");
    }
} 