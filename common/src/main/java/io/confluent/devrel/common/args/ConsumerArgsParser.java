package io.confluent.devrel.common.args;

import lombok.Getter;
import org.apache.commons.cli.*;

@Getter
public class ConsumerArgsParser extends CommandLineArguments {

    private final int numConsumers;

    ConsumerArgsParser(String kafkaPropsPath, int numConsumers) {
        super(kafkaPropsPath);
        this.numConsumers = numConsumers;
    }

    public static ConsumerArgsParser parseOptions(String[] args) {
        Options options = getBaseOptions(args);

        options.addOption(Option.builder("c")
                .longOpt("consumers")
                .hasArg()
                .argName("count")
                .desc("Number of shared consumers to start (default: 5, max: 10)")
                .type(Number.class)
                .build());


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = ConsumerArgsParser.parse(args, options, parser);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("kafka-queues-demo", options, true);
            System.exit(1);
        }

        // Handle help option
        boolean helpRequested = cmd.hasOption("help") || cmd.hasOption("h");
        if (helpRequested) {
            printHelp(options, formatter);
            System.exit(0);
        }

        final int numConsumerArg = Integer.parseInt(cmd.getOptionValue("consumers", "5"));
        if (numConsumerArg > 10) {
            throw new IllegalArgumentException("Number of consumers must be less than 10");
        }
        final String kafkaPropsPath = cmd.getOptionValue("properties");
        return new ConsumerArgsParser(kafkaPropsPath, numConsumerArg);
    }

    private static void printHelp(Options options, HelpFormatter formatter) {
        System.out.println("Kafka Queues Consumer - Command Line Arguments");
        System.out.println("--------------------------------------------");
        System.out.println("This application demonstrates the queue protocol feature in Kafka 4.0+");
        System.out.println("where each message is delivered to exactly one consumer in the group.");
        System.out.println();
        System.out.println("Required Arguments:");
        System.out.println("  -p, --properties <file>    Path to Kafka client properties file");
        System.out.println();
        System.out.println("Optional Arguments:");
        System.out.println("  -c, --consumers <count>    Number of shared consumers to start");
        System.out.println("                              (default: 3, max: 10)");
        System.out.println("  -h, --help                 Display this help message");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar kafka-queues-consumer.jar \\");
        System.out.println("    --properties /path/to/properties \\");
        System.out.println("    --consumers 5");
        System.out.println();
        System.out.println("For more information about the queue protocol feature,");
        System.out.println("visit: https://cwiki.apache.org/confluence/display/KAFKA/KIP-932%3A+Queues+for+Kafka");
    }

}