package org.openelisglobal.integration.ocl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { OclIntegrationTestConfig.class })
public class OclZipImporterIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(OclZipImporterIntegrationTest.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private OclZipImporter oclZipImporter;

    @Value("${org.openelisglobal.ocl.zip.path}")
    private String configuredOclZipPath;

    private static final String OCL_DIR = "src/main/resources/configurations/ocl";

    @Before
    public void setUp() {
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
        log.info("Configured OCL ZIP path: {}", configuredOclZipPath);

        // Check OCL directory
        File oclDir = new File(OCL_DIR);
        if (!oclDir.exists()) {
            log.info("Creating OCL directory: {}", OCL_DIR);
            oclDir.mkdirs();
        }

        if (oclDir.exists() && oclDir.isDirectory()) {
            log.info("Found OCL directory: {}", OCL_DIR);
            File[] files = oclDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            if (files != null && files.length > 0) {
                log.info("Found {} ZIP files in OCL directory:", files.length);
                Arrays.stream(files).forEach(file -> log.info("  - {}", file.getAbsolutePath()));
            } else {
                log.warn("No ZIP files found in OCL directory: {}", OCL_DIR);
            }
        } else {
            log.warn("OCL directory not found or is not a directory: {}", OCL_DIR);
        }

        // Check configured path
        File configuredFile = new File(configuredOclZipPath);
        if (configuredFile.exists()) {
            if (configuredFile.isDirectory()) {
                log.info("Configured path is a directory: {}", configuredOclZipPath);
                File[] files = configuredFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
                if (files != null && files.length > 0) {
                    log.info("Found {} ZIP files in configured directory:", files.length);
                    Arrays.stream(files).forEach(file -> log.info("  - {}", file.getAbsolutePath()));
                } else {
                    log.warn("No ZIP files found in configured directory");
                }
            } else {
                log.info("Configured ZIP file exists at: {}", configuredOclZipPath);
            }
        } else {
            log.warn("Configured path does not exist: {}", configuredOclZipPath);
        }
    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.json", "{\"name\": \"file2\"}" });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import two JSON files", 2, nodes.size());
        assertEquals("First JSON should have name 'file1'", "file1", nodes.get(0).get("name").asText());
        assertEquals("Second JSON should have name 'file2'", "file2", nodes.get(1).get("name").asText());
    }

    @Test
    public void testImportOclPackage_zipWithNonJson() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.txt", "This is a text file" });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import only one JSON file", 1, nodes.size());
        assertEquals("JSON file should have name 'file1'", "file1", nodes.get(0).get("name").asText());
    }

    @Test(expected = IOException.class)
    public void testImportOclPackage_nonExistentZip() throws IOException {
        oclZipImporter.importOclPackage("/non/existent/file.zip");
    }

    @Test
    public void testImportOclPackage_noJsonFiles() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.txt", "This is a text file" });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
        log.info("Parsed {} nodes", nodes.size());
        assertEquals("Should return an empty list when no JSON or CSV files are present", 0, nodes.size());
    }

    @Test
    public void testImportOclPackage_withCsvAndJson() throws IOException {
        String csvContent = "code,name,unit\nLAB123,Hemoglobin Test,g/dL\nLAB456,Glucose Test,mg/dL";
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.csv", csvContent });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import two files (JSON and CSV)", 2, nodes.size());

        // Assuming the order is file1.json then file2.csv
        JsonNode jsonNode = nodes.get(0);
        assertEquals("file1", jsonNode.get("name").asText());

        JsonNode csvNode = nodes.get(1);
        assertTrue(csvNode.isArray());
        assertEquals(2, csvNode.size());
        assertEquals("LAB123", csvNode.get(0).get("code").asText());
        assertEquals("Hemoglobin Test", csvNode.get(0).get("name").asText());
        assertEquals("g/dL", csvNode.get(0).get("unit").asText());
        assertEquals("LAB456", csvNode.get(1).get("code").asText());
        assertEquals("Glucose Test", csvNode.get(1).get("name").asText());
        assertEquals("mg/dL", csvNode.get(1).get("unit").asText());
    }

    @Test
    public void testImportOclZip() throws IOException {
        // Create a test ZIP file with some sample data
        byte[] zipData = createZipWithFiles(
                new String[] { "concepts.json", "{\"name\": \"Test Concept\", \"type\": \"Test\"}" },
                new String[] { "mappings.csv", "id,from_concept_code,to_concept_code\n1,LAB123,LOINC123" });
        String tempZipPath = createTempZipFile(zipData);

        // Test the import
        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
        log.info("Parsed {} nodes", nodes.size());
        for (JsonNode node : nodes) {
            log.info("Node content: {}", node.toPrettyString());
        }

        assertEquals("Should import two files (JSON and CSV)", 2, nodes.size());

        // Verify JSON content
        JsonNode jsonNode = nodes.get(0);
        assertEquals("Test Concept", jsonNode.get("name").asText());
        assertEquals("Test", jsonNode.get("type").asText());

        // Verify CSV content
        JsonNode csvNode = nodes.get(1);
        assertTrue(csvNode.isArray());
        assertEquals(1, csvNode.size());
        assertEquals("LAB123", csvNode.get(0).get("from_concept_code").asText());
        assertEquals("LOINC123", csvNode.get(0).get("to_concept_code").asText());
    }

    @Test
    public void testImportRealOclZip() throws IOException {
        // First try the configured path
        File configuredFile = new File(configuredOclZipPath);
        if (configuredFile.exists()) {
            if (configuredFile.isDirectory()) {
                // If it's a directory, try all ZIP files in it
                File[] zipFiles = configuredFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
                if (zipFiles != null && zipFiles.length > 0) {
                    for (File zipFile : zipFiles) {
                        try {
                            log.info("Attempting to import OCL ZIP from configured directory: {}",
                                    zipFile.getAbsolutePath());
                            List<JsonNode> nodes = oclZipImporter.importOclPackage(zipFile.getAbsolutePath());
                            log.info("Parsed {} nodes from {}:", nodes.size(), zipFile.getName());
                            for (JsonNode node : nodes) {
                                log.info("Node content: {}", node.toPrettyString());
                            }
                        } catch (Exception e) {
                            log.error("Error importing {}: {}", zipFile.getName(), e.getMessage(), e);
                        }
                    }
                } else {
                    log.warn("No ZIP files found in configured directory");
                }
            } else {
                // If it's a file, try to import it directly
                try {
                    log.info("Attempting to import OCL ZIP from configured path: {}", configuredOclZipPath);
                    List<JsonNode> nodes = oclZipImporter.importOclPackage(configuredOclZipPath);
                    log.info("Parsed {} nodes from configured ZIP file", nodes.size());
                    for (JsonNode node : nodes) {
                        log.info("Node content: {}", node.toPrettyString());
                    }
                } catch (Exception e) {
                    log.error("Error importing configured ZIP file: {}", e.getMessage(), e);
                }
            }
        } else {
            log.warn("Configured path does not exist: {}", configuredOclZipPath);
        }

        // Then try files from OCL directory
        File oclDir = new File(OCL_DIR);
        if (oclDir.exists() && oclDir.isDirectory()) {
            File[] zipFiles = oclDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            if (zipFiles != null && zipFiles.length > 0) {
                for (File zipFile : zipFiles) {
                    try {
                        log.info("Attempting to import OCL ZIP from OCL directory: {}", zipFile.getAbsolutePath());
                        List<JsonNode> nodes = oclZipImporter.importOclPackage(zipFile.getAbsolutePath());
                        log.info("Parsed {} nodes from {}:", nodes.size(), zipFile.getName());
                        for (JsonNode node : nodes) {
                            log.info("Node content: {}", node.toPrettyString());
                        }
                    } catch (Exception e) {
                        log.error("Error importing {}: {}", zipFile.getName(), e.getMessage(), e);
                    }
                }
            } else {
                log.warn("No ZIP files found in OCL directory: {}", OCL_DIR);
            }
        } else {
            log.warn("OCL directory not found: {}", OCL_DIR);
        }
    }

    private byte[] createZipWithFiles(String[]... entries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String[] entry : entries) {
                String fileName = entry[0];
                String content = entry[1];
                zos.putNextEntry(new ZipEntry(fileName));
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private String createTempZipFile(byte[] zipData) throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("test-ocl-", ".zip");
        tempFile.deleteOnExit();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(zipData);
        }
        return tempFile.getAbsolutePath();
    }
}