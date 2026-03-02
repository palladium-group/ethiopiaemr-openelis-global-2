package org.openelisglobal.panelitem.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading panel-test mapping configuration files. Supports CSV format for
 * defining which tests belong to which panels.
 *
 * Expected CSV format:
 * panelName,testName,sortOrder
 * Complete Blood Count,Nucleated Red Blood Cell Count,1
 * Complete Blood Count,Hemoglobin,2
 * Chemistry Panel,Glucose,1
 *
 * Notes: - First line is the header (required)
 * - panelName and testName are required fields
 * - sortOrder is optional (auto-assigned if not provided)
 * - Existing mappings will be updated
 */
@Component
public class PanelTestConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private PanelService panelService;

    @Autowired
    private TestService testService;

    @Autowired
    private PanelItemService panelItemService;

    @Override
    public String getDomainName() {
        return "panel-tests";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 250; // Load after panels and tests
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Panel-test configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int panelNameIndex = findColumnIndex(headers, "panelName");
        int testNameIndex = findColumnIndex(headers, "testName");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");

        List<PanelItem> processedMappings = new ArrayList<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            // Skip empty lines and comments (lines starting with #)
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                PanelItem panelItem = processCsvLine(values, panelNameIndex, testNameIndex, sortOrderIndex,
                        lineNumber, fileName, nextSortOrder);
                if (panelItem != null) {
                    processedMappings.add(panelItem);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedMappings.size() + " panel-test mappings from " + fileName);
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers, String fileName) {
        boolean hasPanelNameColumn = false;
        boolean hasTestNameColumn = false;

        for (String header : headers) {
            if ("panelName".equalsIgnoreCase(header)) {
                hasPanelNameColumn = true;
            }
            if ("testName".equalsIgnoreCase(header)) {
                hasTestNameColumn = true;
            }
        }

        if (!hasPanelNameColumn) {
            throw new IllegalArgumentException(
                    "Panel-test configuration file " + fileName + " must have a 'panelName' column");
        }
        if (!hasTestNameColumn) {
            throw new IllegalArgumentException(
                    "Panel-test configuration file " + fileName + " must have a 'testName' column");
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (columnName.equalsIgnoreCase(headers[i])) {
                return i;
            }
        }
        return -1;
    }

    private PanelItem processCsvLine(String[] values, int panelNameIndex, int testNameIndex,
            int sortOrderIndex, int lineNumber, String fileName, int defaultSortOrder) {

        String panelName = getValueOrEmpty(values, panelNameIndex);
        String testName = getValueOrEmpty(values, testNameIndex);

        if (panelName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing panelName");
            return null;
        }

        if (testName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing testName");
            return null;
        }

        // Find panel
        Panel panel = panelService.getPanelByName(panelName);
        if (panel == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Panel '" + panelName
                    + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        // Find test
        Test test = testService.getTestByDescription(testName);
        if (test == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine", "Test '" + testName
                    + "' not found in line " + lineNumber + " of " + fileName + ". Skipping.");
            return null;
        }

        // Check if mapping already exists
        if (mappingExists(panel.getId(), test.getId())) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "processCsvLine",
                    "Mapping already exists: panel '" + panelName + "' -> test '" + testName + "'");
            return null;
        }

        // Create panel-test mapping
        PanelItem panelItem = new PanelItem();
        panelItem.setPanel(panel);
        panelItem.setTest(test);

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            panelItem.setSortOrder(sortOrderStr);
        } else {
            panelItem.setSortOrder(String.valueOf(defaultSortOrder));
        }

        panelItem.setSysUserId("1");
        String panelItemId = panelItemService.insert(panelItem);
        panelItem.setId(panelItemId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine",
                "Created mapping: panel '" + panelName + "' -> test '" + testName + "'");

        return panelItem;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private boolean mappingExists(String panelId, String testId) {
        List<PanelItem> existingMappings = panelItemService.getPanelItemsForPanel(panelId);
        for (PanelItem mapping : existingMappings) {
            if (mapping.getTest().getId().equals(testId)) {
                return true;
            }
        }
        return false;
    }
}
