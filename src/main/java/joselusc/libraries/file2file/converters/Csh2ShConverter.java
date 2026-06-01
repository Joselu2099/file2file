package joselusc.libraries.file2file.converters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import joselusc.libraries.file2file.converters.interfaces.Converter;

/**
 * {@code Csh2ShConverter} is a singleton class that provides functionality to convert
 * C shell (CSH) scripts into Bash (SH) scripts. The conversion process includes
 * translation of common CSH constructs (such as if, while, foreach, switch, alias, goto)
 * into their Bash equivalents, as well as preservation of indentation, blank lines,
 * and comments.
 * <p>
 * The converter performs a best-effort transformation, but due to differences between
 * CSH and Bash, some advanced or complex CSH features may require manual review
 * after conversion.
 * </p>
 * <h2>Supported Features</h2>
 * <ul>
 *   <li>Automatic insertion of the Bash shebang line (<code>#!/bin/bash</code>).</li>
 *   <li>Preservation of indentation, blank lines, and comments.</li>
 *   <li>Translation of CSH control structures (if, else, endif, while, foreach, switch/case, etc.).</li>
 *   <li>Conversion of variable assignments and environment settings.</li>
 *   <li>Support for alias and unalias commands.</li>
 *   <li>Translation of goto statements and label handling.</li>
 *   <li>Support for source/include statements.</li>
 *   <li>Conversion of backtick command substitutions to <code>$(...)</code> syntax.</li>
 * </ul>
 * <p>
 * <b>Note:</b> Some CSH features (such as advanced goto usage or complex alias definitions)
 * may not be fully compatible and should be reviewed manually in the resulting Bash script.
 * </p>
 *
 * @author joselusc
 */
public class Csh2ShConverter extends AbstractConverter {

    private static final Pattern SETENV_PATTERN = Pattern.compile("^setenv\\s+(\\S+)\\s+(\\S+)");
    private static final Pattern SET_PATTERN = Pattern.compile("^set\\s+(\\S+)\\s*=\\s*(.+)");
    private static final Pattern CD_PATTERN = Pattern.compile("^cd\\s+([^\"].+)$");
    private static final Pattern IF_GOTO_PATTERN = Pattern.compile("^if\\s*\\((.+)\\)\\s*goto\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*$");
    private static final Pattern IF_NEG_PATTERN = Pattern.compile("^if\\s*!\\(\\s*(.+?)\\s*\\)\\s*then");
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s*\\(\\s*\\$?(\\w+)\\s*(==|!=|=|!=)\\s*([\"']?.+?[\"']?)\\s*\\)\\s*then");
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^else\\s+if\\s*\\(\\s*\\$?(\\w+)\\s*(==|!=|=|!=)\\s*([\"']?.+?[\"']?)\\s*\\)\\s*then");
    private static final Pattern WHILE_PATTERN = Pattern.compile("^while\\s*\\(\\s*(.+?)\\s*\\)\\s*");
    private static final Pattern FOREACH_PATTERN = Pattern.compile("^foreach\\s+(\\S+)\\s+\\((.+)\\)");
    private static final Pattern SWITCH_PATTERN = Pattern.compile("^switch\\s*\\(\\s*\\$?(\\w+)\\s*\\)");
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^alias\\s+(\\S+)\\s+(.*)");
    private static final Pattern INDENT_PATTERN = Pattern.compile("^(\\s*)");

    /**
     * Singleton instance of the converter.
     */
    private static Csh2ShConverter instance;

    /**
     * Private constructor to enforce singleton pattern.
     */
    private Csh2ShConverter() {
    }

    @Override
    protected String getTargetExtension() {
        return ".sh";
    }

    @Override
    protected boolean acceptFile(java.nio.file.Path file) {
        return file.getFileName().toString().endsWith(".csh");
    }

    @Override
    public void convert(java.nio.file.Path source, java.nio.file.Path target) throws java.io.IOException {
        if (!java.nio.file.Files.isDirectory(source) && !source.getFileName().toString().endsWith(".csh")) {
            throw new IllegalArgumentException("Expected a .csh file");
        }
        super.convert(source, target);
    }

    /**
     * Returns the singleton instance of {@code Csh2ShConverter}.
     *
     * @return the singleton instance
     */
    public static synchronized Csh2ShConverter getInstance() {
        if (instance == null) {
            instance = new Csh2ShConverter();
        }
        return instance;
    }

    /**
     * Indicates whether the converter is currently inside a function block
     * during the translation process. This is used to properly close function
     * definitions when converting labels and goto statements.
     */
    private boolean functionBlockOpen = false;

    /**
     * Converts a CSH script file (with a <code>.csh</code> extension) to a Bash script
     * (with a <code>.sh</code> extension). The output file is created in the same directory
     * as the input file, with the same base name and a <code>.sh</code> extension.
     *
     * @param inputCshFile the path to the input CSH script file
     * @return a {@link File} object representing the generated Bash script
     * @throws IOException if an I/O error occurs during reading or writing
     * @throws FileNotFoundException if the input file does not exist
     * @throws IllegalArgumentException if the input file does not have a <code>.csh</code> extension
     */
    @Override
    protected String convertContent(String content) throws java.io.IOException {
        functionBlockOpen = false;
        StringBuilder writer = new StringBuilder();

        writer.append("#!/bin/bash\n");
        boolean firstLineProcessed = false;

        String[] lines = content.split("\n", -1);

        for (int i=0; i < lines.length; i++) {
            String line = lines[i];
            line = line.replace("\r", "");

            // Skip the original CSH shebang line
            if (!firstLineProcessed) {
                firstLineProcessed = true;
                if (line.trim().startsWith("#!")) {
                    continue;
                }
            }

            // Preserve blank lines
            if (line.trim().isEmpty()) {
                if (i < lines.length - 1) writer.append("\n");
                continue;
            }

            // Convert the current line and write the result
            String converted = convertLine(line);
            if (!converted.isEmpty()) {
                String[] subLines = converted.split("\n");
                for (int j=0; j < subLines.length; j++) {
                    writer.append(subLines[j]);
                    if (j < subLines.length - 1 || i < lines.length - 1) {
                        writer.append("\n");
                    }
                }
            }
        }

        // Close any open function block at the end of the file
        if (functionBlockOpen) {
            if (!writer.toString().endsWith("\n")) {
                writer.append("\n");
            }
            writer.append("}\n");
        }

        return writer.toString();
    }

    /**
     * Converts a single line of a CSH script, preserving its original indentation.
     * The method extracts the indentation, applies the conversion logic to the
     * content, and then reapplies the indentation to each resulting line.
     *
     * @param line the original line from the CSH script
     * @return the converted Bash line(s) with preserved indentation
     */
    @Override
    protected String convertLine(String line) {
        String indent = getIndent(line);
        String trimmed = line.substring(indent.length());

        trimmed = migrateBackticks(trimmed);

        String result = convertLineLogic(trimmed);

        if (result.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String[] parts = result.split("\n");
        for (int i = 0; i < parts.length; i++) {
            sb.append(indent).append(parts[i]);
            if (i < parts.length - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Applies the main set of translation rules to convert a single line of CSH code
     * (without indentation) to its Bash equivalent.
     * <p>
     * Supported conversions include:
     * <ul>
     *   <li>CSH shebang removal</li>
     *   <li>Block comment markers</li>
     *   <li>setenv and set variable assignments</li>
     *   <li>cd command quoting</li>
     *   <li>if/else/endif/while/foreach/switch/case constructs</li>
     *   <li>alias and unalias commands</li>
     *   <li>goto and label handling</li>
     *   <li>source/include statements</li>
     *   <li>Fallback: returns the line as-is if no rule matches</li>
     * </ul>
     *
     * @param trimmed the line content without indentation
     * @return the converted Bash line(s), or an empty string if the line should be omitted
     */
    private String convertLineLogic(String trimmed) {
        // Remove CSH shebang lines
        if (trimmed.startsWith("#!")) {
            return "";
        }

        // Preserve block comment markers
        if (trimmed.startsWith(": <<'END'") || trimmed.equals("END")) {
            return trimmed;
        }

        // setenv VAR VALUE -> export VAR=VALUE
        Matcher setenvMatcher = SETENV_PATTERN.matcher(trimmed);
        if (setenvMatcher.find()) {
            return "export " + setenvMatcher.group(1) + "=" + setenvMatcher.group(2);
        }

        // set VAR = VALUE -> VAR=VALUE (preserve quotes if present)
        Matcher setMatcher = SET_PATTERN.matcher(trimmed);
        if (setMatcher.find()) {
            String var = setMatcher.group(1);
            String val = setMatcher.group(2).trim();
            // If value is quoted, keep quotes; if not, add quotes if it contains spaces
            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                return var + "=" + val;
            } else if (val.contains(" ")) {
                return var + "=\"" + val + "\"";
            } else {
                return var + "=" + val;
            }
        }

        // cd path -> cd "path" (if not already quoted)
        Matcher cdMatcher = CD_PATTERN.matcher(trimmed);
        if (cdMatcher.find()) {
            return "cd \"" + cdMatcher.group(1).trim() + "\"";
        }

        // Detecta: if ($var == valor) goto label
        Matcher ifGotoMatcher = IF_GOTO_PATTERN.matcher(trimmed);
        if (ifGotoMatcher.find()) {
            String condition = ifGotoMatcher.group(1).trim();
            String label = ifGotoMatcher.group(2).trim();
            // Convierte la condición CSH a Bash
            String bashCondition = bashifyCondition(condition);
            return "if [ " + bashCondition + " ]; then\n" +
                label + "  # goto replaced by function call\n" +
                "fi";
        }

        // if !(condition) then -> if [ ! condition ]; then
        Matcher ifNegMatcher = IF_NEG_PATTERN.matcher(trimmed);
        if (ifNegMatcher.find()) {
            String condition = ifNegMatcher.group(1);
            return "if [ ! " + bashifyCondition(condition) + " ]; then";
        }

        // if ($VAR == VALUE) then -> if [ "$VAR" = "VALUE" ]; then
        Matcher ifMatcher = IF_PATTERN.matcher(trimmed);
        if (ifMatcher.find()) {
            String var = ifMatcher.group(1);
            String op = ifMatcher.group(2).equals("==") ? "=" : ifMatcher.group(2);
            String val = ifMatcher.group(3).replaceAll("^['\"]|['\"]$", "");
            return "if [ \"$" + var + "\" " + op + " \"" + val + "\" ]; then";
        }

        // else if ($VAR == VALUE) then -> elif [ "$VAR" = "VALUE" ]; then
        Matcher elseIfMatcher = ELSE_IF_PATTERN.matcher(trimmed);
        if (elseIfMatcher.find()) {
            String var = elseIfMatcher.group(1);
            String op = elseIfMatcher.group(2).equals("==") ? "=" : elseIfMatcher.group(2);
            String val = elseIfMatcher.group(3).replaceAll("^['\"]|['\"]$", "");
            return "elif [ \"$" + var + "\" " + op + " \"" + val + "\" ]; then";
        }

        // else
        if (trimmed.matches("^else\\s*$")) {
            return "else";
        }

        // endif -> fi
        if (trimmed.equals("endif")) {
            return "fi";
        }

        // while (condition) -> while [ condition ]; do
        Matcher whileMatcher = WHILE_PATTERN.matcher(trimmed);
        if (whileMatcher.find() && trimmed.endsWith(")")) {
            String condition = whileMatcher.group(1);
            return "while [ " + bashifyCondition(condition) + " ]; do";
        }

        // foreach VAR (LIST) -> for VAR in LIST; do
        Matcher foreachMatcher = FOREACH_PATTERN.matcher(trimmed);
        if (foreachMatcher.find()) {
            return "for " + foreachMatcher.group(1) + " in " + foreachMatcher.group(2).trim() + "; do";
        }

        // switch ($var) -> case "$var" in
        Matcher switchMatcher = SWITCH_PATTERN.matcher(trimmed);
        if (switchMatcher.find()) {
            return "case \"$" + switchMatcher.group(1) + "\" in";
        }

        // case VALUE: -> VALUE)
        if (trimmed.matches("^case\\s+.+:")) {
            String label = trimmed.replaceFirst("^case\\s+", "").replaceFirst(":$", "").trim();
            if ((label.startsWith("\"") && label.endsWith("\"")) ||
                (label.startsWith("'") && label.endsWith("'"))) {
                label = label.substring(1, label.length() - 1);
            }
            label = label.trim();
            return label + ")";
        }

        // breaksw -> ;;
        if (trimmed.trim().equals("breaksw")) {
            return ";;";
        }
        // default: -> *)
        if (trimmed.trim().equals("default:")) {
            return "*)";
        }
        // endsw -> esac
        if (trimmed.trim().equals("endsw")) {
            return "esac";
        }

        // end -> done
        if (trimmed.equals("end")) {
            return "done";
        }

        // alias and unalias
        Matcher aliasMatcher = ALIAS_PATTERN.matcher(trimmed);
        if (aliasMatcher.find()) {
            String name = aliasMatcher.group(1);
            String value = aliasMatcher.group(2).trim();
            // Always quote the value for Bash
            if (!value.startsWith("'") && !value.startsWith("\"")) {
                value = "'" + value + "'";
            }
            return "alias " + name + "=" + value;
        }
        if (trimmed.startsWith("unalias ")) {
            return trimmed;
        }

        // goto label or goto $var
        if (trimmed.startsWith("goto ")) {
            String label = trimmed.substring(5).trim();
            // In Bash, goto is not supported. We can call the function if label exists.
            if (label.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
                return label + "  # goto replaced by function call\nreturn";
            } else if (label.matches("^\\$([A-Za-z_][A-Za-z0-9_]*|[0-9]+|\\{[A-Za-z_][A-Za-z0-9_]*\\}|\\{[0-9]+\\})$")) {
                return "\"" + label + "\"\nreturn";
            } else {
                return "# goto " + label + " (not supported in Bash)";
            }
        }

        // label: -> label() {
        if (trimmed.matches("^[A-Za-z_][A-Za-z0-9_]*:$")) {
            String label = trimmed.substring(0, trimmed.length() - 1);
            StringBuilder sb = new StringBuilder();
            if (functionBlockOpen) {
                sb.append("}\n\n");
            }
            sb.append(label).append("() {");
            functionBlockOpen = true;
            return sb.toString();
        }

        // source file -> . file
        if (trimmed.startsWith("source ")) {
            return ". " + trimmed.substring(7).trim();
        }

        // Llamada a label como función (si la línea es solo el nombre de la label)
        if (trimmed.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
            return trimmed + "  # possible function call";
        }

        // Fallback: return the line as-is
        return trimmed;
    }

    /**
     * Converts a CSH condition to a Bash-compatible condition for use in [ ... ].
     * Handles ==, !=, <, >, -eq, -ne, etc.
     *
     * @param cond the CSH condition string
     * @return the Bash-compatible condition string
     */
    private String bashifyCondition(String cond) {
        // Replace == and != with Bash equivalents
        cond = cond.replaceAll("\\$?(\\w+)\\s*==\\s*([\"']?\\w+[\"']?)", "\"\\$$1\" = $2");
        cond = cond.replaceAll("\\$?(\\w+)\\s*!=\\s*([\"']?\\w+[\"']?)", "\"\\$$1\" != $2");
        cond = cond.replaceAll("\\$?(\\w+)\\s*<\\s*(\\w+)", "\"\\$$1\" -lt \"$2\"");
        cond = cond.replaceAll("\\$?(\\w+)\\s*>\\s*(\\w+)", "\"\\$$1\" -gt \"$2\"");
        return cond;
    }

    /**
     * Converts backtick command substitutions (e.g., <code>`command`</code>)
     * to the modern Bash syntax <code>$(command)</code>.
     *
     * @param input the input string possibly containing backticks
     * @return the string with backticks replaced by <code>$(...)</code>
     */
    private String migrateBackticks(String input) {
        return input.replaceAll("`([^`]+)`", "\\$\\($1\\)");
    }

    /**
     * Extracts the leading whitespace (indentation) from a line.
     *
     * @param line the original line
     * @return a string containing only the leading whitespace
     */
    private String getIndent(String line) {
        Matcher matcher = INDENT_PATTERN.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
