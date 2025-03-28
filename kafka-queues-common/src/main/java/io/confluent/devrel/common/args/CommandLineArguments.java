package io.confluent.devrel.common.args;

import lombok.Getter;
import org.apache.commons.cli.*;

/**
 * Handles command line argument parsing for the Kafka Queues application.
 */
@Getter
abstract class CommandLineArguments {
    private final String kafkaPropsPath;

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
    }

    public static CommandLine parse(String[] args, Options options, CommandLineParser parser) throws ParseException {
        return parser.parse(options, args);
    }

}