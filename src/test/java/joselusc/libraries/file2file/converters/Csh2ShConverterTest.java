package joselusc.libraries.file2file.converters;

import java.io.File;
import java.io.IOException;

public class Csh2ShConverterTest {
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java FileConverter <archivo.csh>");
            System.exit(1);
        }
        try {
            File converted = Csh2ShConverter.getInstance().convert(args[0]);
            System.out.println("Archivo convertido: " + converted.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(2);
        }
    }
}
