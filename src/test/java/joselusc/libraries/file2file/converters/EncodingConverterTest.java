package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for the {@link EncodingConverter} class.
 * <p>
 * This test suite verifies the correct conversion of files between character encodings,
 * backup file creation, silent mode operation, extension-based filtering, and the
 * exclusion of build directories from processing.
 * </p>
 */
class EncodingConverterTest {

    /**
     * Charset for Windows-1252 encoding.
     */
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    /**
     * Charset for UTF-8 encoding.
     */
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Temporary directory used for test file operations.
     */
    private Path tempDir;

    /**
     * Creates a temporary directory before each test.
     *
     * @throws IOException if the temporary directory cannot be created
     */
    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("encoding-test");
    }

    /**
     * Deletes the temporary directory and all its contents after each test.
     *
     * @throws IOException if an error occurs during deletion
     */
    @AfterEach
    void tearDown() throws IOException {
        deleteRecursively(tempDir);
    }

    /**
     * Tests that a single file is correctly converted from WINDOWS-1252 to UTF-8 encoding,
     * and that a backup file is created.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testConvertSingleFile() throws IOException {
        Path file = tempDir.resolve("test.java");
        String original = "áéíóú ñ Ñ";
        Files.write(file, original.getBytes(WINDOWS_1252));

        EncodingConverter.convertDirectory(
            tempDir.toFile(), WINDOWS_1252, UTF_8,
            Arrays.asList(".java"), true, false
        );

        String result = new String(Files.readAllBytes(file), UTF_8);
        assertEquals(original, result);
        assertTrue(Files.exists(tempDir.resolve("test.java.bak")));
    }

    /**
     * Tests that no backup file is created when the backup option is set to false.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testNoBackupOption() throws IOException {
        Path file = tempDir.resolve("test.js");
        Files.write(file, "á".getBytes(WINDOWS_1252));

        EncodingConverter.convertDirectory(
            tempDir.toFile(), WINDOWS_1252, UTF_8,
            Arrays.asList(".js"), false, false
        );

        assertFalse(Files.exists(tempDir.resolve("test.js.bak")));
    }

    /**
     * Tests that no output is printed to standard output when silent mode is enabled.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testSilentMode() throws IOException {
        Path file = tempDir.resolve("test.jsp");
        Files.write(file, "á".getBytes(WINDOWS_1252));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        EncodingConverter.convertDirectory(
            tempDir.toFile(), WINDOWS_1252, UTF_8,
            Arrays.asList(".jsp"), true, true
        );

        System.setOut(originalOut);
        assertEquals("", out.toString().trim());
    }

    /**
     * Tests that files located in excluded directories (such as build folders)
     * are not converted or backed up.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testExcludedDir() throws IOException {
        Path excluded = tempDir.resolve("target");
        Files.createDirectory(excluded);
        Path file = excluded.resolve("test.java");
        Files.write(file, "á".getBytes(WINDOWS_1252));

        EncodingConverter.convertDirectory(
            tempDir.toFile(), WINDOWS_1252, UTF_8,
            Arrays.asList(".java"), true, false
        );

        // No backup or conversion should occur in excluded folders
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(excluded.resolve("test.java.bak")));
        String content = new String(Files.readAllBytes(file), WINDOWS_1252);
        assertEquals("á", content);
    }

    /**
     * Tests that only files with the specified extension are converted,
     * and other files remain unchanged.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testExtensionFilter() throws IOException {
        Path file1 = tempDir.resolve("a.java");
        Path file2 = tempDir.resolve("b.txt");
        Files.write(file1, "á".getBytes(WINDOWS_1252));
        Files.write(file2, "á".getBytes(WINDOWS_1252));

        EncodingConverter.convertDirectory(
            tempDir.toFile(), WINDOWS_1252, UTF_8,
            Arrays.asList(".java"), true, false
        );

        // Only .java file is converted to UTF-8
        assertEquals("á", new String(Files.readAllBytes(file1), UTF_8));
        assertEquals("á", new String(Files.readAllBytes(file2), WINDOWS_1252));
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param path the directory or file to delete
     * @throws IOException if an I/O error occurs during deletion
     */
    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}
