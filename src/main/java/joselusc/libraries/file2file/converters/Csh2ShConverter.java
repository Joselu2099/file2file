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

import joselusc.libraries.file2file.Converter;

/**
 * Converts CSH scripts into Bash (SH) scripts with various transformations:
 * <ul>
 * <li>Adds the shebang line "#!/bin/bash" if it is not already present.</li>
 * <li>Attempts to preserve indentation, blank lines, and whitespace as much as
 * possible.</li>
 * <li>Converts common CSH constructs (e.g. <code>if</code>, <code>while</code>,
 * <code>foreach</code>, <code>switch</code>, <code>alias</code>,
 * <code>goto</code>) into their Bash equivalents.</li>
 * <li>Performs a best-effort approach to handle various CSH features, but
 * complete compatibility is not guaranteed due to inherent differences between
 * CSH and Bash.</li>
 * </ul>
 *
 * <p>
 * Note that certain CSH features (especially those involving complex
 * <code>goto</code> usage or advanced alias parameters) may require additional
 * manual review in the resulting Bash script.
 * </p>
 */
public class Csh2ShConverter implements Converter {

    /**
     * Singleton instance to enforce a single converter usage pattern.
     */
    private static Csh2ShConverter instance;

    /**
     * Prevents direct instantiation from outside. Use {@link #getInstance()}
     * instead.
     */
    private Csh2ShConverter() {
    }

    /**
     * Retrieves (and creates if necessary) the singleton instance of
     * {@code Csh2ShConverter}.
     *
     * @return the shared converter instance
     */
    public static synchronized Csh2ShConverter getInstance() {
        if (instance == null) {
            instance = new Csh2ShConverter();
        }
        return instance;
    }

    /**
     * Flag indicating whether the converter is currently inside a function
     * block in the CSH-to-Bash translation process. This is reset whenever a
     * new label (function) is opened, or an existing function is closed.
     */
    private boolean functionBlockOpen = false;

    /**
     * Converts an input file with a {@code .csh} extension into a Bash script
     * with a {@code .sh} suffix. The resulting file is written in the same
     * directory as the input, with various transformations to adapt CSH syntax
     * into Bash.
     *
     * @param inputCshFile the path of the <code>.csh</code> file to convert
     * @return a {@link File} object corresponding to the newly created
     * <code>.sh</code> file
     * @throws IOException if any file I/O error occurs
     * @throws FileNotFoundException if {@code inputCshFile} does not exist
     * @throws IllegalArgumentException if {@code inputCshFile} does not end
     * with <code>.csh</code>
     */
    @Override
    public File convert(String inputCshFile) throws IOException {
        File inputFile = new File(inputCshFile);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file does not exist: " + inputCshFile);
        }
        if (!inputFile.getName().endsWith(".csh")) {
            throw new IllegalArgumentException("Expected a .csh file: " + inputCshFile);
        }

        // Generate the output file name by replacing .csh with .sh
        String outputFileName = inputFile.getName().replaceFirst("\\.csh$", ".sh");
        File outputFile = new File(inputFile.getParent(), outputFileName);
        Path inPath = inputFile.toPath();
        Path outPath = outputFile.toPath();

        // Reset state for each conversion
        functionBlockOpen = false;

        try (BufferedReader reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8); BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {

            // Write out the shebang for Bash
            writer.write("#!/bin/bash\n");

            boolean firstLineProcessed = false;
            String line;
            while ((line = reader.readLine()) != null) {
                // Remove potential carriage returns to ensure proper LF endings
                line = line.replace("\r", "");

                // Skip the first line if it was a CSH-style shebang (e.g. #!/bin/csh)
                if (!firstLineProcessed) {
                    firstLineProcessed = true;
                    if (line.trim().startsWith("#!")) {
                        continue;
                    }
                }

                // Preserve blank lines
                if (line.trim().isEmpty()) {
                    writer.write("\n");
                    continue;
                }

                // Convert one line of CSH code into Bash code
                String converted = convertLine(line);
                if (!converted.isEmpty()) {
                    // The conversion may produce multiple sub-lines
                    for (String subLine : converted.split("\n")) {
                        writer.write(subLine);
                        writer.write("\n");
                    }
                }
            }

            // If we ended in the middle of a function block, close it properly
            if (functionBlockOpen) {
                writer.write("}\n");
            }
        }

        return outputFile;
    }

    /**
     * Splits the given line into indentation and content. Removes the
     * indentation from the beginning, converts the remaining content to Bash,
     * then reapplies the original indentation to each line of the conversion
     * result.
     *
     * @param line a single line of the original .csh script
     * @return a potentially multi-line string with converted Bash code plus
     * preserved indentation
     */
    private String convertLine(String line) {
        // Extract indentation
        String indent = getIndent(line);
        // Remove indentation from the front
        String trimmed = line.substring(indent.length());

        // Replace backticks with $(...) syntax in Bash
        trimmed = migrateBackticks(trimmed);

        // Perform the main logic to convert from CSH to Bash
        String result = convertLineLogic(trimmed);

        if (result.isEmpty()) {
            return "";
        }

        // Reapply the indentation to each new line produced by the conversion
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
     * Applies the set of translation rules to convert the content of a single
     * line from CSH syntax to Bash syntax.
     * <p>
     * Includes:
     * <ul>
     * <li>Removing CSH shebang lines.</li>
     * <li>Handling block comments (<code>: <<'END'</code> ...
     * <code>END</code>).</li>
     * <li>Converting <code>setenv</code> to <code>export</code>.</li>
     * <li>Translating <code>set var = value</code> into
     * <code>var=value</code>.</li>
     * <li>Adapting <code>cd path</code> into <code>cd \"path\"</code> if
     * unquoted.</li>
     * <li>Mapping <code>if !( condition ) then</code> into
     * <code>if [ ! condition ]; then</code>.</li>
     * <li>Translating <code>if ($VAR == VALUE) then</code> (or !=) into Bash
     * <code>if [ ... ]; then</code>.</li>
     * <li>Turning <code>else if</code> into <code>elif</code>,
     * <code>endif</code> into <code>fi</code>, <code>end</code> into
     * <code>done</code>.</li>
     * <li>Converting <code>while ( condition )</code> into
     * <code>while [ condition ]; do</code>, <code>foreach var (list)</code>
     * into <code>for var in list; do</code>.</li>
     * <li>Switch-case constructs
     * (<code>switch ... case ... breaksw ... endsw</code>) become
     * <code>case ... esac</code> in Bash.</li>
     * <li>Alias definitions and <code>unalias</code> are preserved/adapted
     * accordingly.</li>
     * <li><code>goto label</code> becomes a function call with
     * <code>; return</code>. For dynamic labels (<code>goto $f_next</code>), it
     * uses <code>eval</code>.</li>
     * <li>Detecting labels (<code>MyLabel:</code>) transforms them into Bash
     * functions (<code>MyLabel() { ... }</code>) and closes any existing
     * function block if open.</li>
     * <li><code>source file</code> becomes <code>. file</code>.</li>
     * </ul>
     *
     * @param trimmed a single line of code, without its original indentation
     * @return the transformed Bash equivalent (possibly empty if the line is
     * removed)
     */
    private String convertLineLogic(String trimmed) {
        // 1. Remove csh shebang lines
        if (trimmed.startsWith("#!")) {
            return "";
        }

        // 2. Keep lines like ": <<'END'" or "END" (block comment markers)
        if (trimmed.startsWith(": <<'END'") || trimmed.equals("END")) {
            return trimmed;
        }

        // 3. setenv var value -> export var=value
        Pattern setenvPattern = Pattern.compile("^setenv\\s+(\\S+)\\s+(\\S+)");
        Matcher setenvMatcher = setenvPattern.matcher(trimmed);
        if (setenvMatcher.find()) {
            return "export " + setenvMatcher.group(1) + "=" + setenvMatcher.group(2);
        }

        // 4. set VAR = VALUE -> VAR=VALUE
        Pattern setPattern = Pattern.compile("^set\\s+(\\S+)\\s*=\\s*(.+)");
        Matcher setMatcher = setPattern.matcher(trimmed);
        if (setMatcher.find()) {
            return setMatcher.group(1) + "=" + setMatcher.group(2);
        }

        // 5. cd path -> cd "path" (if unquoted)
        Pattern cdPattern = Pattern.compile("^cd\\s+([^\"].+)$");
        Matcher cdMatcher = cdPattern.matcher(trimmed);
        if (cdMatcher.find()) {
            return "cd \"" + cdMatcher.group(1) + "\"";
        }

        // 6. if !( condition ) then -> if [ ! condition ]; then
        Pattern ifNegPattern = Pattern.compile("^if\\s*!\\(\\s*(.+?)\\s*\\)\\s*then");
        Matcher ifNegMatcher = ifNegPattern.matcher(trimmed);
        if (ifNegMatcher.find()) {
            String condition = ifNegMatcher.group(1);
            // Attempt to quote arguments if it detects operators like -e
            condition = condition.replaceAll("(-[a-zA-Z])\\s+(\\S+)", "$1 \"$2\"");
            return "if [ ! " + condition + " ]; then";
        }

        // 7. if ($VAR == VALUE) then -> if [ "$VAR" = "VALUE" ]; then
        Pattern ifPattern = Pattern.compile("^if\\s*\\(\\s*\\$?(\\S+)\\s*(==|!=)\\s*(\\S+)\\s*\\)\\s*then");
        Matcher ifMatcher = ifPattern.matcher(trimmed);
        if (ifMatcher.find()) {
            String operator = ifMatcher.group(2).equals("==") ? "=" : "!=";
            return "if [ \"" + ifMatcher.group(1) + "\" " + operator + " \"" + ifMatcher.group(3) + "\" ]; then";
        }

        // 7.1 else if
        Pattern elseIfPattern = Pattern.compile("^else\\s+if\\s*\\(\\s*\\$?(\\S+)\\s*(==|!=)\\s*(\\S+)\\s*\\)\\s*then");
        Matcher elseIfMatcher = elseIfPattern.matcher(trimmed);
        if (elseIfMatcher.find()) {
            String operator = elseIfMatcher.group(2).equals("==") ? "=" : "!=";
            return "elif [ \"" + elseIfMatcher.group(1) + "\" " + operator + " \"" + elseIfMatcher.group(3) + "\" ]; then";
        }

        // 7.2 else
        if (trimmed.matches("^else\\s*$")) {
            return "else";
        }

        // 8. endif -> fi
        if (trimmed.equals("endif")) {
            return "fi";
        }

        // 9. while ( condition ) -> while [ condition ]; do
        Pattern whilePattern = Pattern.compile("^while\\s*\\(\\s*(.+?)\\s*\\)\\s*");
        Matcher whileMatcher = whilePattern.matcher(trimmed);
        if (whileMatcher.find() && trimmed.endsWith(")")) {
            String condition = whileMatcher.group(1);
            // Basic replacements for <, >, ==, !=
            condition = condition.replaceAll("\\$?(\\S+)\\s*<\\s*(\\S+)", "\"$1\" -lt \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*>\\s*(\\S+)", "\"$1\" -gt \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*==\\s*(\\S+)", "\"$1\" = \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*!=\\s*(\\S+)", "\"$1\" != \"$2\"");
            return "while [ " + condition + " ]; do";
        }

        // 10. foreach VAR (LIST) -> for VAR in LIST; do
        Pattern foreachPattern = Pattern.compile("^foreach\\s+(\\S+)\\s+\\((.+)\\)");
        Matcher foreachMatcher = foreachPattern.matcher(trimmed);
        if (foreachMatcher.find()) {
            return "for " + foreachMatcher.group(1) + " in " + foreachMatcher.group(2) + "; do";
        }

        // 11. switch ($var) -> case "$var" in
        Pattern switchPattern = Pattern.compile("^switch\\s*\\(\\s*\\$?(\\S+)\\s*\\)");
        Matcher switchMatcher = switchPattern.matcher(trimmed);
        if (switchMatcher.find()) {
            return "case \"" + switchMatcher.group(1) + "\" in";
        }

        // 11.1 case VALUE: -> VALUE)
        if (trimmed.matches("^case\\s+.+:")) {
            String label = trimmed.replaceFirst("^case\\s+", "").replace(":", "");
            return label + ")";
        }
        // breaksw -> ;;
        if (trimmed.trim().equals("breaksw")) {
            return ";;";
        }
        // default: -> *)
        if (trimmed.trim().startsWith("default:")) {
            return "*)";
        }
        // endsw -> esac
        if (trimmed.trim().equals("endsw")) {
            return "esac";
        }

        // 12. end -> done
        if (trimmed.equals("end")) {
            return "done";
        }

        // 13. alias (alias X Y -> alias X='Y', unalias X -> unalias X)
        Pattern aliasPattern = Pattern.compile("^alias\\s+(\\S+)\\s+(.*)");
        Matcher aliasMatcher = aliasPattern.matcher(trimmed);
        if (aliasMatcher.find()) {
            String name = aliasMatcher.group(1);
            String value = aliasMatcher.group(2).trim();
            if (!value.startsWith("'") && value.contains(" ")) {
                value = "'" + value + "'";
            }
            return "alias " + name + "=" + value;
        }
        if (trimmed.startsWith("unalias ")) {
            return trimmed;
        }

        // 14. goto label -> label; return (or eval "$var"; return)
        if (trimmed.startsWith("goto ")) {
            String label = trimmed.substring(5).trim();
            if (label.startsWith("$")) {
                return "eval \"" + label + "\"\nreturn";
            } else {
                return label + "\nreturn";
            }
        }

        // 15. label: -> label() { ... } (closes any open function first)
        if (trimmed.matches("^[A-Za-z_][A-Za-z0-9_]*:$")) {
            String label = trimmed.substring(0, trimmed.length() - 1);
            StringBuilder sb = new StringBuilder();
            if (functionBlockOpen) {
                sb.append("}\n\n"); // close previous function
            }
            sb.append(label).append("() {");
            functionBlockOpen = true;
            return sb.toString();
        }

        // 16. source -> . file
        if (trimmed.startsWith("source ")) {
            return ". " + trimmed.substring(7);
        }

        // No transformation rule matched, so return the line as-is
        return trimmed;
    }

    /**
     * Replaces backtick command substitutions (e.g. <code>`command`</code>)
     * with the modern Bash syntax <code>$(command)</code>.
     *
     * @param input the line or segment containing possible backtick usage
     * @return the same string with backticks replaced by $(...)
     */
    private String migrateBackticks(String input) {
        return input.replaceAll("`([^`]+)`", "\\$\\($1\\)");
    }

    /**
     * Extracts leading whitespace (indentation) from a line, preserving it so
     * that it can be reapplied to the converted line(s).
     *
     * @param line the original line
     * @return a string containing only the leading spaces or tabs
     */
    private String getIndent(String line) {
        Pattern pattern = Pattern.compile("^(\\s*)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
