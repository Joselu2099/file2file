package joselusc.libraries.file2file.converters.factory;

import joselusc.libraries.file2file.converters.interfaces.Converter;
import joselusc.libraries.file2file.converters.Csh2ShConverter;
import joselusc.libraries.file2file.converters.EncodingConverter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory class for obtaining the appropriate {@link Converter} implementation
 * based on the input file extension and the desired target conversion type.
 * <p>
 * This class maintains a registry of available converters and provides methods
 * to register new converters and retrieve them for use in file conversion operations.
 * </p>
 *
 * <p>
 * Example usage:
 * <pre>
 *   Converter converter = ConverterFactory.getConverter("script.csh", "sh");
 *   File output = converter.convert("script.csh");
 * </pre>
 * </p>
 *
 * <p>
 * To add a new converter, use:
 * <pre>
 *   ConverterFactory.registerConverter("txt", "encoding", EncodingConverter::getInstance);
 * </pre>
 * </p>
 *
 * @author Jose Luis Sanchez Carrasco
 */
public class ConverterFactory {
    /**
     * Registry of converters, organized by input file extension and target type.
     * The outer map key is the input file extension (without dot),
     * the inner map key is the target type, and the value is a {@link Supplier}
     * that provides an instance of the corresponding {@link Converter}.
     */
    private static final Map<String, Map<String, Supplier<Converter>>> converters = new HashMap<>();

    static {
        // Register the CSH to SH converter using a Supplier
        registerConverter("csh", "sh", Csh2ShConverter::getInstance);

        // Register the EncodingConverter for various file types
        registerConverter("sql", "encoding", EncodingConverter::new);
        registerConverter("java", "encoding", EncodingConverter::new);
        registerConverter("js", "encoding", EncodingConverter::new);
        registerConverter("jsp", "encoding", EncodingConverter::new);
        registerConverter("xhtml", "encoding", EncodingConverter::new);
        registerConverter("html", "encoding", EncodingConverter::new);
        registerConverter("txt", "encoding", EncodingConverter::new);
    }

    /**
     * Registers a new converter for a specific input extension and target type.
     *
     * @param inputExt          The source file extension (without dot)
     * @param outputExt         The target conversion type (e.g., "sh", "encoding")
     * @param converterSupplier A supplier function that provides an instance of the converter
     */
    public static void registerConverter(String inputExt, String outputExt, Supplier<Converter> converterSupplier) {
        String inKey = inputExt.toLowerCase();
        String outKey = outputExt.toLowerCase();
        converters.computeIfAbsent(inKey, k -> new HashMap<>()).put(outKey, converterSupplier);
    }

    /**
     * Retrieves the appropriate converter for the given input file and target type.
     *
     * @param inputFile  The path to the input file
     * @param targetType The desired target conversion type (e.g., "sh", "encoding")
     * @return An instance of the corresponding {@link Converter}
     * @throws IllegalArgumentException If no suitable converter is found for the input file and target type
     */
    public static Converter getConverter(String inputFile, String targetType) throws IllegalArgumentException {
        String extension = getFileExtension(inputFile);
        if (extension != null) {
            extension = extension.toLowerCase();
        }
        targetType = targetType.toLowerCase();

        if (extension == null || !converters.containsKey(extension)) {
            throw new IllegalArgumentException("No available converters for " + inputFile);
        }

        Map<String, Supplier<Converter>> targetConverters = converters.get(extension);
        if (targetConverters.containsKey(targetType)) {
            return targetConverters.get(targetType).get(); // Use the Supplier to get an instance
        }

        throw new IllegalArgumentException("No conversion from " + extension + " to " + targetType);
    }

    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName The file name or path
     * @return The file extension (without the dot), or {@code null} if not found
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? null : fileName.substring(dotIndex + 1);
    }
}
