package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BashToPowerShellConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void testBashToPowerShellBasic() throws IOException {
        String input = "#!/bin/bash\n" +
                       "echo \"Hello\"\n" +
                       "var=\"World\"\n" +
                       "if [ $var == \"World\" ]; then\n" +
                       "    echo \"Yes\"\n" +
                       "elif [ $var != \"World\" ]; then\n" +
                       "    echo \"No\"\n" +
                       "else\n" +
                       "    echo \"Maybe\"\n" +
                       "fi\n";

        Path sourceFile = tempDir.resolve("script.sh");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script.ps1");

        BashToPowerShellConverter converter = new BashToPowerShellConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        assertTrue(result.contains("Write-Host \"Hello\""));
        assertTrue(result.contains("$var = \"World\""));
        assertTrue(result.contains("if ($var -eq \"World\") {"));
        assertTrue(result.contains("} elseif ($var -ne \"World\") {"));
        assertTrue(result.contains("} else {"));
        assertTrue(result.contains("}"));
    }

    @Test
    void testBashToPowerShellLoopsAndFunctions() throws IOException {
        String input = "for item in $items; do\n" +
                       "    echo $item\n" +
                       "done\n" +
                       "MyFunc() {\n" +
                       "    echo \"Function\"\n" +
                       "}\n";

        Path sourceFile = tempDir.resolve("script_loop.sh");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script_loop.ps1");

        BashToPowerShellConverter converter = new BashToPowerShellConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        System.out.println("RESULT OF LOOPS AND FUNCTIONS: " + result);
        assertTrue(result.contains("foreach ($item in $items) {"));
        assertTrue(result.contains("Write-Host $item"));
        assertTrue(result.contains("function MyFunc {"));
        assertTrue(result.contains("Write-Host \"Function\""));

        // Ensure blocks are closed
        String[] lines = result.split("\n");
        assertEquals("}", lines[lines.length - 1].trim());
        assertEquals("}", lines[2].trim()); // The 'done' equivalent
    }

    @Test
    void testBashToPowerShellNestedEdgeCases() throws IOException {
        String input = "if [ $x == 1 ]; then\n" +
                       "    for i in $list; do\n" +
                       "        if [ $i != 2 ]; then\n" +
                       "            echo $i\n" +
                       "        fi\n" +
                       "    done\n" +
                       "fi\n";

        Path sourceFile = tempDir.resolve("script_nested.sh");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script_nested.ps1");

        BashToPowerShellConverter converter = new BashToPowerShellConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        assertTrue(result.contains("if ($x -eq 1) {"));
        assertTrue(result.contains("foreach ($i in $list) {"));
        assertTrue(result.contains("if ($i -ne 2) {"));
        assertTrue(result.contains("Write-Host $i"));

        // Ensure blocks are closed correctly in reverse order
        String[] lines = result.split("\n");
        assertEquals("}", lines[lines.length - 1].trim());
        assertEquals("}", lines[lines.length - 2].trim());
        assertEquals("}", lines[lines.length - 3].trim());
    }
}
