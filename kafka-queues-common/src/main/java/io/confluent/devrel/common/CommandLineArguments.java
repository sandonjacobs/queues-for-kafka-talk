package io.confluent.devrel.common;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Handles command line argument parsing for the Kafka Queues application.
 */
public class CommandLineArguments {
    private String duration;
    private String interval;
    private String numConsumers;
    private boolean helpRequested;

    private CommandLineArguments(String duration, String interval, String numConsumers, boolean helpRequested) {
        this.duration = duration;
        this.interval = interval;
        this.numConsumers = numConsumers;
        this.helpRequested = helpRequested;
    }

    /**
     * Parse command line arguments.
     * 
     * @param args Command line arguments
     * @return Parsed CommandLineArguments
     */
    public static CommandLineArguments parse(String[] args) {
        Options options = new Options();
        
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
        
        options.addOption(Option.builder("c")
                .longOpt("consumers")
                .hasArg()
                .argName("count")
                .desc("Number of shared consumers to start (default: 3, max: 10)")
                .type(Number.class)
                .build());
                
        options.addOption(Option.builder("h")
                .longOpt("help")
                .desc("Display help information")
                .build());
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        
        try {
            cmd = parser.parse(options, args);
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
        
        // Get parameter values or use defaults
        String duration = cmd.getOptionValue("duration", "60");
        String interval = cmd.getOptionValue("interval", "500");
        String numConsumers = cmd.getOptionValue("consumers", "3");
        
        // Validate number of consumers
        try {
            int consumers = Integer.parseInt(numConsumers);
            if (consumers < 1 || consumers > 10) {
                System.err.println("Error: Number of consumers must be between 1 and 10");
                formatter.printHelp("kafka-queues-demo", options, true);
                System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Number of consumers must be a valid integer");
            formatter.printHelp("kafka-queues-demo", options, true);
            System.exit(1);
        }
        
        return new CommandLineArguments(duration, interval, numConsumers, helpRequested);
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

    public String getDuration() {
        return duration;
    }

    public String getInterval() {
        return interval;
    }
    
    public String getNumConsumers() {
        return numConsumers;
    }
    
    public int getNumConsumersAsInt() {
        return Integer.parseInt(numConsumers);
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }
} 