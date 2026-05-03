package joselusc.libraries.file2file;

import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;

import joselusc.libraries.file2file.converters.interfaces.Converter;
import joselusc.libraries.file2file.converters.factory.ConverterFactory;

/**
 * Main class for managing file conversions using Apache Commons CLI.
 * <p>
 * This class provides a command-line interface to convert files using the available converters.
 * The user must specify the target conversion type and the input file.
 * The converted file will be saved in the same directory as the input file, with the suffix "_CONVERTED"
 * and the appropriate extension.
 * </p>
 *
 * <pre>
 * Usage:
 *   java -jar file2file.jar -t &lt;target_type&gt; &lt;input_file&gt;
 * Example:
 *   java -jar file2file.jar -t sh script.csh
 *   java -jar file2file.jar -t encoding file.txt
 * </pre>
 *
 * Supported target types:
 * <ul>
 *   <li>sh - Convert CSH scripts to Bash (SH)</li>
 *   <li>encoding - General encoding conversion</li>
 * </ul>
 *
 * The program will print a success message with the output file path, or an error message if the conversion fails.
 *
 * @author Jose Luis Sanchez Carrasco
 */
public class FileConverter {

    /**
     * Entry point for the file2file command-line application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        Options options = new Options();
        Option targetOption = Option.builder("t")
                .required(true)
                .hasArg(true)
                .desc("Target conversion type (sh, encoding)")
                .build();
        options.addOption(targetOption);

        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Show help message")
                .build();
        options.addOption(helpOption);

        Option dryRunOption = Option.builder()
                .longOpt("dry-run")
                .desc("Show what would be modified and a diff without writing to disk")
                .build();
        options.addOption(dryRunOption);

        Option backupOption = Option.builder()
                .longOpt("backup")
                .desc("Create a backup copy (.bak) before overwriting any file")
                .build();
        options.addOption(backupOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("file2file.jar -t <target_type> <source_path> [target_path]", options);
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("file2file.jar -t <target_type> <source_path> [target_path]", options);
            System.exit(0);
        }

        String targetType = cmd.getOptionValue("t");
        String[] remainingArgs = cmd.getArgs();

        if (remainingArgs.length < 1 || remainingArgs.length > 2) {
            System.err.println("You must specify a source path and optionally a target path.");
            formatter.printHelp("file2file.jar -t <target_type> <source_path> [target_path]", options);
            System.exit(1);
        }

        String inputFilePath = remainingArgs[0];

        try {
            Converter converter = ConverterFactory.getConverter(inputFilePath, targetType);
            if (converter == null) {
                throw new IllegalArgumentException("No converter found for " + inputFilePath + " -> " + targetType);
            }

            if (cmd.hasOption("dry-run")) {
                converter.setDryRun(true);
            }
            if (cmd.hasOption("backup")) {
                converter.setBackup(true);
            }

            if (remainingArgs.length == 2) {
                // New way with source and target paths
                java.nio.file.Path source = java.nio.file.Path.of(inputFilePath);
                java.nio.file.Path target = java.nio.file.Path.of(remainingArgs[1]);
                converter.convert(source, target);
                System.out.println("Conversion successful: " + target.toAbsolutePath());
            } else {
                // Legacy way
                File outputFile = converter.convert(inputFilePath);
                System.out.println("Conversion successful: " + outputFile.getAbsolutePath());
            }
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
