package joselusc.libraries.file2file.converters;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

import joselusc.libraries.file2file.converters.interfaces.Converter;

/**
 * Utility class for converting the character encoding of text files within a directory tree.
 * <p>
 * This converter is designed for Java and web projects, supporting common file extensions such as
 * .java, .js, .jsp, .xhtml, .html, and .sql. It preserves special characters (e.g., accents, ñ)
 * during conversion and can be used both programmatically and from the command line.
 * </p>
 * <ul>
 *   <li>Supports custom file extensions via command-line or API</li>
 *   <li>Allows specification of source and target encodings</li>
 *   <li>Excludes typical build and repository directories from processing</li>
 *   <li>Optionally creates .bak backups before overwriting files</li>
 *   <li>Silent mode available to suppress per-file output</li>
 * </ul>
 * <p>
 * <b>Command-line usage:</b>
 * <pre>
 *   java EncodingConverter &lt;directory&gt; &lt;sourceEncoding&gt; &lt;targetEncoding&gt; [-ext=ext1,ext2,...] [-nobak] [-silent]
 *   Example: java EncodingConverter ./project windows-1252 UTF-8 -ext=.java,.js,.jsp -nobak -silent
 * </pre>
 * </p>
 */
public class EncodingConverter implements Converter {

    private static final List<String> DEFAULT_EXTENSIONS = Arrays.asList(
        ".java", ".js", ".jsp", ".xhtml", ".html", ".sql"
    );
    private static final Set<String> EXCLUDED_DIRS = new HashSet<>(Arrays.asList(
        "target", ".git", ".svn", "node_modules", "build", "out"
    ));

    /**
     * Converts all supported files in the specified directory from the default source encoding
     * (windows-1252) to the default target encoding (UTF-8).
     * <p>
     * This method is provided for compatibility with the {@link Converter} interface.
     * </p>
     *
     * @param inputPath the root directory to process
     * @return the processed root directory
     * @throws IOException if any I/O error occurs during conversion
     */
    @Override
    public File convert(String inputPath) throws IOException {
        return convertDirectory(
            new File(inputPath),
            Charset.forName("windows-1252"),
            Charset.forName("UTF-8"),
            DEFAULT_EXTENSIONS,
            true,
            false
        );
    }

    /**
     * Converts all files with the specified extensions in the given directory (recursively)
     * from the source encoding to the target encoding.
     *
     * @param root the root directory to process
     * @param sourceEncoding the source character encoding
     * @param targetEncoding the target character encoding
     * @param extensions list of file extensions to convert (e.g., ".java", ".js")
     * @param backup if {@code true}, creates a .bak backup before overwriting each file
     * @param silent if {@code true}, suppresses per-file output messages
     * @return the processed root directory
     * @throws IOException if any I/O error occurs during conversion
     * @throws IllegalArgumentException if {@code root} is not a valid directory
     */
    public static File convertDirectory(
            File root,
            Charset sourceEncoding,
            Charset targetEncoding,
            List<String> extensions,
            boolean backup,
            boolean silent
    ) throws IOException {
        if (!root.isDirectory()) {
            throw new IllegalArgumentException("Not a valid directory: " + root.getAbsolutePath());
        }
        List<File> files = listFilesRecursively(root, extensions);
        for (File file : files) {
            convertFileEncoding(file, sourceEncoding, targetEncoding, backup, silent);
        }
        return root;
    }

    /**
     * Command-line entry point for the encoding converter.
     * <p>
     * Usage:
     * <pre>
     *   java EncodingConverter &lt;directory&gt; &lt;sourceEncoding&gt; &lt;targetEncoding&gt; [-ext=ext1,ext2,...] [-nobak] [-silent]
     * </pre>
     * Example:
     * <pre>
     *   java EncodingConverter ./project windows-1252 UTF-8 -ext=.java,.js,.jsp -nobak -silent
     * </pre>
     * </p>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            printHelp();
            return;
        }

        File root = new File(args[0]).getAbsoluteFile();
        Charset sourceEncoding;
        Charset targetEncoding;
        List<String> extensions = new ArrayList<>(DEFAULT_EXTENSIONS);
        boolean backup = true;
        boolean silent = false;

        // Parse optional flags
        for (int i = 3; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.startsWith("-ext=")) {
                String[] exts = arg.substring(5).split(",");
                extensions = new ArrayList<>();
                for (String ext : exts) {
                    if (!ext.startsWith(".")) ext = "." + ext;
                    extensions.add(ext.toLowerCase());
                }
            } else if (arg.equalsIgnoreCase("-nobak")) {
                backup = false;
            } else if (arg.equalsIgnoreCase("-silent")) {
                silent = true;
            }
        }

        try {
            sourceEncoding = Charset.forName(args[1]);
            targetEncoding = Charset.forName(args[2]);
        } catch (Exception e) {
            System.out.println("Unsupported encoding: " + e.getMessage());
            return;
        }

        try {
            convertDirectory(root, sourceEncoding, targetEncoding, extensions, backup, silent);
            if (!silent) System.out.println("Conversion finished.");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Prints the help message for command-line usage.
     */
    private static void printHelp() {
        System.out.println("Usage: java EncodingConverter <directory> <sourceEncoding> <targetEncoding> [-ext=ext1,ext2,...] [-nobak] [-silent]");
        System.out.println("Example: java EncodingConverter ./project windows-1252 UTF-8 -ext=.java,.js,.jsp -nobak -silent");
        System.out.println("By default, creates .bak backups and converts .java,.js,.jsp,.xhtml,.html,.sql files.");
    }

    /**
     * Recursively lists all files in the specified directory that match the given extensions,
     * excluding typical build and repository folders.
     *
     * @param dir the directory to search
     * @param extensions list of file extensions to include (case-insensitive)
     * @return a list of matching files
     */
    private static List<File> listFilesRecursively(File dir, List<String> extensions) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            if (file.isDirectory()) {
                if (EXCLUDED_DIRS.contains(file.getName())) continue;
                result.addAll(listFilesRecursively(file, extensions));
            } else {
                for (String ext : extensions) {
                    if (file.getName().toLowerCase().endsWith(ext)) {
                        result.add(file);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Converts the encoding of a single file from the specified source encoding to the target encoding.
     * Optionally creates a backup before overwriting the file.
     *
     * @param file the file to convert
     * @param sourceEncoding the source character encoding
     * @param targetEncoding the target character encoding
     * @param backup if {@code true}, creates a .bak backup before overwriting
     * @param silent if {@code true}, suppresses output for this file
     */
    private static void convertFileEncoding(
            File file,
            Charset sourceEncoding,
            Charset targetEncoding,
            boolean backup,
            boolean silent
    ) {
        try {
            // Read the entire content as text using the source encoding
            String content;
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), sourceEncoding)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[4096];
                int len;
                while ((len = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, len);
                }
                content = sb.toString();
            }

            // Create backup if requested
            if (backup) {
                Path bakPath = Paths.get(file.getAbsolutePath() + ".bak");
                if (!Files.exists(bakPath)) {
                    Files.copy(file.toPath(), bakPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // Write the content with the target encoding
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, false), targetEncoding)) {
                writer.write(content);
            }
            if (!silent) System.out.println("Converted: " + file.getPath());
        } catch (IOException e) {
            System.err.println("Error: " + file.getPath() + " → " + e.getMessage());
        }
    }
}