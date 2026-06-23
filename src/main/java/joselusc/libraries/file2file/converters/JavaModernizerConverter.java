package joselusc.libraries.file2file.converters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Modernizes old Java code syntax.
 */
public class JavaModernizerConverter extends AbstractConverter {

    private static final Pattern DIAMOND_PATTERN = Pattern.compile(
            "(=[\\s]*new[\\s]+[A-Za-z0-9_]+)<[A-Za-z0-9_,\\s\\?]+>\\s*\\(");

    @Override
    protected String getTargetExtension() {
        return ".java";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }

    @Override
    protected String convertContent(String content) throws IOException {
        String result = super.convertContent(content);

        // 1. Switch Expressions
        // case A: return B; -> case A -> B;
        result = result.replaceAll("case\\s+([^:]+):\\s*return\\s+([^;]+);", "case $1 -> $2;");
        // case C: doSomething(); break; -> case C -> { doSomething(); }
        result = result.replaceAll("case\\s+([^:]+):\\s*([^;]+;)\\s*break;", "case $1 -> { $2 }");

        // 2. Text Blocks
        // "..." + \n "..." -> """..."""
        boolean changed = true;
        while (changed) {
            String newContent = result.replaceAll(
                    "\"([^\"]*)\"\\s*\\+\\s*\\r?\\n\\s*\"([^\"]*)\"", "\"\"\"\n$1$2\"\"\"");
            newContent = newContent.replaceAll(
                    "\"\"\"([\\s\\S]*?)\"\"\"\\s*\\+\\s*\\r?\\n\\s*\"([^\"]*)\"", "\"\"\"$1$2\"\"\"");
            if (newContent.equals(result)) {
                changed = false;
            } else {
                result = newContent;
            }
        }

        // 3. Records
        // public class Point { private final int x; public Point(int x) { this.x = x; } }
        // -> public record Point(int x) {}
        result = result.replaceAll(
            "public\\s+class\\s+([A-Za-z0-9_]+)\\s*\\{\\s*private\\s+final\\s+([A-Za-z0-9_]+)\\s+"
            + "([A-Za-z0-9_]+);\\s*public\\s+\\1\\s*\\(\\2\\s+\\3\\)\\s*\\{\\s*this\\.\\3\\s*=\\s*\\3;\\s*\\}\\s*\\}",
            "public record $1($2 $3) {}"
        );

        return result;
    }

    @Override
    protected String convertLine(String line) {
        // Add diamond operator
        // List<String> list = new ArrayList<String>(); -> List<String> list = new ArrayList<>();
        Matcher matcher = DIAMOND_PATTERN.matcher(line);
        line = matcher.replaceAll("$1<>(");

        return line;
    }
}
