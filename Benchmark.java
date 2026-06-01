import joselusc.libraries.file2file.converters.BatchToBashConverter;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public class Benchmark {
    public static void main(String[] args) throws IOException {
        String input = "@echo off\n" +
                       "echo Hello %NAME%\n" +
                       "set VAR=World\n" +
                       "if \"%VAR%\"==\"World\" (\n" +
                       "    echo Yes\n" +
                       ")\n" +
                       "for %%I in (1 2 3) do (\n" +
                       "    echo %%I\n" +
                       ")\n";

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<10000; i++) {
            sb.append(input);
        }
        String largeInput = sb.toString();

        Path sourceFile = Files.createTempFile("benchmark", ".bat");
        Files.writeString(sourceFile, largeInput);
        Path targetFile = Files.createTempFile("benchmark", ".sh");

        BatchToBashConverter converter = new BatchToBashConverter();

        // Warm up
        for (int i=0; i<5; i++) {
            converter.convert(sourceFile, targetFile);
        }

        long start = System.nanoTime();
        for (int i=0; i<10; i++) {
            converter.convert(sourceFile, targetFile);
        }
        long end = System.nanoTime();

        System.out.println("Average time: " + ((end - start) / 10 / 1000000.0) + " ms");
    }
}
