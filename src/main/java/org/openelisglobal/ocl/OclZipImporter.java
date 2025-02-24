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
    private final CsvMapper csvMapper = new CsvMapper(); // Added for CSV parsing

    public void importOclZip() {
        log.info("Starting import of OCL ZIP package from path: {}", oclZipPath);
        try {
            List<JsonNode> nodes = importOclPackage(oclZipPath);
            for (JsonNode node : nodes) {
                String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
                log.info("Parsed JSON:\n{}", prettyJson);
            }
        } catch (IOException e) {
            log.error("Failed to import OCL ZIP package", e);
        }
    }

    public List<JsonNode> importOclPackage(String zipPath) throws IOException {
        log.info("Importing OCL package from: {}", zipPath);
        File file = new File(zipPath);
        if (!file.exists()) {
            throw new IOException("OCL package not found at: " + zipPath);
        }

        List<JsonNode> jsonNodes = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            zipFile.stream()
                    .filter(entry -> !entry.isDirectory()
                            && (entry.getName().endsWith(".json") || entry.getName().endsWith(".csv")))
                    .forEach(entry -> {
                        try {
                            JsonNode node;
                            if (entry.getName().endsWith(".json")) {
                                node = parseJsonEntry(zipFile, entry);
                            } else if (entry.getName().endsWith(".csv")) {
                                node = parseCsvEntry(zipFile, entry); // New CSV parsing method
                            } else {
                                return;
                            }
                            jsonNodes.add(node);
                            log.info("Processed entry: {}", entry.getName());
                        } catch (RuntimeException e) {
                            log.error("Error processing entry: {}", entry.getName(), e);
                        }
                    });
        }
        log.info("Imported {} files (JSON or CSV)", jsonNodes.size());
        return jsonNodes;
    }

    private JsonNode parseJsonEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            return objectMapper.readTree(zipFile.getInputStream(entry));
        } catch (IOException e) {
            log.error("Failed to parse JSON file: {}", entry.getName(), e);
            throw new RuntimeException("Failed to parse JSON: " + entry.getName(), e);
        }
    }

    // The comments are for future reference if any modification is needed
    private JsonNode parseCsvEntry(ZipFile zipFile, ZipEntry entry) {
        try {
            // Assuming CSV has headers; adjust schema if needed
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            List<Object> rows = csvMapper.readerFor(Map.class).with(schema).readValues(zipFile.getInputStream(entry))
                    .readAll();
            return objectMapper.valueToTree(rows);
        } catch (IOException e) {
            log.error("Failed to parse CSV file: {}", entry.getName(), e);
            throw new RuntimeException("Failed to parse CSV: " + entry.getName(), e);
        }
    }
}