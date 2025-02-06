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
 * Extended converter from CSH to Bash.
 * <p>
 * This converter performs several transformations:
 * <ul>
 *   <li>Adds the shebang "#!/bin/bash" if not already present.</li>
 *   <li>Preserves indentation, blank lines, and whitespace.</li>
 *   <li>Converts common CSH constructs (e.g., if, while, foreach, switch, alias, goto)
 *       into their Bash equivalents.</li>
 *   <li>Attempts to cover a wide range of cases; however, 100% compatibility is not guaranteed.</li>
 * </ul>
 * </p>
 */
public class Csh2ShConverter implements Converter {

    // Singleton instance
    private static Csh2ShConverter instance;

    // Private constructor to enforce the singleton pattern
    private Csh2ShConverter() {}

    /**
     * Returns the singleton instance of Csh2ShConverter.
     *
     * @return the instance of Csh2ShConverter
     */
    public static synchronized Csh2ShConverter getInstance() {
        if (instance == null) {
            instance = new Csh2ShConverter();
        }
        return instance;
    }

    // Flag to track if a function block is currently open
    private boolean functionBlockOpen = false;

    /**
     * Converts a .csh file to a .sh file with the suffix "_REFACTORED.sh".
     *
     * @param inputCshFile the path to the input .csh file
     * @return the resulting converted File object
     * @throws IOException if an I/O error occurs or if the input file is invalid
     */
    public File convert(String inputCshFile) throws IOException {
        File inputFile = new File(inputCshFile);
        if (!inputFile.exists()) {
            throw new FileNotFoundException("Input file does not exist: " + inputCshFile);
        }
        if (!inputFile.getName().endsWith(".csh")) {
            throw new IllegalArgumentException("Expected a .csh file: " + inputCshFile);
        }

        String outputFileName = inputFile.getName().replaceFirst("\\.csh$", ".sh");
        File outputFile = new File(inputFile.getParent(), outputFileName);
        Path inPath = inputFile.toPath();
        Path outPath = outputFile.toPath();

        functionBlockOpen = false;

        try (BufferedReader reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8);
             BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {

            // Write the Bash interpreter shebang line
            writer.write("#!/bin/bash");
            writer.newLine();

            boolean firstLineProcessed = false;
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip the first line if it is a shebang (e.g., for CSH)
                if (!firstLineProcessed) {
                    firstLineProcessed = true;
                    if (line.trim().startsWith("#!")) {
                        continue;
                    }
                }

                // Preserve blank lines
                if (line.trim().isEmpty()) {
                    writer.newLine();
                    continue;
                }

                String converted = convertLine(line);
                if (!converted.isEmpty()) {
                    // The result may contain multiple lines separated by newline characters
                    for (String subLine : converted.split("\n")) {
                        writer.write(subLine);
                        writer.newLine();
                    }
                }
            }

            // Close any open function block at the end of the file
            if (functionBlockOpen) {
                writer.write("}");
                writer.newLine();
            }
        }

        return outputFile;
    }

    /**
     * Extracts the leading indentation from a line and applies conversion rules.
     *
     * @param line the original line of code
     * @return the converted line with preserved indentation
     */
    private String convertLine(String line) {
        String indent = getIndent(line);
        String trimmed = line.substring(indent.length());

        // Migrate backticks `command` to $(command)
        trimmed = migrateBackticks(trimmed);

        // Apply conversion rules to the trimmed line
        String result = convertLineLogic(trimmed);

        if (result.isEmpty()) {
            return "";
        }
        // Reapply the original indentation to each line of the result
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
     * Applies a set of conversion rules to transform a CSH command into its Bash equivalent.
     *
     * The conversion rules include:
     * 1. <b>Shebang Removal:</b> Lines starting with "#!" are omitted.
     * 2. <b>Comment Blocks:</b> Pass through CSH comment block markers such as ": <<'END'" and "END".
     * 3. <b>Environment Variable Setting:</b> {@code setenv X Y} is converted to {@code export X=Y}.
     * 4. <b>Variable Assignment:</b> {@code set X = Y} becomes {@code X=Y}.
     * 5. <b>Change Directory:</b> {@code cd PATH} is converted to {@code cd "PATH"} if the path is not already quoted.
     * 6. <b>If with Negation:</b> {@code if !( condition ) then} is converted to {@code if [ ! condition ]; then}, attempting to add quotes where necessary.
     * 7. <b>If Conditions:</b> {@code if ($VAR == VALUE) then} and {@code if ($VAR != VALUE) then} are converted to their Bash equivalents with proper quoting.
     * 8. <b>Else If:</b> {@code else if (condition) then} is converted to {@code elif [ condition ]; then}.
     * 9. <b>Else:</b> A line containing only "else" remains unchanged.
     * 10. <b>If End:</b> {@code endif} is converted to {@code fi}.
     * 11. <b>While Loop:</b> {@code while ( condition )} is converted to {@code while [ condition ]; do}, with a best-effort translation of relational operators.
     * 12. <b>Foreach Loop:</b> {@code foreach VAR (LIST)} is converted to {@code for VAR in LIST; do}.
     * 13. <b>Switch Statement:</b> {@code switch ($var)} becomes {@code case "$var" in}, with subsequent case rules applied:
     *      - {@code case VALUE:} becomes {@code VALUE)}.
     *      - {@code breaksw} becomes {@code ;;}.
     *      - {@code default:} becomes {@code *)}.
     *      - {@code endsw} becomes {@code esac}.
     * 14. <b>Loop End:</b> The keyword {@code end} is converted to {@code done}, closing loops.
     * 15. <b>Alias Definitions:</b> {@code alias X Y} is converted to {@code alias X='Y'} (adding single quotes if needed),
     *     while {@code unalias X} remains unchanged.
     * 16. <b>Goto Statements:</b> {@code goto label} is transformed into a function call by outputting the label followed by a {@code return}.
     *     If the label starts with a '$', it is evaluated using {@code eval}.
     * 17. <b>Function Definitions:</b> A line with a label in the form {@code Label:} is converted to a Bash function definition
     *     {@code Label() {}}, closing any previously open function block.
     * 18. <b>Source Command:</b> {@code source X} is converted to {@code . X}.
     *
     * @param trimmed the line with leading whitespace removed
     * @return the transformed line according to the conversion rules
     */
    private String convertLineLogic(String trimmed) {
        // 1. Skip shebang lines
        if (trimmed.startsWith("#!")) {
            return "";
        }

        // 2. Pass through comment block markers (e.g., ": <<'END'" and "END")
        if (trimmed.startsWith(": <<'END'") || trimmed.equals("END")) {
            return trimmed;
        }

        // 3. Convert "setenv X Y" to "export X=Y"
        Pattern setenvPattern = Pattern.compile("^setenv\\s+(\\S+)\\s+(\\S+)");
        Matcher setenvMatcher = setenvPattern.matcher(trimmed);
        if (setenvMatcher.find()) {
            return "export " + setenvMatcher.group(1) + "=" + setenvMatcher.group(2);
        }

        // 4. Convert "set X = Y" to "X=Y"
        Pattern setPattern = Pattern.compile("^set\\s+(\\S+)\\s*=\\s*(.+)");
        Matcher setMatcher = setPattern.matcher(trimmed);
        if (setMatcher.find()) {
            return setMatcher.group(1) + "=" + setMatcher.group(2);
        }

        // 5. Convert "cd PATH" to "cd \"PATH\""
        Pattern cdPattern = Pattern.compile("^cd\\s+([^\"].+)$");
        Matcher cdMatcher = cdPattern.matcher(trimmed);
        if (cdMatcher.find()) {
            return "cd \"" + cdMatcher.group(1) + "\"";
        }

        // 6. Convert "if !( condition ) then" to "if [ ! condition ]; then"
        Pattern ifNegPattern = Pattern.compile("^if\\s*!\\(\\s*(.+?)\\s*\\)\\s*then");
        Matcher ifNegMatcher = ifNegPattern.matcher(trimmed);
        if (ifNegMatcher.find()) {
            String condition = ifNegMatcher.group(1);
            // Attempt to add quotes around operands when detecting operators like -e
            condition = condition.replaceAll("(-[a-zA-Z])\\s+(\\S+)", "$1 \"$2\"");
            return "if [ ! " + condition + " ]; then";
        }

        // 7. Convert "if ($VAR == VALUE) then" and "if ($VAR != VALUE) then" to Bash if statements
        Pattern ifPattern = Pattern.compile("^if\\s*\\(\\s*\\$?(\\S+)\\s*(==|!=)\\s*(\\S+)\\s*\\)\\s*then");
        Matcher ifMatcher = ifPattern.matcher(trimmed);
        if (ifMatcher.find()) {
            String operator = ifMatcher.group(2).equals("==") ? "=" : "!=";
            return "if [ \"" + ifMatcher.group(1) + "\" " + operator + " \"" + ifMatcher.group(3) + "\" ]; then";
        }

        // 7.1 Convert "else if (condition) then" to "elif [ condition ]; then"
        Pattern elseIfPattern = Pattern.compile("^else\\s+if\\s*\\(\\s*\\$?(\\S+)\\s*(==|!=)\\s*(\\S+)\\s*\\)\\s*then");
        Matcher elseIfMatcher = elseIfPattern.matcher(trimmed);
        if (elseIfMatcher.find()) {
            String operator = elseIfMatcher.group(2).equals("==") ? "=" : "!=";
            return "elif [ \"" + elseIfMatcher.group(1) + "\" " + operator + " \"" + elseIfMatcher.group(3) + "\" ]; then";
        }

        // 7.2 Preserve "else" as is
        if (trimmed.matches("^else\\s*$")) {
            return "else";
        }

        // 8. Convert "endif" to "fi"
        if (trimmed.equals("endif")) {
            return "fi";
        }

        // 9. Convert "while ( condition )" to "while [ condition ]; do"
        Pattern whilePattern = Pattern.compile("^while\\s*\\(\\s*(.+?)\\s*\\)\\s*");
        Matcher whileMatcher = whilePattern.matcher(trimmed);
        if (whileMatcher.find() && trimmed.endsWith(")")) {
            String condition = whileMatcher.group(1);
            // Best-effort translation for relational operators (e.g., <, >, ==, !=)
            condition = condition.replaceAll("\\$?(\\S+)\\s*<\\s*(\\S+)", "\"$1\" -lt \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*>\\s*(\\S+)", "\"$1\" -gt \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*==\\s*(\\S+)", "\"$1\" = \"$2\"");
            condition = condition.replaceAll("\\$?(\\S+)\\s*!=\\s*(\\S+)", "\"$1\" != \"$2\"");
            return "while [ " + condition + " ]; do";
        }

        // 10. Convert "foreach VAR (LIST)" to "for VAR in LIST; do"
        Pattern foreachPattern = Pattern.compile("^foreach\\s+(\\S+)\\s+\\((.+)\\)");
        Matcher foreachMatcher = foreachPattern.matcher(trimmed);
        if (foreachMatcher.find()) {
            return "for " + foreachMatcher.group(1) + " in " + foreachMatcher.group(2) + "; do";
        }

        // 11. Convert "switch ($var)" to "case \"$var\" in"
        Pattern switchPattern = Pattern.compile("^switch\\s*\\(\\s*\\$?(\\S+)\\s*\\)");
        Matcher switchMatcher = switchPattern.matcher(trimmed);
        if (switchMatcher.find()) {
            return "case \"" + switchMatcher.group(1) + "\" in";
        }

        // 11.1 Convert case labels and related syntax:
        //      - "case VALUE:" becomes "VALUE)"
        //      - "breaksw" becomes ";;"
        //      - "default:" becomes "*)"
        //      - "endsw" becomes "esac"
        if (trimmed.matches("^case\\s+.+:")) {
            // Example: "case ABC:" -> remove "case" and ":" to produce "ABC)"
            String label = trimmed.replaceFirst("^case\\s+", "").replace(":", "");
            return label + ")";
        }
        if (trimmed.trim().equals("breaksw")) {
            return ";;";
        }
        if (trimmed.trim().startsWith("default:")) {
            return "*)";
        }
        if (trimmed.trim().equals("endsw")) {
            return "esac";
        }

        // 12. Convert "end" to "done" to close loops (e.g., while, foreach)
        if (trimmed.equals("end")) {
            return "done";
        }

        // 13. Convert alias definitions:
        //     - "alias X Y" becomes "alias X='Y'" (adding single quotes if needed)
        //     - "unalias X" remains unchanged
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

        // 14. Convert "goto label" into a function call.
        //     If the label starts with '$', evaluate it using eval.
        if (trimmed.startsWith("goto ")) {
            String label = trimmed.substring(5).trim();
            if (label.startsWith("$")) {
                return "eval \"" + label + "\"\nreturn";
            } else {
                return label + "\nreturn";
            }
        }

        // 15. Convert a label definition "Label:" into a Bash function definition "Label() {"
        //     Close any previously open function block before starting a new one.
        if (trimmed.matches("^[A-Za-z_][A-Za-z0-9_]*:$")) {
            String label = trimmed.substring(0, trimmed.length() - 1);
            String result = "";
            if (functionBlockOpen) {
                result += "}\n\n"; // Close the previous function block
            }
            result += label + "() {";
            functionBlockOpen = true;
            return result;
        }

        // 16. Convert "source X" to ". X"
        if (trimmed.startsWith("source ")) {
            return ". " + trimmed.substring(7);
        }

        // If no conversion rules apply, return the line unchanged
        return trimmed;
    }

    /**
     * Replaces backtick command substitutions (e.g., `command`) with the modern $(command) syntax.
     *
     * @param input the original command string containing backticks
     * @return the command string with backticks replaced by $(...)
     */
    private String migrateBackticks(String input) {
        // Basic replacement of backticks with $(...)
        return input.replaceAll("`([^`]+)`", "\\$\\($1\\)");
    }

    /**
     * Detects and returns the leading whitespace (indentation) from a line.
     *
     * @param line the line from which to extract indentation
     * @return a string containing the leading whitespace (spaces or tabs)
     */
    private String getIndent(String line) {
        Pattern pattern = Pattern.compile("^(\\s*)");
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : "";
    }
}
