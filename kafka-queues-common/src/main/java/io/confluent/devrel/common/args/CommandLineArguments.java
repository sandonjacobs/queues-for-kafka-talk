package io.confluent.devrel.common.args;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Optional;

/**
 * Handles command line argument parsing for the Kafka Queues application.
 */
abstract class CommandLineArguments {
    private String kafkaPropsPath;

    protected CommandLineArguments(String kafkaPropsPath) {
        this.kafkaPropsPath = kafkaPropsPath;
    }

    protected static Options getBaseOptions(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("c")
                .longOpt("consumers")
                .hasArg()
                .argName("count")
                .desc("Number of shared consumers to start (default: 3, max: 10)")
                .type(Number.class)
                .build());

        options.addOption(Option.builder("p")
                .longOpt("properties")
                .hasArg()
                .argName("properties")
                .desc("Kafka Client properties file path.")
                .type(String.class)
                .build());

        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Display help information")
                .build());

        return options;
        
//        CommandLineParser parser = new DefaultParser();
//        HelpFormatter formatter = new HelpFormatter();
//        CommandLine cmd = null;
//
//        try {
//            cmd = parser.parse(options, args);
//        } catch (ParseException e) {
//            System.err.println("Error parsing command line arguments: " + e.getMessage());
//            formatter.printHelp("kafka-queues-demo", options, true);
//            System.exit(1);
//        }
//
//        // Handle help option
//        boolean helpRequested = cmd.hasOption("help") || cmd.hasOption("h");
//        if (helpRequested) {
//            printHelp(options, formatter);
//            System.exit(0);
//        }
//
//        // Get parameter values or use defaults
//        String duration = cmd.getOptionValue("duration", "60");
//        String interval = cmd.getOptionValue("interval", "500");
//        String numConsumers = cmd.getOptionValue("consumers", "3");
//        String kafkaPropsPath = cmd.getOptionValue("properties");
//
//        // Validate number of consumers
//        try {
//            int consumers = Integer.parseInt(numConsumers);
//            if (consumers < 1 || consumers > 10) {
//                System.err.println("Error: Number of consumers must be between 1 and 10");
//                formatter.printHelp("kafka-queues-demo", options, true);
//                System.exit(1);
//            }
//        } catch (NumberFormatException e) {
//            System.err.println("Error: Number of consumers must be a valid integer");
//            formatter.printHelp("kafka-queues-demo", options, true);
//            System.exit(1);
//        }
//
//        return new CommandLineArguments(duration, interval, numConsumers, helpRequested, kafkaPropsPath);
    }

    public static CommandLine parse(String[] args, Options options, CommandLineParser parser) throws ParseException {
        return parser.parse(options, args);
    }

    public String getKafkaPropsPath() {
        return kafkaPropsPath;
    }
}