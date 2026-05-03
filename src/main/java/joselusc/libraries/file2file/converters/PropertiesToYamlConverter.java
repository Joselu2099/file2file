package joselusc.libraries.file2file.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Converts .properties files to nested .yml files.
 */
public class PropertiesToYamlConverter extends AbstractConverter {

    @Override
    protected String getTargetExtension() {
        return ".yml";
    }

    @Override
    protected boolean acceptFile(Path file) {
        return file.getFileName().toString().endsWith(".properties");
    }

    @Override
    protected String convertContent(String content) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(content));

        Map<String, Object> rootMap = new LinkedHashMap<>();

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String[] parts = key.split("\\.");
            Map<String, Object> currentMap = rootMap;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (part.isEmpty()) continue;

                Object existing = currentMap.get(part);

                if (existing instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> existingMap = (Map<String, Object>) existing;
                    currentMap = existingMap;
                } else {
                    Map<String, Object> newMap = new LinkedHashMap<>();
                    currentMap.put(part, newMap);
                    currentMap = newMap;
                }
            }

            String lastPart = parts[parts.length - 1];
            if (!lastPart.isEmpty()) {
                currentMap.put(lastPart, value);
            }
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.writeValueAsString(rootMap);
    }
}
