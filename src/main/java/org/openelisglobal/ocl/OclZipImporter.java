package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
            zipFile.stream().filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".json"))
                    .forEach(entry -> {
                        try {
                            JsonNode node = parseJsonEntry(zipFile, entry);
                            jsonNodes.add(node);
                            log.info("Processed entry: {}", entry.getName());
                        } catch (RuntimeException e) {
                            log.error("Error processing entry: {}", entry.getName(), e);
                        }
                    });
        }
        log.info("Imported {} JSON files", jsonNodes.size());
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
}