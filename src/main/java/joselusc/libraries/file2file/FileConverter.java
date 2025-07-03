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
     * @param args Command-line arguments. Requires -t &lt;target_type&gt; and &lt;input_file&gt;.
     */
    public static void main(String[] args) {
        // Define command-line options
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

        // Parse command-line arguments
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            formatter.printHelp("file2file.jar -t <target_type> <input_file>", options);
            System.exit(1);
        }

        if (cmd.hasOption("h")) {
            formatter.printHelp("file2file.jar -t <target_type> <input_file>", options);
            System.exit(0);
        }

        // Get the target type and input file from arguments
        String targetType = cmd.getOptionValue("t");
        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs.length != 1) {
            System.err.println("Exactly one input file must be specified.");
            formatter.printHelp("file2file.jar -t <target_type> <input_file>", options);
            System.exit(1);
        }
        String inputFilePath = remainingArgs[0];

        // Perform the conversion
        try {
            Converter converter = ConverterFactory.getConverter(inputFilePath, targetType);
            if (converter == null) {
                throw new IllegalArgumentException("No converter found for " 
                        + inputFilePath + " -> " + targetType);
            }
            File outputFile = converter.convert(inputFilePath);

            // Rename the output file with _CONVERTED and the appropriate extension
            String outputPath = getConvertedFileName(inputFilePath, targetType);
            File renamed = new File(outputPath);
            if (outputFile.renameTo(renamed)) {
                System.out.println("Conversion successful: " + renamed.getAbsolutePath());
            } else {
                System.out.println("Conversion completed, but the output file could not be renamed.");
            }
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Generates the output file name by appending "_CONVERTED" and the appropriate extension
     * based on the target type.
     *
     * @param originalPath The path of the original input file.
     * @param targetType   The target conversion type (e.g., "sh", "encoding").
     * @return The absolute path of the converted file.
     */
    private static String getConvertedFileName(String originalPath, String targetType) {
        File original = new File(originalPath);
        String name = original.getName();
        int dot = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;
        String ext = (targetType.equals("sh")) ? ".sh" : ".txt";
        return new File(original.getParent(), base + "_CONVERTED" + ext).getAbsolutePath();
    }
}
