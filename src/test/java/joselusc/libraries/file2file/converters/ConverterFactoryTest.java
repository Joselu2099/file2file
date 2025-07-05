package joselusc.libraries.file2file.converters;

import joselusc.libraries.file2file.converters.factory.ConverterFactory;
import joselusc.libraries.file2file.converters.interfaces.Converter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ConverterFactory} to ensure case-insensitive lookups.
 */
class ConverterFactoryTest {

    @Test
    void testCaseInsensitiveLookup() {
        Converter csh = ConverterFactory.getConverter("SCRIPT.CSH", "SH");
        assertNotNull(csh);
        assertTrue(csh instanceof Csh2ShConverter);

        Converter enc = ConverterFactory.getConverter("file.Txt", "ENCODING");
        assertNotNull(enc);
        assertTrue(enc instanceof EncodingConverter);
    }
}
