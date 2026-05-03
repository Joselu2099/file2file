package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerShellToBashConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void testPowerShellToBashBasic() throws IOException {
        String input = "Write-Host \"Hello\"\n" +
                       "$var = \"World\"\n" +
                       "if ($var -eq \"World\") {\n" +
                       "    Write-Host \"Yes\"\n" +
                       "}\n" +
                       "elseif ($var -ne \"World\") {\n" +
                       "    Write-Host \"No\"\n" +
                       "}\n" +
                       "else {\n" +
                       "    Write-Host \"Maybe\"\n" +
                       "}\n";

        Path sourceFile = tempDir.resolve("script.ps1");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script.sh");

        PowerShellToBashConverter converter = new PowerShellToBashConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        assertTrue(result.contains("#!/bin/bash"));
        assertTrue(result.contains("echo \"Hello\""));
        assertTrue(result.contains("var=\"World\""));
        assertTrue(result.contains("if [ $var == \"World\" ]; then"));
        assertTrue(result.contains("elif [ $var != \"World\" ]; then"));
        assertTrue(result.contains("else"));
        assertTrue(result.contains("fi"));
    }

    @Test
    void testPowerShellToBashLoopsAndFunctions() throws IOException {
        String input = "foreach ($item in $items) {\n" +
                       "    Write-Host $item\n" +
                       "}\n" +
                       "function MyFunc {\n" +
                       "    Write-Host \"Function\"\n" +
                       "}\n";

        Path sourceFile = tempDir.resolve("script_loop.ps1");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script_loop.sh");

        PowerShellToBashConverter converter = new PowerShellToBashConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        assertTrue(result.contains("for item in $items; do"));
        assertTrue(result.contains("echo $item"));
        assertTrue(result.contains("done"));
        assertTrue(result.contains("MyFunc() {"));
        assertTrue(result.contains("echo \"Function\""));
        assertTrue(result.contains("}"));
    }

    @Test
    void testPowerShellToBashNestedEdgeCases() throws IOException {
        String input = "if ($x -eq 1) {\n" +
                       "    foreach ($i in $list) {\n" +
                       "        if ($i -ne 2) {\n" +
                       "            Write-Host $i\n" +
                       "        }\n" +
                       "    }\n" +
                       "}\n";

        Path sourceFile = tempDir.resolve("script_nested.ps1");
        Files.writeString(sourceFile, input);

        Path targetFile = tempDir.resolve("script_nested.sh");

        PowerShellToBashConverter converter = new PowerShellToBashConverter();
        converter.convert(sourceFile, targetFile);

        assertTrue(Files.exists(targetFile));
        String result = Files.readString(targetFile);

        assertTrue(result.contains("if [ $x == 1 ]; then"));
        assertTrue(result.contains("for i in $list; do"));
        assertTrue(result.contains("if [ $i != 2 ]; then"));
        assertTrue(result.contains("echo $i"));
        // Ensure blocks are closed correctly in reverse order
        String[] lines = result.split("\n");
        assertEquals("fi", lines[lines.length - 1].trim());
        assertEquals("done", lines[lines.length - 2].trim());
        assertEquals("fi", lines[lines.length - 3].trim());
    }
}
