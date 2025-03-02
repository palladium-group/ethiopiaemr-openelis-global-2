package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OclZipImporter {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporter.class);

    @Value("${org.openelisglobal.ocl.zip.path}")
    private String oclZipPath;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CsvMapper csvMapper = new CsvMapper();

    /**
     * Imports and parses the OCL ZIP package, returning the parsed data.
     * Also logs the complete JSON output for inspection.
     * @return List of JsonNode objects representing parsed JSON/CSV files.
     * @throws IOException if the import fails.
     */
    public List<JsonNode> importOclZip() throws IOException {
        log.info("Starting import of OCL ZIP package from path: {}", oclZipPath);
        List<JsonNode> nodes = importOclPackage(oclZipPath);
        log.info("Parsed {} nodes for mapping", nodes.size());
        
        // Log the complete JSON output as in the original implementation
        int nodeIndex = 0;
        for (JsonNode node : nodes) {
            nodeIndex++;
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            log.info("Parsed JSON node #{}/{}:\n{}", nodeIndex, nodes.size(), prettyJson);
        }
        
        return nodes;
    }

    /**
     * Imports an OCL package from the specified path.
     * @param zipPath Path to the ZIP file.
     * @return List of JsonNode objects from JSON/CSV files.
     * @throws IOException if the file doesn't exist or parsing fails.
     */
    public List<JsonNode> importOclPackage(String zipPath) throws IOException {
        log.info("Importing OCL package from: {}", zipPath);
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
                    jsonNodes.add(node);
                    log.info("Successfully parsed entry: {}", entry.getName());
                } catch (RuntimeException e) {
                    log.error("Error parsing entry: {}", entry.getName(), e);
                }
            });
        }
        log.info("Imported {} files from ZIP", jsonNodes.size());
        return jsonNodes;
    }

    private JsonNode parseJsonEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            log.debug("Parsing JSON file: {}", entry.getName());
            JsonNode node = objectMapper.readTree(zipFile.getInputStream(entry));
            log.debug("JSON structure: isArray={}, fields={}", 
                node.isArray(), node.isObject() ? node.fieldNames().next() : "N/A");
            if (node.isArray() && node.size() > 0) {
                log.debug("Sample JSON entry: {}", node.get(0).toString());
            } else if (node.isObject()) {
                log.debug("Sample JSON keys: {}", node.fieldNames().next());
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON: " + entry.getName(), e);
        }
    }

    private JsonNode parseCsvEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            log.debug("Parsing CSV file: {}", entry.getName());
            CsvSchema schema = csvMapper.schemaFor(Map.class).withHeader(); // Currently we yse first row as header
            List<Object> rows = csvMapper.readerFor(Map.class)
                    .with(schema)
                    .readValues(zipFile.getInputStream(entry))
                    .readAll();
            JsonNode node = objectMapper.valueToTree(rows);
            if (!rows.isEmpty()) {
                Map<?, ?> firstRow = (Map<?, ?>) rows.get(0);
                log.debug("CSV headers: {}, row count: {}, sample: {}", 
                    firstRow.keySet(), rows.size(), firstRow);
            } else {
                log.warn("CSV file is empty: {}", entry.getName());
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV: " + entry.getName(), e);
        }
    }
}