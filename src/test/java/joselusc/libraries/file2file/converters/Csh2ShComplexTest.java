package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class Csh2ShComplexTest {

    @TempDir
    Path tempDir;

    @Test
    void testComplexCshScript() throws IOException {
        Path source = tempDir.resolve("complex.csh");
        Path target = tempDir.resolve("complex.sh");

        String input = "#!/bin/csh\n" +
                       "setenv MY_VAR \"Hello\"\n" +
                       "set index = 0\n" +
                       "while ( $index < 5 )\n" +
                       "    if ( $index == 2 ) then\n" +
                       "        echo \"Found 2\"\n" +
                       "    else if ( $index == 3 ) then\n" +
                       "        echo \"Found 3\"\n" +
                       "    else\n" +
                       "        echo \"Other\"\n" +
                       "    endif\n" +
                       "    @ index++\n" +
                       "end\n" +
                       "\n" +
                       "foreach file ( `ls *.txt` )\n" +
                       "    echo \"Processing $file\"\n" +
                       "end\n" +
                       "\n" +
                       "switch ( $MY_VAR )\n" +
                       "    case \"Hello\":\n" +
                       "        echo \"Hi\"\n" +
                       "        breaksw\n" +
                       "    default:\n" +
                       "        echo \"Unknown\"\n" +
                       "        breaksw\n" +
                       "endsw\n" +
                       "\n" +
                       "goto my_label\n" +
                       "echo \"Skipped\"\n" +
                       "my_label:\n" +
                       "echo \"Here\"\n";

        Files.writeString(source, input);

        Csh2ShConverter converter = Csh2ShConverter.getInstance();
        converter.convert(source, target);

        String output = Files.readString(target);

        // Assert the basics
        assertTrue(output.startsWith("#!/bin/bash"));
        assertTrue(output.contains("export MY_VAR=\"Hello\""));
        assertTrue(output.contains("index=0"));

        // Assert the while loop and if blocks
        assertTrue(output.contains("while [ \"$index\" -lt \"5\" ]; do"));
        assertTrue(output.contains("if [ \"$index\" = \"2\" ]; then"));
        assertTrue(output.contains("elif [ \"$index\" = \"3\" ]; then"));
        assertTrue(output.contains("else"));
        assertTrue(output.contains("fi"));
        assertTrue(output.contains("done"));

        // Assert foreach and backticks
        assertTrue(output.contains("for file in $(ls *.txt); do"));

        // Assert switch/case
        assertTrue(output.contains("case \"$MY_VAR\" in"));
        assertTrue(output.contains("Hello)"));
        assertTrue(output.contains(";;"));
        assertTrue(output.contains("*)"));
        assertTrue(output.contains("esac"));

        // Assert goto
        assertTrue(output.contains("my_label  # goto replaced by function call"));
        assertTrue(output.contains("my_label() {"));
        assertTrue(output.contains("}")); // function closing brace at the end
    }
}
