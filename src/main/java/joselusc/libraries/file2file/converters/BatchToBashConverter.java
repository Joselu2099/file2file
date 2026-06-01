package joselusc.libraries.file2file.converters;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class BatchToBashConverter extends AbstractConverter {

    private static final Pattern VAR_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");
    private static final Pattern SET_PATTERN = Pattern.compile("^set\\s+([a-zA-Z0-9_]+)=(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s+(not\\s+)?(exist\\s+)?(.+?)\\s*(\\(?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOR_PATTERN = Pattern.compile("^for\\s+%%([a-zA-Z])\\s+in\\s+\\((.*)\\)\\s*do\\s*(\\(?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\s*)");

    private Stack<String> blockStack = new Stack<>();

    public BatchToBashConverter() {
    }

    @Override
    protected String getTargetExtension() {
        return ".sh";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.toString().toLowerCase().endsWith(".bat") || file.toString().toLowerCase().endsWith(".cmd");
    }

    @Override
    protected String convertContent(String content) throws java.io.IOException {
        blockStack.clear();
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();

        sb.append("#!/bin/bash\n");

        for (int i = 0; i < lines.length; i++) {
            String converted = convertLine(lines[i]);
            if (converted != null && !converted.isEmpty()) {
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

        if (trimmed.toLowerCase().startsWith("rem ") || trimmed.startsWith("::")) {
            return indent + "#" + trimmed.substring(trimmed.startsWith("::") ? 2 : 4);
        }

        if (trimmed.toLowerCase().equals("@echo off") || trimmed.toLowerCase().equals("echo off")) {
            return "";
        }

        if (trimmed.toLowerCase().startsWith("echo ")) {
            return indent + "echo " + convertVariables(trimmed.substring(5).trim());
        }

        Matcher setMatcher = SET_PATTERN.matcher(trimmed);
        if (setMatcher.find()) {
            return indent + setMatcher.group(1) + "=" + convertVariables(setMatcher.group(2));
        }

        Matcher ifMatcher = IF_PATTERN.matcher(trimmed);
        if (ifMatcher.find()) {
            String not = ifMatcher.group(1);
            String exist = ifMatcher.group(2);
            String condition = ifMatcher.group(3).trim();
            String paren = ifMatcher.group(4);

            StringBuilder bashCond = new StringBuilder();
            if (not != null) {
                bashCond.append("! ");
            }
            if (exist != null) {
                bashCond.append("-e ").append(convertVariables(condition));
            } else {
                bashCond.append(bashifyCondition(convertVariables(condition)));
            }

            if (paren != null && paren.equals("(")) {
                blockStack.push("if");
                return indent + "if [ " + bashCond.toString() + " ]; then";
            } else {
                return indent + "if [ " + bashCond.toString() + " ]";
            }
        }

        Matcher forMatcher = FOR_PATTERN.matcher(trimmed);
        if (forMatcher.find()) {
            String var = forMatcher.group(1);
            String items = convertVariables(forMatcher.group(2));
            String paren = forMatcher.group(3);

            if (paren != null && paren.equals("(")) {
                blockStack.push("for");
                return indent + "for " + var + " in " + items + "; do";
            }
        }

        if (trimmed.equals("(")) {
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

        if (trimmed.equals(")")) {
            if (!blockStack.isEmpty()) {
                String lastBlock = blockStack.pop();
                if (lastBlock.equals("if")) {
                    return indent + "fi";
                } else if (lastBlock.equals("for")) {
                    return indent + "done";
                }
            }
            return indent + "}";
        }

        return indent + convertVariables(trimmed);
    }

    private String convertVariables(String str) {
        Matcher matcher = VAR_PATTERN.matcher(str);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "\\$" + matcher.group(1));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String bashifyCondition(String cond) {
        cond = cond.replaceAll("==", "=");
        cond = cond.replaceAll("(?i)EQU", "=");
        cond = cond.replaceAll("(?i)NEQ", "!=");
        cond = cond.replaceAll("(?i)LSS", "-lt");
        cond = cond.replaceAll("(?i)LEQ", "-le");
        cond = cond.replaceAll("(?i)GTR", "-gt");
        cond = cond.replaceAll("(?i)GEQ", "-ge");
        return cond;
    }

    private String getIndent(String line) {
        Matcher matcher = INDENT_PATTERN.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
