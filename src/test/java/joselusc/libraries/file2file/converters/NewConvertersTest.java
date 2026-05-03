package joselusc.libraries.file2file.converters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class NewConvertersTest {

    @TempDir
    Path tempDir;

    @Test
    void testJUnit4To5Converter() throws IOException {
        Path source = tempDir.resolve("MyTest.java");
        Path target = tempDir.resolve("MyTest_Converted.java");

        String input = "import org.junit.Test;\n" +
                       "import org.junit.Before;\n" +
                       "import org.junit.Assert;\n" +
                       "\n" +
                       "public class MyTest {\n" +
                       "    @Before\n" +
                       "    public void setUp() {}\n" +
                       "\n" +
                       "    @Test\n" +
                       "    public void testSomething() {\n" +
                       "        Assert.assertEquals(1, 1);\n" +
                       "        Assert.assertTrue(true);\n" +
                       "    }\n" +
                       "    @Test(expected = IllegalArgumentException.class)\n" +
                       "    public void testException() {}\n" +
                       "}\n";

        Files.writeString(source, input);

        JUnit4To5Converter converter = new JUnit4To5Converter();
        converter.convert(source, target);

        String output = Files.readString(target);

        assertTrue(output.contains("import org.junit.jupiter.api.Test;"));
        assertTrue(output.contains("import org.junit.jupiter.api.BeforeEach;"));
        assertTrue(output.contains("import org.junit.jupiter.api.Assertions;"));
        assertTrue(output.contains("@BeforeEach"));
        assertTrue(output.contains("Assertions.assertEquals(1, 1);"));
        assertTrue(output.contains("Assertions.assertTrue(true);"));
        assertTrue(output.contains("@Test // TODO: manually wrap with Assertions.assertThrows(IllegalArgumentException.class, () -> { ... });"));
    }

    @Test
    void testJavaModernizerConverter() throws IOException {
        Path source = tempDir.resolve("ModernizeMe.java");
        Path target = tempDir.resolve("ModernizeMe_Converted.java");

        String input = "import java.util.*;\n" +
                       "public class ModernizeMe {\n" +
                       "    public void doSomething() {\n" +
                       "        List<String> list = new ArrayList<String>();\n" +
                       "        Map<String, Integer> map = new HashMap<String, Integer>();\n" +
                       "    }\n" +
                       "}\n";

        Files.writeString(source, input);

        JavaModernizerConverter converter = new JavaModernizerConverter();
        converter.convert(source, target);

        String output = Files.readString(target);

        assertTrue(output.contains("List<String> list = new ArrayList<>();"));
        assertTrue(output.contains("Map<String, Integer> map = new HashMap<>();"));
    }

    @Test
    void testPython2To3Converter() throws IOException {
        Path source = tempDir.resolve("script.py");
        Path target = tempDir.resolve("script_Converted.py");

        String input = "def hello():\n" +
                       "    print \"Hello world\"\n" +
                       "    for i in xrange(10):\n" +
                       "        print i\n" +
                       "    try:\n" +
                       "        pass\n" +
                       "    except ValueError, e:\n" +
                       "        print \"Error: \", e\n" +
                       "    name = raw_input(\"Name: \")\n";

        Files.writeString(source, input);

        Python2To3Converter converter = new Python2To3Converter();
        converter.convert(source, target);

        String output = Files.readString(target);

        assertTrue(output.contains("print(\"Hello world\")"));
        assertTrue(output.contains("range(10)"));
        assertTrue(output.contains("print(i)"));
        assertTrue(output.contains("except ValueError as e:"));
        assertTrue(output.contains("input(\"Name: \")"));
    }
}
