package io.confluent.devrel.producer;

import io.confluent.devrel.common.args.ProducerArgsParser;
import io.confluent.devrel.common.config.KafkaConfig;
import io.confluent.devrel.common.model.MyEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application for the producer module.
 */
public class ProducerApp {
    private static final Logger logger = LoggerFactory.getLogger(ProducerApp.class);
    
    public static void main(String[] args) throws Exception {
        // Parse command line arguments
        ProducerArgsParser cmdArgs = ProducerArgsParser.parseOptions(args);
        
        // Get parameter values
        int producerDuration = cmdArgs.getDuration();
        int producerInterval = cmdArgs.getInterval();
        
        logger.info("Starting Kafka Event Producer (String serialization)");
        logger.info("Producer will run for {} seconds with {} ms interval between events", 
                producerDuration, producerInterval);

        Optional<String> propertyPathOverride = Optional.ofNullable(cmdArgs.getKafkaPropsPath());
        KafkaProducer<String, String> kafkaProducer;
        if (propertyPathOverride.isPresent()) {
            kafkaProducer = KafkaConfig.createProducer(propertyPathOverride.get());
        } else {
            kafkaProducer = KafkaConfig.createProducer();
        }

        try (EventProducer<String, String> eventProducer = new EventProducer<>(kafkaProducer, KafkaConfig.TOPIC)) {
            
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
                
                MyEvent event = new MyEvent(
                        id,
                        "CREATE",
                        "Sample message " + counter++
                );
                
                eventProducer.sendEvent(id, event.toString());
                
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