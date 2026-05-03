package joselusc.libraries.file2file.converters.factory;

import joselusc.libraries.file2file.converters.interfaces.Converter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConverterFactoryTest {

    @Test
    void testGetConverterSuccess() {
        Converter converter = ConverterFactory.getConverter("test.csh", "sh");
        assertNotNull(converter);
    }

    @Test
    void testGetConverterUnknownExtension() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ConverterFactory.getConverter("test.unknown", "sh");
        });
        assertTrue(exception.getMessage().contains("No available converters for test.unknown"));
    }

    @Test
    void testGetConverterNoExtension() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ConverterFactory.getConverter("testfile", "sh");
        });
        assertTrue(exception.getMessage().contains("No available converters for testfile"));
    }

    @Test
    void testGetConverterUnknownTargetType() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ConverterFactory.getConverter("test.csh", "unknown");
        });
        assertTrue(exception.getMessage().contains("No conversion from csh to unknown"));
    }
}
