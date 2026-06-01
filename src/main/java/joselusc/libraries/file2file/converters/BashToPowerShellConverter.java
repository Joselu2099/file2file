package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class BashToPowerShellConverter extends AbstractConverter {

    private static final Pattern VAR_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)=(.+)$");
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\[\\s*(.*)\\s*\\];?\\s*then$");
    private static final Pattern IF_NO_THEN_PATTERN = Pattern.compile("^if\\s*\\[\\s*(.*)\\s*\\]$");
    private static final Pattern ELIF_PATTERN = Pattern.compile("^elif\\s*\\[\\s*(.*)\\s*\\];?\\s*then$");
    private static final Pattern FOR_PATTERN = Pattern.compile("^for\\s+([a-zA-Z0-9_]+)\\s+in\\s+([^;]+);?\\s*do$");
    private static final Pattern FOR_NO_DO_PATTERN = Pattern.compile("^for\\s+([a-zA-Z0-9_]+)\\s+in\\s+([^;]+);?$");
    private static final Pattern FUNC_PATTERN = Pattern.compile("^([a-zA-Z0-9_]+)\\(\\)\\s*\\{?$");
    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\s*)");

    private Stack<String> blockStack = new Stack<>();

    public BashToPowerShellConverter() {
    }

    @Override
    protected String getTargetExtension() {
        return ".ps1";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.toString().toLowerCase().endsWith(".sh");
    }

    @Override
    protected String convertContent(String content) throws java.io.IOException {
        blockStack.clear();
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String converted = convertLine(lines[i]);
            if (converted != null && !converted.startsWith("#!")) {
                sb.append(converted);
                if (i < lines.length - 1 && !lines[i].startsWith("#!")) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    @Override
    protected String convertLine(String line) {
        String trimmed = line.trim();
        String indent = getIndent(line);

        if (trimmed.isEmpty() || trimmed.startsWith("#!")) {
            return line;
        }

        if (trimmed.startsWith("#")) {
            return line;
        }

        if (trimmed.startsWith("echo ")) {
            return indent + "Write-Host " + trimmed.substring(5).trim();
        }

        Matcher varMatcher = VAR_PATTERN.matcher(trimmed);
        if (varMatcher.find()) {
            return indent + "$" + varMatcher.group(1) + " = " + varMatcher.group(2);
        }

        Matcher ifMatcher = IF_PATTERN.matcher(trimmed);
        if (ifMatcher.find()) {
            blockStack.push("if");
            String condition = psifyCondition(ifMatcher.group(1).trim());
            return indent + "if (" + condition + ") {";
        }

        Matcher ifNoThenMatcher = IF_NO_THEN_PATTERN.matcher(trimmed);
        if (ifNoThenMatcher.find()) {
            String condition = psifyCondition(ifNoThenMatcher.group(1).trim());
            return indent + "if (" + condition + ")";
        }

        Matcher elifMatcher = ELIF_PATTERN.matcher(trimmed);
        if (elifMatcher.find()) {
            String condition = psifyCondition(elifMatcher.group(1).trim());
            return indent + "} elseif (" + condition + ") {";
        }

        if (trimmed.equals("then")) {
            blockStack.push("if");
            return indent + "{";
        }

        if (trimmed.equals("else")) {
            return indent + "} else {";
        }

        if (trimmed.equals("fi")) {
            if (!blockStack.isEmpty() && blockStack.peek().equals("if")) {
                blockStack.pop();
            }
            return indent + "}";
        }

        Matcher forMatcher = FOR_PATTERN.matcher(trimmed);
        if (forMatcher.find()) {
            blockStack.push("for");
            return indent + "foreach ($" + forMatcher.group(1) + " in " + forMatcher.group(2).trim() + ") {";
        }

        Matcher forNoDoMatcher = FOR_NO_DO_PATTERN.matcher(trimmed);
        if (forNoDoMatcher.find()) {
            return indent + "foreach ($" + forNoDoMatcher.group(1) + " in " + forNoDoMatcher.group(2).trim() + ")";
        }

        if (trimmed.equals("do")) {
            blockStack.push("for");
            return indent + "{";
        }

        if (trimmed.equals("done")) {
            if (!blockStack.isEmpty() && blockStack.peek().equals("for")) {
                blockStack.pop();
            }
            return indent + "}";
        }

        Matcher funcMatcher = FUNC_PATTERN.matcher(trimmed);
        if (funcMatcher.find()) {
            if (trimmed.endsWith("{")) {
                blockStack.push("function");
            }
            return indent + "function " + funcMatcher.group(1) + " {";
        }

        if (trimmed.equals("}")) {
            if (!blockStack.isEmpty() && blockStack.peek().equals("function")) {
                blockStack.pop();
            }
            return indent + "}";
        }

        return line;
    }

    private String psifyCondition(String cond) {
        cond = cond.replaceAll("==", "-eq");
        cond = cond.replaceAll("!=", "-ne");
        cond = cond.replaceAll("=", "-eq");
        return cond;
    }

    private String getIndent(String line) {
        Matcher matcher = INDENT_PATTERN.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
