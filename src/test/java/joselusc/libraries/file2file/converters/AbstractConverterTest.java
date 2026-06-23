package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractConverterTest {

    @TempDir
    Path tempDir;

    private TestConverter testConverter;

    @BeforeEach
    void setUp() {
        testConverter = new TestConverter();
    }

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

    @Test
    void testNullExcludes() {
        testConverter.setExcludes(null);
        assertFalse(testConverter.testIsExcluded(Path.of("file.txt")), "Should return false for null excludes");
    }

    @Test
    void testEmptyExcludes() {
        testConverter.setExcludes(Collections.emptyList());
        assertFalse(testConverter.testIsExcluded(Path.of("file.txt")), "Should return false for empty excludes");
    }

    @Test
    void testNullFileName() {
        testConverter.setExcludes(Arrays.asList("file.txt"));
        assertFalse(testConverter.testIsExcluded(Path.of("/")), "Should return false when filename is null");
    }

    @Test
    void testExactMatch() {
        testConverter.setExcludes(Arrays.asList("file.txt", "another.txt"));
        assertTrue(testConverter.testIsExcluded(Path.of("file.txt")), "Should return true for exact match");
        assertTrue(testConverter.testIsExcluded(Path.of("/path/to/another.txt")), "Should return true for exact match on path with parent");
        assertFalse(testConverter.testIsExcluded(Path.of("other.txt")), "Should return false for no match");
    }

    @Test
    void testGlobMatch() {
        testConverter.setExcludes(Arrays.asList("*.txt", "img_?.png"));
        assertTrue(testConverter.testIsExcluded(Path.of("file.txt")), "Should return true for glob match");
        assertTrue(testConverter.testIsExcluded(Path.of("/some/path/img_1.png")), "Should return true for glob match");
        assertFalse(testConverter.testIsExcluded(Path.of("file.java")), "Should return false for no glob match");
        assertFalse(testConverter.testIsExcluded(Path.of("img_12.png")), "Should return false for no glob match");
    }

    @Test
    void testExceptionHandlingInGlob() {
        testConverter.setExcludes(Arrays.asList("[invalid"));
        // This should not throw an exception but return false
        assertFalse(testConverter.testIsExcluded(Path.of("file.txt")), "Should handle invalid glob gracefully and return false");
    }

    // Concrete subclass to test protected methods
    private static class TestConverter extends AbstractConverter {
        @Override
        protected String getTargetExtension() {
            return ".dummy";
        }

        @Override
        protected boolean acceptFile(Path file) {
            return true;
        }

        public boolean testIsExcluded(Path path) {
            return isExcluded(path);
        }
    }
}
