package joselusc.libraries.file2file.converters;

import joselusc.libraries.file2file.converters.interfaces.Converter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractConverter implements Converter {

    protected List<String> excludes = new ArrayList<>();

    @Override
    public void setExcludes(List<String> excludes) {
        if (excludes != null) {
            this.excludes = new ArrayList<>(excludes);
        }
    }

    protected boolean isExcluded(Path path) {
        if (excludes == null || excludes.isEmpty()) return false;
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) return false;
        String fileName = fileNamePath.toString();
        for (String pattern : excludes) {
            if (fileName.equals(pattern)) {
                return true;
            }
            try {
                // If it's a glob pattern like *.txt
                if (FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(path.getFileName())) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore invalid pattern syntax
            }
        }
        return false;
    }

    /**
     * Provides the target extension (including the dot, e.g., ".sh", ".java").
     */
    protected abstract String getTargetExtension();

    /**
     * Determines if a specific file should be processed by this converter.
     */
    protected abstract boolean acceptFile(Path file);

    /**
     * Converts the content of a single file.
     * By default, it processes line by line, but subclasses can override this
     * if they need to process the entire content at once.
     */
    protected String convertContent(String content) throws IOException {
        String[] lines = content.split("\n", -1);
        StringBuilder sb = new StringBuilder();
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

    /**
     * Converts a single line. Subclasses that don't override convertContent
     * should override this method.
     */
    protected String convertLine(String line) {
        return line;
    }

    @Override
    public void convert(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source path does not exist: " + source);
        }

        if (Files.isDirectory(source)) {
            if (!Files.exists(target)) {
                Files.createDirectories(target);
            } else if (!Files.isDirectory(target)) {
                throw new IOException("Target exists but is not a directory: " + target);
            }

            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isExcluded(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) throws IOException {
                    if (isExcluded(p)) {
                        return FileVisitResult.CONTINUE;
                    }
                    try {
                        if (Files.isRegularFile(p) && acceptFile(p)) {
                            Path relative = source.relativize(p);
                            Path targetFile = target.resolve(relative);

                            String ext = getTargetExtension();
                            if (ext != null && !ext.isEmpty()) {
                                String fileName = targetFile.getFileName().toString();
                                int dotIndex = fileName.lastIndexOf('.');
                                if (dotIndex > 0) {
                                    fileName = fileName.substring(0, dotIndex) + ext;
                                } else {
                                    fileName = fileName + ext;
                                }
                                targetFile = targetFile.resolveSibling(fileName);
                            }

                            Files.createDirectories(targetFile.getParent());
                            convertFile(p, targetFile);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to convert file " + p + ": " + e.getMessage());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.err.println("Failed to visit file " + file + ": " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            Path targetFile = target;
            if (Files.isDirectory(target)) {
                String ext = getTargetExtension();
                String fileName = source.getFileName().toString();
                if (ext != null && !ext.isEmpty()) {
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        fileName = fileName.substring(0, dotIndex) + ext;
                    } else {
                        fileName = fileName + ext;
                    }
                }
                targetFile = target.resolve(fileName);
            } else {
                // If target is a file path but parent doesn't exist, create it
                if (targetFile.getParent() != null && !Files.exists(targetFile.getParent())) {
                    Files.createDirectories(targetFile.getParent());
                }
            }
            convertFile(source, targetFile);
        }
    }

    protected void convertFile(Path source, Path target) throws IOException {
        Charset charset = detectCharset(source);
        // We read all bytes to handle exact content properly, especially \r\n vs \n
        String content = new String(Files.readAllBytes(source), charset);

        // Let subclass modify the content
        String converted = convertContent(content);

        Files.write(target, converted.getBytes(charset));
    }

    protected Charset detectCharset(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        try {
            // Try to decode as UTF-8 strictly
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            // Fallback to ISO-8859-1
            return StandardCharsets.ISO_8859_1;
        }
    }
}
