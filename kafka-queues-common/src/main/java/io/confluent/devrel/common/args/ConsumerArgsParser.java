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
                .desc("Number of shared consumers to start (default: 3, max: 10)")
                .type(Number.class)
                .build());


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = ProducerArgsParser.parse(args, options, parser);
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

        final int numConsumerArg = Integer.parseInt(cmd.getOptionValue("interval", "500"));
        final String kafkaPropsPath = cmd.getOptionValue("properties");
        return new ConsumerArgsParser(kafkaPropsPath, numConsumerArg);
    }

    private static void printHelp(Options options, HelpFormatter formatter) {
        // TODO show the help for consumer arg usage
    }

}