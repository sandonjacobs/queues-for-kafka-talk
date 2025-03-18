# Kafka Protobuf Serialization Example

This project demonstrates how to build a Kafka client application that produces and consumes events with:
- String keys
- Protobuf-serialized values
- Integration with Confluent Schema Registry

## Features

- Produces events serialized with Protocol Buffers to a Kafka topic
- Consumes events deserialized from Protocol Buffers from the same Kafka topic
- Uses Confluent Schema Registry for schema management
- Provides a simple event model with different event types
- Uses Apache Kafka 4.0.0-rc4 (pure KRaft mode without Zookeeper)
- Uses Confluent Schema Registry 7.9.0

## Prerequisites

- Java 21 or higher
- Maven 3.8 or higher
- Docker and Docker Compose (for running Kafka and Schema Registry locally)

## Project Structure

- `src/main/proto/message.proto`: Protobuf definition for event messages
- `src/main/java/io/confluent/devrel/producer/EventProducer.java`: Kafka producer with Protobuf serialization
- `src/main/java/io/confluent/devrel/consumer/EventConsumer.java`: Kafka consumer with Protobuf deserialization
- `src/main/java/io/confluent/devrel/App.java`: Main application demonstrating producer and consumer usage

## Setting Up Kafka and Schema Registry

You can use Docker Compose to run Apache Kafka 4.0.0-rc4 (in pure KRaft mode) and Confluent Schema Registry locally:

```bash
# Start the services
docker-compose up -d
```

The docker-compose.yml file contains:
- Apache Kafka 4.0.0-rc4 (pure KRaft mode without Zookeeper)
- Confluent Schema Registry 7.9.0

### Note on KRaft Mode

This setup uses Apache Kafka 4.0.0-rc4 in pure KRaft mode, which is Kafka's new consensus protocol that removes the dependency on Zookeeper. KRaft mode provides:

- Simpler architecture (no separate Zookeeper cluster)
- Better performance and scalability
- Reduced operational complexity
- A single security model

## Building and Running the Application

```bash
# Build the application
mvn clean compile

# Run the tests
mvn test

# Package the application
mvn package

# Run the application
mvn exec:java
```

You can also run the packaged jar with all dependencies:

```bash
java -jar target/kafka-protobuf-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Understanding the Protobuf Event Schema

The example uses a simple event schema defined in `message.proto`:

```protobuf
message Event {
  string id = 1;
  string content = 2;
  int64 timestamp = 3;
  EventType type = 4;
  
  enum EventType {
    UNKNOWN = 0;
    CREATE = 1;
    UPDATE = 2;
    DELETE = 3;
  }
}
```

## How It Works

1. The application creates both a producer and consumer
2. The producer serializes events using Protobuf and the Confluent Protobuf Serializer
3. The consumer deserializes events using the Confluent Protobuf Deserializer
4. Schema Registry manages and validates the Protobuf schemas
5. The consumer processes events in a separate thread

## Example Output

When you run the application, you should see output similar to:

```
13:45:22.123 [main] INFO io.confluent.devrel.App - Starting Kafka Protobuf Serialization Example
13:45:22.567 [main] INFO io.confluent.devrel.producer.EventProducer - Event producer initialized for topic: events
13:45:22.789 [main] INFO io.confluent.devrel.consumer.EventConsumer - Event consumer initialized for topic: events with group: event-processor
13:45:23.456 [main] INFO io.confluent.devrel.producer.EventProducer - Sent event with id: 123e4567-e89b-12d3-a456-426614174000 and type: CREATE
13:45:24.123 [pool-1-thread-1] INFO io.confluent.devrel.App - Processing event: id=123e4567-e89b-12d3-a456-426614174000, type=CREATE, content=Sample message 0
...
13:45:33.456 [main] INFO io.confluent.devrel.App - Kafka Protobuf Serialization Example completed 