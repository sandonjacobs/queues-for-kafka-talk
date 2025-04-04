# Makefile for Queues for Kafka project

# Colors for better readability
BLUE:=$(shell printf "\033[0;34m")
GREEN:=$(shell printf "\033[0;32m")
YELLOW:=$(shell printf "\033[1;33m")
RED:=$(shell printf "\033[0;31m")
BOLD:=$(shell printf "\033[1m")
RESET:=$(shell printf "\033[0m")

# Emojis for better readability
CHECK=✅
WARNING=⚠️
ERROR=❌
INFO=ℹ️
ROCKET=🚀
COFFEE=☕
CLOCK=🕒
CLOUD=☁️
STAR=⭐
TERRAFORM=🌐

# Maven wrapper command
MVN = ./mvnw

# Default target
all: build

# Build the entire project
build:
	$(MVN) clean install

# Run all unit tests
test:
	$(MVN) test

test-kafka-producer:
	$(MVN) test -pl kafka-producer

test-queue-consumer:
	$(MVN) test -pl kafka-queues-consumer

# Default properties file paths
CONSUMER_PROPS ?= kafka-queues-consumer/src/main/resources/default-consumer.properties

# Run the producer application with optional arguments
# Usage: make run-kafka-producer [PRODUCER_PROPS=/path/to/properties] [ARGS="--duration 120 --interval 1000"]
run-kafka-producer:
	$(MVN) compile exec:java -pl kafka-producer -Dexec.mainClass="io.confluent.devrel.producer.ProducerApp" -Dexec.args="$(if $(PRODUCER_PROPS),--properties $(PRODUCER_PROPS)) $(ARGS)"

help-kafka-producer:
	$(MVN) exec:java -pl kafka-producer -Dexec.args="--help"

help-queue-consumer:
	$(MVN) exec:java -pl kafka-queues-consumer -Dexec.args="--help"

# Run the consumer application with optional arguments
# Usage: make run-queue-consumer [CONSUMER_PROPS=/path/to/properties] [ARGS="--consumers 10"]
run-queue-consumer:
	$(MVN) compile exec:java -pl kafka-queues-consumer -Dexec.mainClass="io.confluent.devrel.consumer.ConsumerApp" -Dexec.args="$(if $(CONSUMER_PROPS),--properties $(CONSUMER_PROPS)) $(ARGS)"

# Clean the project
clean:
	$(MVN) clean

# Docker Compose targets
docker-start:
	docker compose up -d

docker-stop:
	docker compose stop

docker-remove:
	docker compose down


# Clean up Docker environment (stop and remove containers)
docker-clean: docker-stop docker-remove

# Help target
help:
	@echo "${YELLOW}=======================================================================================${RESET}"
	@echo ""
	@echo "${STAR}${STAR} ${BOLD}${GREEN}Demo for Apache Kafka Share Consumers (KIP-932) ${STAR}${STAR} ${RESET}"
	@echo ""
	@echo "${YELLOW}=======================================================================================${RESET}"
	@echo ""
	@echo "${BOLD}- Top level targets${RESET}"
	@echo "	🏗️ all           - Build the entire project (default target)"
	@echo "	🏗️ build         - Build the entire project"
	@echo "	✅ test          - Run all unit tests"
	@echo "	🧹 clean         - Clean the project"
	@echo "	ℹ️ help          - Show this help message"
	@echo ""
	@echo "${YELLOW}=======================================================================================${RESET}"
	@echo ""
	@echo "${BOLD}- Docker Targets${RESET}"
	@echo "	🐳 docker-start  - Start the Kafka environment"
	@echo "	🐳 docker-stop   - Stop the Kafka environment"
	@echo "	🐳 docker-remove - Remove the Kafka environment"
	@echo "	🐳 docker-clean  - Stop and remove the Kafka environment"
	@echo ""
	@echo "${YELLOW}=======================================================================================${RESET}"
	@echo ""
	@echo "${BOLD}- Test Targets${RESET}"
	@echo "	✅ test-producer - Run unit tests for producer module"
	@echo "	✅ test-consumer - Run unit tests for consumer module"
	@echo ""
	@echo "${YELLOW}=======================================================================================${RESET}"
	@echo "${BOLD}- Run Targets${RESET}"
	@echo ""
	@echo "	ℹ️ help-kafka-producer  - Show help for kafka producer application"
	@echo "	🚀 run-kafka-producer   - Run the kafka producer application"
	@echo "	ℹ️ help-queue-consumer  - Show help for queue consumer application"
	@echo "	🚀 run-queue-consumer   - Run the queue consumer application"
	@echo ""
	@echo "	** Example usage with arguments:"
	@echo "		make run-kafka-producer [PRODUCER_PROPS=/path/to/properties] [ARGS=\"--duration 120 --interval 1000\"]"
	@echo "	 	make run-queue-consumer [CONSUMER_PROPS=/path/to/properties] [ARGS=\"--consumers 5\"]"
	@echo ""
	@echo "	** Default properties files:"
	@echo "		Producer: kafka-producer/src/main/resources/default-producer.properties"
	@echo "		Consumer: kafka-queues-consumer/src/main/resources/default-consumer.properties"
	@echo "${YELLOW}=======================================================================================${RESET}"

.PHONY: all build test test-kafka-producer test-queue-consumer run-kafka-producer run-queue-consumer clean docker-start docker-stop docker-remove docker-clean docker-logs docker-logs-kafka docker-logs-topics help