package joselusc.libraries.file2file;

import java.io.File;
import java.io.IOException;

/**
 * Interfaz para los convertidores de archivos.
 */
public interface Converter {
    File convert(String inputPath) throws IOException;
}
