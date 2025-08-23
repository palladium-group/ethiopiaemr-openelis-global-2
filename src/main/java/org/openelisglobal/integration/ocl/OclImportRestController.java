package org.openelisglobal.integration.ocl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/rest/ocl")
public class OclImportRestController {

    private static final Logger log = LoggerFactory.getLogger(OclImportRestController.class);

    @Value("${org.openelisglobal.ocl.import.directory:src/main/resources/configurations/ocl}")
    private String oclImportDirectory;

    @Autowired
    private OclImportInitializer oclImportInitializer;

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importOclFile(@RequestParam("oclFile") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".zip")) {
                response.put("success", false);
                response.put("message", "Only ZIP files are supported");
                return ResponseEntity.badRequest().body(response);
            }

            // Create import directory if it doesn't exist
            Path importDir = Paths.get(oclImportDirectory);
            if (!Files.exists(importDir)) {
                Files.createDirectories(importDir);
            }

            // Save uploaded file to import directory
            String filename = file.getOriginalFilename();
            File destinationFile = new File(importDir.toFile(), filename);

            // Save the file
            try (FileOutputStream fos = new FileOutputStream(destinationFile)) {
                fos.write(file.getBytes());
            }

            log.info("OCL Import: Uploaded file {} saved to {}", filename, destinationFile.getAbsolutePath());

            // Reset execution flag to allow re-import
            OclImportInitializer.resetExecutionFlag();

            // Start async import process immediately (fire and forget)
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("OCL Import: Starting background import process for file {}", filename);
                    oclImportInitializer.performOclImport();
                    log.info("OCL Import: Successfully completed background processing for file {}", filename);
                } catch (Exception e) {
                    log.error("OCL Import: Failed to process file " + filename + " in background", e);
                }
            });

            // Return immediate response
            response.put("success", true);
            response.put("message", "OCL file uploaded successfully. Processing started in background.");
            response.put("filename", filename);
            response.put("status", "uploaded");

            log.info("OCL Import: File {} uploaded and processing started in background", filename);

        } catch (IOException e) {
            log.error("OCL Import: Failed to save uploaded file", e);
            response.put("success", false);
            response.put("message", "Failed to save uploaded file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("OCL Import: Unexpected error during file upload", e);
            response.put("success", false);
            response.put("message", "Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @Async
    public CompletableFuture<Void> processOclImportAsync(String filename) {
        try {
            log.info("OCL Import: Starting async import process for file {}", filename);

            // Call the import method directly instead of the event handler
            oclImportInitializer.performOclImport();

            log.info("OCL Import: Successfully completed async processing for file {}", filename);

        } catch (Exception e) {
            log.error("OCL Import: Failed to process file " + filename + " in async mode", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetImportFlag() {
        Map<String, Object> response = new HashMap<>();

        try {
            OclImportInitializer.resetExecutionFlag();
            response.put("success", true);
            response.put("message", "Import flag reset successfully");
            log.info("OCL Import: Execution flag reset via REST endpoint");
        } catch (Exception e) {
            log.error("OCL Import: Failed to reset execution flag", e);
            response.put("success", false);
            response.put("message", "Failed to reset import flag: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reimport")
    public ResponseEntity<Map<String, Object>> reimportOclFile() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Reset the execution flag first
            OclImportInitializer.resetExecutionFlag();

            // Start async processing in a separate thread immediately
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("OCL Re-import: Starting background re-import process");
                    oclImportInitializer.performOclImport();
                    log.info("OCL Re-import: Background re-import completed successfully");
                } catch (Exception e) {
                    log.error("OCL Re-import: Background re-import failed", e);
                }
            });

            response.put("success", true);
            response.put("message", "OCL re-import started successfully. Processing in background...");
            log.info("OCL Re-import: Immediate response sent, processing continues in background");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("OCL Re-import: Failed to start re-import", e);
            response.put("success", false);
            response.put("message", "Failed to start re-import: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
