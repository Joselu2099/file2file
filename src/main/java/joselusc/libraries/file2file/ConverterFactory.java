package joselusc.libraries.file2file;

import joselusc.libraries.file2file.converters.Csh2ShConverter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory class to obtain the appropriate converter based on file extension and target type.
 */
public class ConverterFactory {
    private static final Map<String, Map<String, Supplier<Converter>>> converters = new HashMap<>();

    static {
        // Register the CSH to SH converter using Supplier
        registerConverter("csh", "sh", Csh2ShConverter::getInstance);
    }

    /**
     * Registers a new converter.
     *
     * @param inputExt      The source file extension
     * @param outputExt     The target file extension
     * @param converterSupplier The supplier function providing an instance of the converter
     */
    public static void registerConverter(String inputExt, String outputExt, Supplier<Converter> converterSupplier) {
        converters.computeIfAbsent(inputExt, k -> new HashMap<>()).put(outputExt, converterSupplier);
    }

    /**
     * Retrieves the appropriate converter for the given file type.
     *
     * @param inputFile  The file path
     * @param targetType The target conversion type
     * @return An instance of the corresponding Converter
     * @throws IllegalArgumentException If no suitable converter is found
     */
    public static Converter getConverter(String inputFile, String targetType) throws IllegalArgumentException {
        String extension = getFileExtension(inputFile);

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
     * Extracts the file extension from the file name.
     *
     * @param fileName The file name
     * @return The file extension (without dot) or null if not found
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? null : fileName.substring(dotIndex + 1);
    }
}
