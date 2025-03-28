package io.confluent.devrel.common.args;

import lombok.Getter;
import org.apache.commons.cli.*;

@Getter
public class ProducerArgsParser extends CommandLineArguments {

    private final int duration;
    private final int interval;

    private ProducerArgsParser(String kafkaPropsPath, int duration, int interval) {
        super(kafkaPropsPath);
        this.duration = duration;
        this.interval = interval;
    }

    public static ProducerArgsParser parseOptions(String[] args) {
        Options options = getBaseOptions(args);

        options.addOption(Option.builder("d")
                .longOpt("duration")
                .hasArg()
                .argName("seconds")
                .desc("Duration in seconds for the producer to run (default: 60)")
                .type(Number.class)
                .build());

        options.addOption(Option.builder("i")
                .longOpt("interval")
                .hasArg()
                .argName("milliseconds")
                .desc("Interval in milliseconds between producing events (default: 500)")
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

        showHelp(options, cmd, formatter);

        final int durationArg = Integer.parseInt(cmd.getOptionValue("duration", "60"));
        final int intervalArg = Integer.parseInt(cmd.getOptionValue("interval", "500"));
        final String kafkaPropsPath = cmd.getOptionValue("properties");

        return new ProducerArgsParser(kafkaPropsPath, durationArg, intervalArg);
    }

    public static void showHelp(Options options, CommandLine cmd, HelpFormatter formatter) {
        // Handle help option
        boolean helpRequested = cmd.hasOption("help") || cmd.hasOption("h");
        if (helpRequested) {
            printHelp(options, formatter);
            System.exit(0);
        }
    }

    /**
     * Print help information
     */
    private static void printHelp(Options options, HelpFormatter formatter) {
        System.out.println("\n");
        System.out.println("===============================================");
        System.out.println("          KAFKA QUEUES DEMO HELP              ");
        System.out.println("===============================================");
        System.out.println("\nAvailable command line options:");
        formatter.printHelp("mvn exec:java -Dexec.args=\"[options]\"", options);
        System.out.println("\nExamples:");
        System.out.println("  Default settings (60s duration, 500ms interval, 3 shared consumers):");
        System.out.println("    mvn exec:java");
        System.out.println("  Custom duration and interval:");
        System.out.println("    mvn exec:java -Dexec.args=\"-d 30 -i 200\"");
        System.out.println("  Custom number of shared consumers:");
        System.out.println("    mvn exec:java -Dexec.args=\"-c 5\"");
        System.out.println("  Display this help message:");
        System.out.println("    mvn exec:java -Dexec.args=\"-h\" or");
        System.out.println("    mvn exec:java -Dexec.args=\"--help\"");
        System.out.println("===============================================");
    }

}
