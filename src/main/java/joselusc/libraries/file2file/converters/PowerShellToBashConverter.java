package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class PowerShellToBashConverter extends AbstractConverter {

    private Stack<String> blockStack = new Stack<>();

    public PowerShellToBashConverter() {
    }

    @Override
    protected String getTargetExtension() {
        return ".sh";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.toString().toLowerCase().endsWith(".ps1");
    }

    @Override
    protected String convertContent(String content) throws java.io.IOException {
        blockStack.clear();
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        if (!content.startsWith("#!")) {
            sb.append("#!/bin/bash\n");
        }

        for (int i = 0; i < lines.length; i++) {
            String converted = convertLine(lines[i]);
            if (converted != null) {
                sb.append(converted);
                if (i < lines.length - 1) {
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

        if (trimmed.isEmpty()) {
            return line;
        }

        if (trimmed.startsWith("#")) {
            return line;
        }

        if (trimmed.toLowerCase().startsWith("write-host ")) {
            return indent + "echo " + trimmed.substring(11).trim();
        }

        Pattern varPattern = Pattern.compile("^\\$([a-zA-Z0-9_]+)\\s*=\\s*(.*)$");
        Matcher varMatcher = varPattern.matcher(trimmed);
        if (varMatcher.find()) {
            return indent + varMatcher.group(1) + "=" + varMatcher.group(2);
        }

        Pattern ifPattern = Pattern.compile("^if\\s*\\((.*)\\)\\s*\\{?$");
        Matcher ifMatcher = ifPattern.matcher(trimmed);
        if (ifMatcher.find()) {
            String condition = bashifyCondition(ifMatcher.group(1).trim());
            if (trimmed.endsWith("{")) {
                blockStack.push("if");
                return indent + "if [ " + condition + " ]; then";
            } else {
                return indent + "if [ " + condition + " ]";
            }
        }

        Pattern elseifPattern = Pattern.compile("^elseif\\s*\\((.*)\\)\\s*\\{?$");
        Matcher elseifMatcher = elseifPattern.matcher(trimmed);
        if (elseifMatcher.find()) {
            String condition = bashifyCondition(elseifMatcher.group(1).trim());
            if (trimmed.endsWith("{")) {
                blockStack.push("if");
                return indent + "elif [ " + condition + " ]; then";
            }
        }

        if (trimmed.equals("else {") || trimmed.equals("else{")) {
            return indent + "else";
        }

        Pattern foreachPattern = Pattern.compile("^foreach\\s*\\(\\$([a-zA-Z0-9_]+)\\s+in\\s+(.*)\\)\\s*\\{?$");
        Matcher foreachMatcher = foreachPattern.matcher(trimmed);
        if (foreachMatcher.find()) {
            if (trimmed.endsWith("{")) {
                blockStack.push("for");
                return indent + "for " + foreachMatcher.group(1) + " in " + foreachMatcher.group(2) + "; do";
            }
        }

        if (trimmed.equals("{")) {
            if (!blockStack.isEmpty()) {
                String lastBlock = blockStack.peek();
                if (lastBlock.equals("if")) {
                    return indent + "then";
                } else if (lastBlock.equals("for")) {
                    return indent + "do";
                }
            }
            return indent + "{";
        }

        if (trimmed.equals("}")) {
            if (!blockStack.isEmpty()) {
                String lastBlock = blockStack.pop();
                if (lastBlock.equals("if")) {
                    return indent + "fi";
                } else if (lastBlock.equals("for")) {
                    return indent + "done";
                } else if (lastBlock.equals("function")) {
                    return indent + "}";
                }
            }
            return indent + "}";
        }

        Pattern funcPattern = Pattern.compile("^function\\s+([a-zA-Z0-9_]+)\\s*\\{?$");
        Matcher funcMatcher = funcPattern.matcher(trimmed);
        if (funcMatcher.find()) {
            if (trimmed.endsWith("{")) {
                blockStack.push("function");
            }
            return indent + funcMatcher.group(1) + "() {";
        }

        return line;
    }

    private String bashifyCondition(String cond) {
        cond = cond.replaceAll("-eq", "==");
        cond = cond.replaceAll("-ne", "!=");
        return cond;
    }

    private String getIndent(String line) {
        Pattern pattern = Pattern.compile("^(\\s*)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
