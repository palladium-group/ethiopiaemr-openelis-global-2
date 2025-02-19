package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OclZipImporter {

    private static final Logger log = LoggerFactory.getLogger(OclZipImporter.class);
    // To Do: Change the default value to the actual path of the OCL ZIP package
    // reference this from the properties file
    @Value("${ocl.zip.path:src/main/java/org/openelisglobal/ocl/test(1).zip}")
    private String oclZipPath;

    public void importOclZip() {
        log.info("Starting import of OCL ZIP package from path: {}", oclZipPath);
        try (ZipFile zipFile = new ZipFile(oclZipPath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            ObjectMapper objectMapper = new ObjectMapper();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".json")) {
                    log.info("Processing entry: {}", entry.getName());
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        JsonNode jsonNode = objectMapper.readTree(is);
                        // Pretty print the JSON output for better readability
                        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                        log.info("Parsed JSON from {}:\n{}", entry.getName(), prettyJson);
                    } catch (Exception e) {
                        log.error("Error parsing JSON entry: {}", entry.getName(), e);
                    }
                } else {
                    log.info("Skipping non-JSON or directory entry: {}", entry.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to import OCL ZIP package", e);
        }
    }
}
