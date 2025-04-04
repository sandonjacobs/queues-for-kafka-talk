package io.confluent.devrel.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application for the producer module.
 */
public class ProducerApp {
    private static final Logger logger = LoggerFactory.getLogger(ProducerApp.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final List<String> TYPE_OPTIONS = List.of("CREATE", "UPDATE", "DELETE");
    
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
            kafkaProducer = ProducerFactory.createProducer(propertyPathOverride.get());
        } else {
            kafkaProducer = ProducerFactory.createProducer();
        }

        try (EventProducer<String, String> eventProducer = new EventProducer<>(kafkaProducer, "events-string")) {
            
            // Calculate end time based on duration parameter
            long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(producerDuration);

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
                
                MyEvent event = new MyEvent(id, randomOption(), "Sample message " + id);
                
                eventProducer.sendEvent(id, objectMapper.writeValueAsString(event));
                
                // Wait between sends according to the interval parameter
                Thread.sleep(producerInterval);
            }
            
            logger.info("All events sent, producer shutting down");
            
        } catch (Exception e) {
            logger.error("Error in producer: {}", e.getMessage(), e);
        }
        
        logger.info("Producer completed");
    }

    private static String randomOption() {
        final int index = ThreadLocalRandom.current().nextInt(TYPE_OPTIONS.size()) % TYPE_OPTIONS.size();
        return TYPE_OPTIONS.get(index);
    }
} 