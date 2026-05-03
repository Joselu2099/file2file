package joselusc.libraries.file2file.converters;

import java.nio.file.Path;

/**
 * Converts JUnit 4 tests to JUnit 5 tests.
 */
public class JUnit4To5Converter extends AbstractConverter {

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
        // Imports
        line = line.replace("import org.junit.Test;", "import org.junit.jupiter.api.Test;");
        line = line.replace("import org.junit.Before;", "import org.junit.jupiter.api.BeforeEach;");
        line = line.replace("import org.junit.After;", "import org.junit.jupiter.api.AfterEach;");
        line = line.replace("import org.junit.BeforeClass;", "import org.junit.jupiter.api.BeforeAll;");
        line = line.replace("import org.junit.AfterClass;", "import org.junit.jupiter.api.AfterAll;");
        line = line.replace("import org.junit.Ignore;", "import org.junit.jupiter.api.Disabled;");
        line = line.replace("import org.junit.Assert;", "import org.junit.jupiter.api.Assertions;");
        line = line.replace("import static org.junit.Assert.", "import static org.junit.jupiter.api.Assertions.");

        // Annotations
        line = line.replace("@Before\n", "@BeforeEach\n");
        line = line.replace("@Before ", "@BeforeEach ");
        if (line.trim().equals("@Before")) line = line.replace("@Before", "@BeforeEach");

        line = line.replace("@After\n", "@AfterEach\n");
        line = line.replace("@After ", "@AfterEach ");
        if (line.trim().equals("@After")) line = line.replace("@After", "@AfterEach");

        line = line.replace("@BeforeClass\n", "@BeforeAll\n");
        line = line.replace("@BeforeClass ", "@BeforeAll ");
        if (line.trim().equals("@BeforeClass")) line = line.replace("@BeforeClass", "@BeforeAll");

        line = line.replace("@AfterClass\n", "@AfterAll\n");
        line = line.replace("@AfterClass ", "@AfterAll ");
        if (line.trim().equals("@AfterClass")) line = line.replace("@AfterClass", "@AfterAll");

        line = line.replace("@Ignore", "@Disabled");

        // Assertions (simple replacements)
        line = line.replaceAll("\\bAssert\\.", "Assertions.");

        return line;
    }

    @Override
    protected String convertContent(String content) throws java.io.IOException {
        // Handle expected exceptions in @Test
        // @Test(expected = Exception.class) -> assertThrows(Exception.class, () -> { ... })
        // This is a complex transformation, we'll do a simple regex for now but it might not cover all cases
        // since we need to wrap the whole method body.
        // A full AST parser would be better, but regex is requested.

        String converted = super.convertContent(content);

        // Very basic substitution for the @Test(expected=...)
        // It's hard to wrap the block without a parser, so we just remove it and add a comment
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("@Test\\s*\\(\\s*expected\\s*=\\s*([^\\)]+)\\s*\\)");
        java.util.regex.Matcher m = p.matcher(converted);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "@Test // TODO: manually wrap with Assertions.assertThrows(" + m.group(1) + ", () -> { ... });");
        }
        m.appendTail(sb);
        converted = sb.toString();

        return converted;
    }
}
