package org.openelisglobal.integration.ocl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { OclIntegrationTestConfig.class })
public class OclZipImporterIntegrationTest {

    @Autowired
    private OclZipImporter oclZipImporter;

    @Before
    public void setUp() {
        if (oclZipImporter == null) {
            fail("OclZipImporter bean not autowired. Check Spring configuration.");
        }
    }

    @Test
    public void testImportOclPackage_validZip() throws IOException {
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.json", "{\"name\": \"file2\"}" });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
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
        assertEquals("Should return an empty list when no JSON or CSV files are present", 0, nodes.size());
    }

    @Test
    public void testImportOclPackage_withCsvAndJson() throws IOException {
        String csvContent = "code,name,unit\nLAB123,Hemoglobin Test,g/dL\nLAB456,Glucose Test,mg/dL";
        byte[] zipData = createZipWithFiles(new String[] { "file1.json", "{\"name\": \"file1\"}" },
                new String[] { "file2.csv", csvContent });
        String tempZipPath = createTempZipFile(zipData);

        List<JsonNode> nodes = oclZipImporter.importOclPackage(tempZipPath);
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
    public void testImportOclZip() {
        try {
            oclZipImporter.importOclZip();
        } catch (Exception e) {
            fail("Exception during OCL ZIP import: " + e.getMessage());
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