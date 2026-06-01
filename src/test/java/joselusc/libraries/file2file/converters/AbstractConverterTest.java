package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractConverterTest {

    private TestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TestConverter();
    }

    @Test
    void testNullExcludes() {
        converter.setExcludes(null);
        assertFalse(converter.testIsExcluded(Path.of("file.txt")), "Should return false for null excludes");
    }

    @Test
    void testEmptyExcludes() {
        converter.setExcludes(Collections.emptyList());
        assertFalse(converter.testIsExcluded(Path.of("file.txt")), "Should return false for empty excludes");
    }

    @Test
    void testNullFileName() {
        converter.setExcludes(Arrays.asList("file.txt"));
        assertFalse(converter.testIsExcluded(Path.of("/")), "Should return false when filename is null");
    }

    @Test
    void testExactMatch() {
        converter.setExcludes(Arrays.asList("file.txt", "another.txt"));
        assertTrue(converter.testIsExcluded(Path.of("file.txt")), "Should return true for exact match");
        assertTrue(converter.testIsExcluded(Path.of("/path/to/another.txt")), "Should return true for exact match on path with parent");
        assertFalse(converter.testIsExcluded(Path.of("other.txt")), "Should return false for no match");
    }

    @Test
    void testGlobMatch() {
        converter.setExcludes(Arrays.asList("*.txt", "img_?.png"));
        assertTrue(converter.testIsExcluded(Path.of("file.txt")), "Should return true for glob match");
        assertTrue(converter.testIsExcluded(Path.of("/some/path/img_1.png")), "Should return true for glob match");
        assertFalse(converter.testIsExcluded(Path.of("file.java")), "Should return false for no glob match");
        assertFalse(converter.testIsExcluded(Path.of("img_12.png")), "Should return false for no glob match");
    }

    @Test
    void testExceptionHandlingInGlob() {
        converter.setExcludes(Arrays.asList("[invalid"));
        // This should not throw an exception but return false
        assertFalse(converter.testIsExcluded(Path.of("file.txt")), "Should handle invalid glob gracefully and return false");
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
