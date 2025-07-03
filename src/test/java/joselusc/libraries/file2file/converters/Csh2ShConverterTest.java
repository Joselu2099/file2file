package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for the {@link Csh2ShConverter} class.
 * <p>
 * This test suite verifies the correct conversion of CSH scripts to Bash scripts,
 * including the handling of special characters, control structures, variable assignments,
 * aliases, labels, and error conditions.
 * </p>
 */
class Csh2ShConverterTest {

    /**
     * Temporary directory used for creating test files.
     */
    private Path tempDir;

    /**
     * Creates a temporary directory before each test.
     *
     * @throws IOException if the temporary directory cannot be created
     */
    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("csh2sh-test");
    }

    /**
     * Cleans up the temporary directory after each test.
     *
     * @throws IOException if an error occurs during cleanup
     */
    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    /**
     * Tests the conversion of a simple CSH script containing a shebang,
     * environment variable assignment, and an if statement.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testConvertSimpleCshScript() throws IOException {
        String cshScript = "#!/bin/csh\nsetenv VAR valor\nif ($VAR == valor) then\necho ok\nendif\n";
        Path cshFile = tempDir.resolve("test.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("#!/bin/bash"));
        assertTrue(shContent.contains("export VAR=valor"));
        assertTrue(shContent.contains("if [ \"$VAR\" = \"valor\" ]; then"));
        assertTrue(shContent.contains("fi"));
    }

    /**
     * Tests the conversion of scripts containing accented characters and special symbols.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testConvertWithTildes() throws IOException {
        String cshScript = "echo áéíóú ñ Ñ";
        Path cshFile = tempDir.resolve("tildes.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("áéíóú ñ Ñ"));
    }

    /**
     * Tests the conversion of CSH foreach and while loops to their Bash equivalents.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testForeachAndWhileConversion() throws IOException {
        String cshScript = "foreach f (a b c)\necho $f\nend\nwhile ($i < 10)\necho $i\nend";
        Path cshFile = tempDir.resolve("loops.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("for f in a b c; do"));
        assertTrue(shContent.contains("done"));
        assertTrue(shContent.contains("while [ \"$i\" -lt \"10\" ]; do"));
    }

    /**
     * Tests the conversion of CSH switch/case constructs to Bash case statements.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testSwitchCaseConversion() throws IOException {
        String cshScript = "switch ($var)\ncase 1:\necho one\nbreaksw\ndefault:\necho other\nendsw";
        Path cshFile = tempDir.resolve("switch.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("case \"$var\" in"));
        assertTrue(shContent.contains("1)"));
        assertTrue(shContent.contains(";;"));
        assertTrue(shContent.contains("*)"));
        assertTrue(shContent.contains("esac"));
    }

    /**
     * Tests the conversion of alias and unalias commands from CSH to Bash.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testAliasAndUnaliasConversion() throws IOException {
        String cshScript = "alias ll ls -l\nunalias ll";
        Path cshFile = tempDir.resolve("alias.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("alias ll='ls -l'"));
        assertTrue(shContent.contains("unalias ll"));
    }

    /**
     * Tests the conversion of goto statements and labels from CSH to Bash functions and calls.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testGotoAndLabelConversion() throws IOException {
        String cshScript = "goto label1\nlabel1:\necho here\ngoto $next";
        Path cshFile = tempDir.resolve("goto.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("label1() {"));
        assertTrue(shContent.contains("label1"));
        assertTrue(shContent.contains("eval \"$next\""));
        assertTrue(shContent.contains("return"));
    }

    /**
     * Tests the conversion of the CSH 'source' command to the Bash dot (.) command.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testSourceConversion() throws IOException {
        String cshScript = "source mylib.csh";
        Path cshFile = tempDir.resolve("source.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains(". mylib.csh"));
    }

    /**
     * Tests that comments and blank lines are preserved during conversion.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testCommentsAndBlankLinesPreserved() throws IOException {
        String cshScript = "# comment\n\n: <<'END'\nblock\nEND\n";
        Path cshFile = tempDir.resolve("comments.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("# comment"));
        assertTrue(shContent.contains(": <<'END'"));
        assertTrue(shContent.contains("block"));
        assertTrue(shContent.contains("END"));
        // Should preserve blank lines
        assertTrue(shContent.contains("\n\n"));
    }

    /**
     * Tests the conversion of setenv and set variable assignments from CSH to Bash.
     *
     * @throws IOException if file operations fail
     */
    @Test
    void testSetenvAndSetConversion() throws IOException {
        String cshScript = "setenv VAR1 value1\nset VAR2 = value2";
        Path cshFile = tempDir.resolve("setenvset.csh");
        Files.write(cshFile, cshScript.getBytes());

        File shFile = Csh2ShConverter.getInstance().convert(cshFile.toString());
        String shContent = new String(Files.readAllBytes(shFile.toPath()));

        assertTrue(shContent.contains("export VAR1=value1"));
        assertTrue(shContent.contains("VAR2=value2"));
    }

    /**
     * Tests that an IllegalArgumentException is thrown when attempting to convert a non-.csh file.
     */
    @Test
    void testErrorOnNonCshFile() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            Csh2ShConverter.getInstance().convert("not_a_csh.txt");
        });
        assertTrue(ex.getMessage().contains("Expected a .csh file"));
    }

    /**
     * Tests that a FileNotFoundException is thrown when the input file does not exist.
     */
    @Test
    void testErrorOnMissingFile() {
        Exception ex = assertThrows(FileNotFoundException.class, () -> {
            Csh2ShConverter.getInstance().convert("missing.csh");
        });
        assertTrue(ex.getMessage().contains("does not exist"));
    }

}
