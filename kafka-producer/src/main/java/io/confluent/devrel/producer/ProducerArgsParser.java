package io.confluent.devrel.producer;

import lombok.Getter;
import org.apache.commons.cli.*;

@Getter
public class ProducerArgsParser {

    private final int duration;
    private final int interval;
    private final String kafkaPropsPath;

    private ProducerArgsParser(String kafkaPropsPath, int duration, int interval) {
        this.kafkaPropsPath = kafkaPropsPath;
        this.duration = duration;
        this.interval = interval;
    }

    public static ProducerArgsParser parseOptions(String[] args) {
        Options options = new Options();

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
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            formatter.printHelp("kafka-producer", options, true);
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
        System.out.println("Kafka Producer - Command Line Arguments");
        System.out.println("-------------------------------------");
        System.out.println("This application produces events to a Kafka topic.");
        System.out.println();
        System.out.println("Required Arguments:");
        System.out.println("  -p, --properties <file>    Path to Kafka client properties file");
        System.out.println();
        System.out.println("Optional Arguments:");
        System.out.println("  -d, --duration <seconds>   Duration in seconds for the producer to run");
        System.out.println("                              (default: 60)");
        System.out.println("  -i, --interval <ms>        Interval in milliseconds between producing events");
        System.out.println("                              (default: 500)");
        System.out.println("  -h, --help                 Display this help message");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  java -jar kafka-producer.jar \\");
        System.out.println("    --properties /path/to/properties \\");
        System.out.println("    --duration 120 \\");
        System.out.println("    --interval 1000");
    }

}
