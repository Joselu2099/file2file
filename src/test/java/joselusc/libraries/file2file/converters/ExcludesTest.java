package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ExcludesTest {

    @TempDir
    Path tempDir;

    @Test
    void testExcludesDirectoryAndFile() throws IOException {
        Path source = tempDir.resolve("source");
        Path target = tempDir.resolve("target");
        Files.createDirectories(source);

        Path targetFolder = source.resolve("targetFolder");
        Path gitFolder = source.resolve(".git");
        Path normalFolder = source.resolve("src");

        Files.createDirectories(targetFolder);
        Files.createDirectories(gitFolder);
        Files.createDirectories(normalFolder);

        Files.writeString(targetFolder.resolve("script1.py"), "print 'hello target'");
        Files.writeString(gitFolder.resolve("script2.py"), "print 'hello git'");
        Files.writeString(normalFolder.resolve("script3.py"), "print 'hello src'");

        Files.writeString(source.resolve("app.py"), "print 'hello app'");
        Files.writeString(source.resolve("ignore.py"), "print 'ignore me'");

        Python2To3Converter converter = new Python2To3Converter();
        converter.setExcludes(List.of(".git", "targetFolder", "ignore.py"));

        converter.convert(source, target);

        // Assert that app.py exists
        assertTrue(Files.exists(target.resolve("app.py")));
        assertTrue(Files.readString(target.resolve("app.py")).contains("print('hello app')"));

        // Assert that src/script3.py exists
        assertTrue(Files.exists(target.resolve("src").resolve("script3.py")));
        assertTrue(Files.readString(target.resolve("src").resolve("script3.py")).contains("print('hello src')"));

        // Assert that excluded folders are ignored
        assertFalse(Files.exists(target.resolve("targetFolder")));
        assertFalse(Files.exists(target.resolve(".git")));

        // Assert that excluded file is ignored
        assertFalse(Files.exists(target.resolve("ignore.py")));
    }
}
