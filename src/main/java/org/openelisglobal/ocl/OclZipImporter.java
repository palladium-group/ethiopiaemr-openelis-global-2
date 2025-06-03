package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OclZipImporter {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporter.class);
    private static final String OCL_CONFIG_DIR = "configurations/ocl";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CsvMapper csvMapper = new CsvMapper();

    /**
     * Imports and parses the OCL ZIP package from the configurations/ocl directory.
     * 
     * @return List of JsonNode objects representing parsed JSON/CSV files.
     * @throws IOException if the import fails.
     */
    public List<JsonNode> importOclZip() throws IOException {
        Path configDir = Paths.get(OCL_CONFIG_DIR);
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
            log.info("Created OCL configuration directory at: {}", configDir.toAbsolutePath());
        }

        File[] zipFiles = configDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            throw new IOException("No OCL ZIP files found in " + configDir.toAbsolutePath());
        }

        // Use the first zip file found
        String zipPath = zipFiles[0].getAbsolutePath();
        log.info("Importing OCL ZIP package from: {}", zipPath);

        List<JsonNode> nodes = importOclPackage(zipPath);
        log.info("Successfully parsed {} nodes from OCL package", nodes.size());

        return nodes;
    }

    /**
     * Imports an OCL package from the specified path.
     * 
     * @param zipPath Path to the ZIP file.
     * @return List of JsonNode objects from JSON/CSV files.
     * @throws IOException if the file doesn't exist or parsing fails.
     */
    List<JsonNode> importOclPackage(String zipPath) throws IOException {
        File file = new File(zipPath);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("OCL package not found or invalid at: " + zipPath);
        }

        List<JsonNode> jsonNodes = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            log.debug("ZIP file details: size={} bytes, entries={}", file.length(), zipFile.size());

            zipFile.stream().forEach(entry -> {
                try {
                    log.debug("Parsing entry: {}", entry.getName());
                    if (entry.isDirectory()) {
                        log.debug("Skipping directory: {}", entry.getName());
                        return;
                    }

                    JsonNode node = null;
                    if (entry.getName().endsWith(".json")) {
                        node = parseJsonEntry(zipFile, entry);
                    } else if (entry.getName().endsWith(".csv")) {
                        node = parseCsvEntry(zipFile, entry);
                    } else {
                        log.warn("Skipping unsupported file: {}", entry.getName());
                        return;
                    }

                    if (node != null) {
                        jsonNodes.add(node);
                        log.info("Successfully parsed entry: {}", entry.getName());
                        // Log sample of the parsed data
                        if (node.isArray() && node.size() > 0) {
                            log.debug("Sample entry from {}: {}", entry.getName(), node.get(0).toString());
                        }
                    }
                } catch (RuntimeException e) {
                    log.error("Error parsing entry: {}", entry.getName(), e);
                }
            });
        }

        return jsonNodes;
    }

    private JsonNode parseJsonEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            log.debug("Parsing JSON file: {}", entry.getName());
            JsonNode node = objectMapper.readTree(zipFile.getInputStream(entry));
            log.debug("JSON structure: isArray={}, fields={}", node.isArray(),
                    node.isObject() ? node.fieldNames().next() : "N/A");
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + entry.getName(), e);
        }
    }

    private JsonNode parseCsvEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            log.debug("Parsing CSV file: {}", entry.getName());
            CsvSchema schema = csvMapper.schemaFor(Map.class).withHeader();
            List<Object> rows = csvMapper.readerFor(Map.class).with(schema).readValues(zipFile.getInputStream(entry))
                    .readAll();

            JsonNode node = objectMapper.valueToTree(rows);
            if (!rows.isEmpty()) {
                Map<?, ?> firstRow = (Map<?, ?>) rows.get(0);
                log.debug("CSV headers: {}, row count: {}", firstRow.keySet(), rows.size());
            } else {
                log.warn("CSV file is empty: {}", entry.getName());
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV: " + entry.getName(), e);
        }
    }
}