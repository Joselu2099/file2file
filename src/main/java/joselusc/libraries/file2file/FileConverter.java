package joselusc.libraries.file2file;

import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.*;

/**
 * Clase principal para gestionar la conversión de archivos usando Apache Commons CLI.
 */
public class FileConverter {

    public static void main(String[] args) {
        // Definir las opciones de línea de comandos
        Options options = new Options();
        Option targetOption = Option.builder("t")
                .required(true)
                .hasArg(true)
                .desc("Tipo de conversión de destino (por ejemplo, sh)")
                .build();
        options.addOption(targetOption);

        // Parseo de los argumentos
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error al parsear los argumentos: " + e.getMessage());
            formatter.printHelp("file2file.jar -t <tipo_destino> <archivo_entrada>", options);
            System.exit(1);
        }

        // Obtener el valor de la opción -t y el argumento posicional (archivo de entrada)
        String targetType = cmd.getOptionValue("t");
        String[] remainingArgs = cmd.getArgs();
        if (remainingArgs.length != 1) {
            System.err.println("Debe especificarse exactamente un archivo de entrada.");
            formatter.printHelp("file2file.jar -t <tipo_destino> <archivo_entrada>", options);
            System.exit(1);
        }
        String inputFilePath = remainingArgs[0];

        // Realizar la conversión
        try {
            Converter converter = ConverterFactory.getConverter(inputFilePath, targetType);
            if (converter == null) {
                throw new IllegalArgumentException("No se encontró un convertidor para " 
                        + inputFilePath + " -> " + targetType);
            }
            File outputFile = converter.convert(inputFilePath);
            System.out.println("Conversión exitosa: " + outputFile.getAbsolutePath());
        } catch (IllegalArgumentException | IOException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
