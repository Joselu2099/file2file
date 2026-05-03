package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Modernizes old Java code syntax.
 */
public class JavaModernizerConverter extends AbstractConverter {

    @Override
    protected String getTargetExtension() {
        return ".java";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }

    @Override
    protected String convertLine(String line) {
        // Add diamond operator
        // List<String> list = new ArrayList<String>(); -> List<String> list = new ArrayList<>();
        Pattern diamondPattern = Pattern.compile("(=[\\s]*new[\\s]+[A-Za-z0-9_]+)<[A-Za-z0-9_,\\s\\?]+>\\s*\\(");
        Matcher matcher = diamondPattern.matcher(line);
        line = matcher.replaceAll("$1<>(");

        return line;
    }
}
