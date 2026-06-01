package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void testConvertThrowsIOExceptionWhenSourceDoesNotExist() {
        Path source = tempDir.resolve("non_existent_file.txt");
        Path target = tempDir.resolve("target.txt");

        AbstractConverter converter = new AbstractConverter() {
            @Override
            protected String getTargetExtension() {
                return ".txt";
            }

            @Override
            protected boolean acceptFile(Path file) {
                return true;
            }
        };

        IOException exception = assertThrows(IOException.class, () -> {
            converter.convert(source, target);
        });

        assertEquals("Source path does not exist: " + source, exception.getMessage());
    }
}
