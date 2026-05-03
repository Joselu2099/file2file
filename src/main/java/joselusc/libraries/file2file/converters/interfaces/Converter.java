package joselusc.libraries.file2file.converters.interfaces;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for file converters.
 */
public interface Converter {

    /**
     * Converts a file or directory from source to target path.
     * @param source the source file or directory
     * @param target the target file or directory
     * @throws IOException if an I/O error occurs
     */
    void convert(Path source, Path target) throws IOException;

    /**
     * Sets a list of directories or patterns to exclude during the conversion process.
     * @param excludes a list of patterns to exclude
     */
    default void setExcludes(java.util.List<String> excludes) {}

    /**
     * Configures the dry-run mode. If true, no files should be modified.
     * @param dryRun true to enable dry-run mode
     */
    default void setDryRun(boolean dryRun) {
        // Default implementation does nothing
    }

    /**
     * Configures the backup mode. If true, existing files should be backed up
     * before being overwritten.
     * @param backup true to enable backup mode
     */
    default void setBackup(boolean backup) {
        // Default implementation does nothing
    }

    /**
     * Converts the specified input file and returns the resulting output file.
     * Legacy method for backward compatibility.
     *
     * @param inputPath The path to the input file to be converted.
     * @return A {@link File} object representing the converted output file.
     * @throws IOException If an I/O error occurs during conversion.
     */
    default File convert(String inputPath) throws IOException {
        Path source = Path.of(inputPath);

        // This is a hack to preserve backward compatibility for Csh2ShConverter's specific checks before checking for file existence
        if (this.getClass().getSimpleName().equals("Csh2ShConverter") && !inputPath.endsWith(".csh")) {
            throw new IllegalArgumentException("Expected a .csh file");
        }

        if (!java.nio.file.Files.exists(source)) {
            throw new java.io.FileNotFoundException("Input file does not exist: " + inputPath);
        }
        String fileName = source.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = (dot > 0) ? fileName.substring(0, dot) : fileName;
        String ext = (dot > 0) ? fileName.substring(dot) : "";
        Path target = source.resolveSibling(base + "_CONVERTED" + ext);
        convert(source, target);
        return target.toFile();
    }
}
