package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class BashToPowerShellConverter extends AbstractConverter {

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

        Pattern varPattern = Pattern.compile("^([a-zA-Z0-9_]+)=(.+)$");
        Matcher varMatcher = varPattern.matcher(trimmed);
        if (varMatcher.find()) {
            return indent + "$" + varMatcher.group(1) + " = " + varMatcher.group(2);
        }

        Pattern ifPattern = Pattern.compile("^if\\s*\\[\\s*(.*)\\s*\\];?\\s*then$");
        Matcher ifMatcher = ifPattern.matcher(trimmed);
        if (ifMatcher.find()) {
            blockStack.push("if");
            String condition = psifyCondition(ifMatcher.group(1).trim());
            return indent + "if (" + condition + ") {";
        }

        Pattern ifNoThenPattern = Pattern.compile("^if\\s*\\[\\s*(.*)\\s*\\]$");
        Matcher ifNoThenMatcher = ifNoThenPattern.matcher(trimmed);
        if (ifNoThenMatcher.find()) {
            String condition = psifyCondition(ifNoThenMatcher.group(1).trim());
            return indent + "if (" + condition + ")";
        }

        Pattern elifPattern = Pattern.compile("^elif\\s*\\[\\s*(.*)\\s*\\];?\\s*then$");
        Matcher elifMatcher = elifPattern.matcher(trimmed);
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

        Pattern forPattern = Pattern.compile("^for\\s+([a-zA-Z0-9_]+)\\s+in\\s+([^;]+);?\\s*do$");
        Matcher forMatcher = forPattern.matcher(trimmed);
        if (forMatcher.find()) {
            blockStack.push("for");
            return indent + "foreach ($" + forMatcher.group(1) + " in " + forMatcher.group(2).trim() + ") {";
        }

        Pattern forNoDoPattern = Pattern.compile("^for\\s+([a-zA-Z0-9_]+)\\s+in\\s+([^;]+);?$");
        Matcher forNoDoMatcher = forNoDoPattern.matcher(trimmed);
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

        Pattern funcPattern = Pattern.compile("^([a-zA-Z0-9_]+)\\(\\)\\s*\\{?$");
        Matcher funcMatcher = funcPattern.matcher(trimmed);
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
        Pattern pattern = Pattern.compile("^(\\s*)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
