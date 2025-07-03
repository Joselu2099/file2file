package joselusc.libraries.file2file.converters.interfaces;

import java.io.File;
import java.io.IOException;

/**
 * Interface for file converters.
 * <p>
 * Implementations of this interface provide logic to convert an input file
 * to a specific output format or encoding.
 * </p>
 *
 * <p>
 * The {@code convert} method takes the path to the input file and returns a {@link File}
 * object representing the converted output file. Implementations should handle
 * any necessary file I/O and throw an {@link IOException} if an error occurs during conversion.
 * </p>
 *
 * <pre>
 * Example usage:
 *   Converter converter = ...;
 *   File output = converter.convert("input.txt");
 * </pre>
 */
public interface Converter {
    /**
     * Converts the specified input file and returns the resulting output file.
     *
     * @param inputPath The path to the input file to be converted.
     * @return A {@link File} object representing the converted output file.
     * @throws IOException If an I/O error occurs during conversion.
     */
    File convert(String inputPath) throws IOException;
}
