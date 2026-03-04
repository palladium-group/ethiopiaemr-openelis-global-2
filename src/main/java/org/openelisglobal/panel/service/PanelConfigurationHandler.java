package org.openelisglobal.panel.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handler for loading panel configuration files. Supports CSV format for
 * defining laboratory panels with their sample types and localization.
 *
 * Expected CSV format:
 * panelName,sampleType,description,englishName,frenchName,isActive,sortOrder
 * Complete Blood Count,Whole Blood,CBC panel with all parameters,Complete Blood
 * Count,Formule Sanguine Complète,Y,1
 *
 * Notes: - First line is the header (required) - panelName is required field -
 * sampleType is optional but recommended (Can specify multiple separated by |)
 * - description, englishName, frenchName are optional - isActive defaults to
 * "Y" if not specified - sortOrder is optional (auto-assigned if not provided)
 * - Existing panels with matching name will be updated
 */
@Component
public class PanelConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private PanelService panelService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private TypeOfSamplePanelService typeOfSamplePanelService;

    @Autowired
    private LocalizationService localizationService;

    @Override
    public String getDomainName() {
        return "panels";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 150; // Load after test sections and sample types, before tests
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Read and validate header
        String headerLine = reader.readLine();
        if (headerLine == null) {
            throw new IllegalArgumentException("Panel configuration file " + fileName + " is empty");
        }

        String[] headers = parseCsvLine(headerLine);
        validateHeaders(headers, fileName);

        // Get column indices
        int panelNameIndex = findColumnIndex(headers, "panelName");
        int sampleTypeIndex = findColumnIndex(headers, "sampleType");
        int descriptionIndex = findColumnIndex(headers, "description");
        int englishNameIndex = findColumnIndex(headers, "englishName");
        int frenchNameIndex = findColumnIndex(headers, "frenchName");
        int isActiveIndex = findColumnIndex(headers, "isActive");
        int sortOrderIndex = findColumnIndex(headers, "sortOrder");

        List<Panel> processedPanels = new ArrayList<>();
        String line;
        int lineNumber = 1;
        int nextSortOrder = 1;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }

            try {
                String[] values = parseCsvLine(line);
                Panel panel = processCsvLine(values, panelNameIndex, sampleTypeIndex, descriptionIndex,
                        englishNameIndex, frenchNameIndex, isActiveIndex, sortOrderIndex, lineNumber, fileName,
                        nextSortOrder);
                if (panel != null) {
                    processedPanels.add(panel);
                    nextSortOrder++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error processing line " + lineNumber + " in file " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Successfully loaded " + processedPanels.size() + " panels from " + fileName);
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

        for (String header : headers) {
            if ("panelName".equalsIgnoreCase(header)) {
                hasPanelNameColumn = true;
            }
        }

        if (!hasPanelNameColumn) {
            throw new IllegalArgumentException(
                    "Panel configuration file " + fileName + " must have a 'panelName' column");
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

    private Panel processCsvLine(String[] values, int panelNameIndex, int sampleTypeIndex, int descriptionIndex,
            int englishNameIndex, int frenchNameIndex, int isActiveIndex, int sortOrderIndex, int lineNumber,
            String fileName, int defaultSortOrder) {

        String panelName = getValueOrEmpty(values, panelNameIndex);

        if (panelName.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processCsvLine",
                    "Skipping row " + lineNumber + " in " + fileName + " with missing panelName");
            return null;
        }

        // Check if panel already exists
        Panel existingPanel = panelService.getPanelByName(panelName);

        Panel panel;
        if (existingPanel != null) {
            panel = updatePanel(existingPanel, values, panelName, sampleTypeIndex, descriptionIndex, englishNameIndex,
                    frenchNameIndex, isActiveIndex, sortOrderIndex, defaultSortOrder);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine", "Updated existing panel: " + panelName);
        } else {
            panel = createPanel(values, panelName, sampleTypeIndex, descriptionIndex, englishNameIndex, frenchNameIndex,
                    isActiveIndex, sortOrderIndex, defaultSortOrder);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processCsvLine", "Created new panel: " + panelName);
        }

        // Handle sample type mappings
        String sampleTypes = getValueOrEmpty(values, sampleTypeIndex);
        if (!sampleTypes.isEmpty() && panel != null) {
            createSampleTypeMappings(panel, sampleTypes, lineNumber, fileName);
        }

        return panel;
    }

    private String getValueOrEmpty(String[] values, int index) {
        if (index >= 0 && index < values.length) {
            String value = values[index];
            return value != null ? value : "";
        }
        return "";
    }

    private Panel updatePanel(Panel panel, String[] values, String panelName, int sampleTypeIndex, int descriptionIndex,
            int englishNameIndex, int frenchNameIndex, int isActiveIndex, int sortOrderIndex, int defaultSortOrder) {

        panel.setPanelName(panelName);

        String description = getValueOrEmpty(values, descriptionIndex);
        if (!description.isEmpty()) {
            panel.setDescription(description);
        }

        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            panel.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        }

        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                panel.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "updatePanel",
                        "Invalid sortOrder '" + sortOrderStr + "', using default");
                panel.setSortOrderInt(defaultSortOrder);
            }
        }

        panel.setSysUserId("1");
        panelService.update(panel);
        return panel;
    }

    private Panel createPanel(String[] values, String panelName, int sampleTypeIndex, int descriptionIndex,
            int englishNameIndex, int frenchNameIndex, int isActiveIndex, int sortOrderIndex, int defaultSortOrder) {

        Panel panel = new Panel();
        panel.setPanelName(panelName);

        // Create localization for panel name
        String englishName = getValueOrEmpty(values, englishNameIndex);
        String frenchName = getValueOrEmpty(values, frenchNameIndex);

        if (englishName.isEmpty()) {
            englishName = panelName;
        }
        if (frenchName.isEmpty()) {
            frenchName = englishName;
        }

        Localization localization = new Localization();
        localization.setDescription("panel name");
        localization.setEnglish(englishName);
        localization.setFrench(frenchName);
        localization.setSysUserId("1");
        localizationService.insert(localization);
        panel.setLocalization(localization);

        String description = getValueOrEmpty(values, descriptionIndex);
        if (!description.isEmpty()) {
            panel.setDescription(description);
        }

        String isActive = getValueOrEmpty(values, isActiveIndex);
        if (!isActive.isEmpty()) {
            panel.setIsActive("Y".equalsIgnoreCase(isActive) || "true".equalsIgnoreCase(isActive) ? "Y" : "N");
        } else {
            panel.setIsActive("Y");
        }

        // Set sort order
        String sortOrderStr = getValueOrEmpty(values, sortOrderIndex);
        if (!sortOrderStr.isEmpty()) {
            try {
                panel.setSortOrderInt(Integer.parseInt(sortOrderStr));
            } catch (NumberFormatException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createPanel",
                        "Invalid sortOrder '" + sortOrderStr + "', using default");
                panel.setSortOrderInt(defaultSortOrder);
            }
        } else {
            panel.setSortOrderInt(defaultSortOrder);
        }

        panel.setSysUserId("1");
        String panelId = panelService.insert(panel);
        panel.setId(panelId);
        return panel;
    }

    private void createSampleTypeMappings(Panel panel, String sampleTypes, int lineNumber, String fileName) {
        // Sample types can be separated by |
        String[] sampleTypeNames = sampleTypes.split("\\|");

        for (String sampleTypeName : sampleTypeNames) {
            sampleTypeName = sampleTypeName.trim();
            if (sampleTypeName.isEmpty()) {
                continue;
            }

            TypeOfSample sampleType = findSampleType(sampleTypeName);
            if (sampleType == null) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createSampleTypeMappings", "Sample type '"
                        + sampleTypeName + "' not found for panel in line " + lineNumber + " of " + fileName);
                continue;
            }

            // Check if mapping already exists
            if (!panelSampleTypeMappingExists(panel.getId(), sampleType.getId())) {
                TypeOfSamplePanel mapping = new TypeOfSamplePanel();
                mapping.setPanelId(panel.getId());
                mapping.setTypeOfSampleId(sampleType.getId());
                mapping.setSysUserId("1");
                typeOfSamplePanelService.insert(mapping);
                LogEvent.logDebug(this.getClass().getSimpleName(), "createSampleTypeMappings",
                        "Created mapping: panel '" + panel.getPanelName() + "' -> sample type '" + sampleTypeName
                                + "'");
            }
        }
    }

    private TypeOfSample findSampleType(String sampleTypeName) {
        List<TypeOfSample> allSampleTypes = typeOfSampleService.getAllTypeOfSamples();

        for (TypeOfSample sampleType : allSampleTypes) {
            if (sampleType.getLocalizedName() != null
                    && sampleType.getLocalizedName().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            if (sampleType.getDescription() != null && sampleType.getDescription().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
            if (sampleType.getLocalAbbreviation() != null
                    && sampleType.getLocalAbbreviation().equalsIgnoreCase(sampleTypeName)) {
                return sampleType;
            }
        }
        return null;
    }

    private boolean panelSampleTypeMappingExists(String panelId, String sampleTypeId) {
        List<TypeOfSamplePanel> existingMappings = typeOfSamplePanelService.getTypeOfSamplePanelsForPanel(panelId);
        for (TypeOfSamplePanel mapping : existingMappings) {
            if (mapping.getTypeOfSampleId().equals(sampleTypeId)) {
                return true;
            }
        }
        return false;
    }
}
