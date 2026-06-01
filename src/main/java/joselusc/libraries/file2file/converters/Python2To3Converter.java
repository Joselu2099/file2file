package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Converts basic Python 2 syntax to Python 3.
 */
public class Python2To3Converter extends AbstractConverter {

    private static final Pattern PRINT_PATTERN = Pattern.compile("^(\\s*)print\\s+(.*)$");
    private static final Pattern EXCEPT_PATTERN = Pattern.compile("^(\\s*)except\\s+([A-Za-z0-9_]+)\\s*,\\s*([A-Za-z0-9_]+)\\s*:$");

    @Override
    protected String getTargetExtension() {
        return ".py";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.getFileName().toString().endsWith(".py");
    }

    @Override
    protected String convertLine(String line) {
        // print "hello" -> print("hello")
        // Note: this is a simplistic regex that handles the most basic cases
        Matcher printMatcher = PRINT_PATTERN.matcher(line);
        if (printMatcher.find()) {
            String indent = printMatcher.group(1);
            String content = printMatcher.group(2).trim();
            // Ignore if it's already a function call or empty
            if (!content.startsWith("(") && !content.isEmpty()) {
                line = indent + "print(" + content + ")";
            } else if (content.isEmpty()) {
                line = indent + "print()";
            }
        }

        // except Exception, e: -> except Exception as e:
        Matcher exceptMatcher = EXCEPT_PATTERN.matcher(line);
        if (exceptMatcher.find()) {
            line = exceptMatcher.group(1) + "except " + exceptMatcher.group(2) + " as " + exceptMatcher.group(3) + ":";
        }

        // xrange -> range
        line = line.replaceAll("\\bxrange\\s*\\(", "range(");

        // raw_input -> input
        line = line.replaceAll("\\braw_input\\s*\\(", "input(");

        return line;
    }
}
