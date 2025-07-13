package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.valueholder.TypeOfTestResult;

public class OclToOpenElisMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Map OCL test sections to valid OpenELIS test section IDs
    private static final Map<String, String> TEST_SECTION_MAPPING = new HashMap<>();
    static {
        TEST_SECTION_MAPPING.put("HEMATOLOGY", "36");
        TEST_SECTION_MAPPING.put("BIOCHEMISTRY", "56");
        TEST_SECTION_MAPPING.put("CHEMISTRY", "56");
        TEST_SECTION_MAPPING.put("IMMUNOLOGY", "59");
        TEST_SECTION_MAPPING.put("VIROLOGIE", "76");
        TEST_SECTION_MAPPING.put("VIROLOGY", "76");
        TEST_SECTION_MAPPING.put("SEROLOGY", "97");
        TEST_SECTION_MAPPING.put("SEROLOGY-IMMUNOLOGY", "117");
        TEST_SECTION_MAPPING.put("MOLECULAR BIOLOGY", "136");
        TEST_SECTION_MAPPING.put("MOLECULAR_BIOLOGY", "136");
        TEST_SECTION_MAPPING.put("PATHOLOGY", "163");
        TEST_SECTION_MAPPING.put("IMMUNOHISTOCHEMISTRY", "164");
        TEST_SECTION_MAPPING.put("CYTOLOGY", "165");
        TEST_SECTION_MAPPING.put("MICROBIOLOGY", "163");
        TEST_SECTION_MAPPING.put("PARASITOLOGY", "163");
        TEST_SECTION_MAPPING.put("PANEL", "36");
        TEST_SECTION_MAPPING.put("TEST", "36");
    }

    // Map OCL data types to OpenELIS result type IDs
    private static final Map<String, String> RESULT_TYPE_MAPPING = new HashMap<>();
    static {
        RESULT_TYPE_MAPPING.put("NUMERIC", "N");
        RESULT_TYPE_MAPPING.put("NUMBER", "N");
        RESULT_TYPE_MAPPING.put("CODED", "D");
        RESULT_TYPE_MAPPING.put("SELECT", "D");
        RESULT_TYPE_MAPPING.put("BOOLEAN", "D");
        RESULT_TYPE_MAPPING.put("TEXT", "T");
        RESULT_TYPE_MAPPING.put("STRING", "T");
        RESULT_TYPE_MAPPING.put("PANEL", "P");
    }

    /**
     * Creates a wrapper node with concepts array for a single concept
     */
    public JsonNode createConceptWrapper(JsonNode singleConcept) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        ArrayNode conceptsArray = objectMapper.createArrayNode();
        conceptsArray.add(singleConcept);
        wrapper.set("concepts", conceptsArray);
        return wrapper;
    }

    /**
     * Maps OCL concepts to TestAddForm objects ready for submission
     */
    public List<TestAddForm> mapConceptsToTestAddForms(JsonNode rootNode) {
        try {
            List<TestAddForm> forms = new ArrayList<>();

            // Handle array of concepts
            if (rootNode.has("concepts") && rootNode.get("concepts").isArray()) {
                for (JsonNode conceptNode : rootNode.get("concepts")) {
                    TestAddForm form = mapSingleConceptToForm(conceptNode);
                    if (form != null) {
                        forms.add(form);
                    }
                }
            } else {
                // Handle single concept
                TestAddForm form = mapSingleConceptToForm(rootNode);
                if (form != null) {
                    forms.add(form);
                }
            }

            return forms;
        } catch (Exception e) {
            System.err.println("Error mapping OCL concepts to TestAddForm: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Maps a single OCL concept to a TestAddForm
     */
    private TestAddForm mapSingleConceptToForm(JsonNode concept) {
        try {
            TestAddForm form = new TestAddForm();

            // Create the JSON structure expected by TestAddRestController
            ObjectNode jsonWad = objectMapper.createObjectNode();

            // Map all required fields in the exact format
            mapTestNames(concept, jsonWad);
            mapTestSection(concept, jsonWad);
            mapPanels(concept, jsonWad);
            mapUnits(concept, jsonWad);
            mapLoinc(concept, jsonWad);
            mapResultType(concept, jsonWad);
            mapOrderableFlags(concept, jsonWad);
            mapSampleTypes(concept, jsonWad);
            mapNumericValidation(concept, jsonWad);
            mapResultLimits(concept, jsonWad);
            mapDictionaryResults(concept, jsonWad);

            // Set default values for any missing fields
            setDefaultValues(jsonWad);

            // Convert to JSON string and set in form
            form.setJsonWad(objectMapper.writeValueAsString(jsonWad));

            // Also set LOINC separately as it's used for validation
            form.setLoinc(getText(concept, "loinc"));

            System.out.println("Mapped OCL concept to TestAddForm: " + getText(concept, "id"));

            return form;
        } catch (Exception e) {
            System.err.println("Error mapping single OCL concept: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void mapTestNames(JsonNode concept, ObjectNode jsonWad) {
        String englishName = null;
        String frenchName = null;

        // Try OCL standard format with names array first
        JsonNode names = concept.get("names");
        if (names != null && names.isArray()) {
            for (JsonNode nameNode : names) {
                String locale = getText(nameNode, "locale");
                String name = getText(nameNode, "name");
                if ("en".equals(locale) && name != null) {
                    englishName = name;
                } else if ("fr".equals(locale) && name != null) {
                    frenchName = name;
                }
            }
        }

        // Fallback to simplified format fields
        if (englishName == null) {
            englishName = getText(concept, "display_name");
            if (englishName == null) {
                englishName = getText(concept, "description");
            }
            if (englishName == null) {
                englishName = getText(concept, "id");
            }
        }

        // Ensure French name has fallback
        if (frenchName == null) {
            frenchName = englishName;
        }

        jsonWad.put("testNameEnglish", englishName != null ? englishName : "");
        jsonWad.put("testNameFrench", frenchName != null ? frenchName : "");
        jsonWad.put("testReportNameEnglish", englishName != null ? englishName : "");
        jsonWad.put("testReportNameFrench", frenchName != null ? frenchName : "");
    }

    private void mapTestSection(JsonNode concept, ObjectNode jsonWad) {
        String testSection = getText(concept, "testSection");
        String conceptClass = getText(concept, "concept_class");
        String sectionId = null;

        // Try to map based on testSection field first
        if (testSection != null) {
            sectionId = TEST_SECTION_MAPPING.get(testSection.toUpperCase());
        }

        // Try to map based on concept_class
        if (sectionId == null && conceptClass != null) {
            sectionId = TEST_SECTION_MAPPING.get(conceptClass.toUpperCase());
        }

        // Try to infer from test ID
        if (sectionId == null) {
            String testId = getText(concept, "id");
            if (testId != null) {
                sectionId = inferSectionFromTestId(testId);
            }
        }

        // Default to Hematology
        if (sectionId == null) {
            sectionId = "36";
        }

        jsonWad.put("testSection", sectionId);
    }

    private void mapPanels(JsonNode concept, ObjectNode jsonWad) {
        // Initialize as empty array - can be enhanced to map OCL panel relationships
        ArrayNode panelsArray = objectMapper.createArrayNode();
        jsonWad.set("panels", panelsArray);
    }

    private void mapUnits(JsonNode concept, ObjectNode jsonWad) {
        String units = null;

        // Try extras first (OCL standard)
        JsonNode extras = concept.get("extras");
        if (extras != null && extras.has("units")) {
            units = getText(extras, "units");
        }

        // Try direct units field
        if (units == null) {
            units = getText(concept, "units");
        }

        jsonWad.put("uom", units != null ? units : "");
    }

    private void mapLoinc(JsonNode concept, ObjectNode jsonWad) {
        String loinc = null;

        // Try mappings array first (OCL standard format)
        JsonNode mappings = concept.get("mappings");
        if (mappings != null && mappings.isArray()) {
            for (JsonNode mapping : mappings) {
                String mapType = getText(mapping, "map_type");
                if ("SAME-AS".equals(mapType) || "LOINC".equals(mapType)) {
                    loinc = getText(mapping, "to_concept_code");
                    if (loinc != null)
                        break;
                }
            }
        }

        // Fallback to direct loinc field
        if (loinc == null) {
            loinc = getText(concept, "loinc");
        }

        jsonWad.put("loinc", loinc != null ? loinc : "");
    }

    private void mapResultType(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        String resultTypeId = "T"; // Default to text

        if (dataType != null) {
            String mappedType = RESULT_TYPE_MAPPING.get(dataType.toUpperCase());
            if (mappedType != null) {
                resultTypeId = mappedType;
            }
        }

        // Convert to actual DB ID
        try {
            TypeOfTestResultService service = SpringContext.getBean(TypeOfTestResultService.class);
            TypeOfTestResult typeObj = service.getTypeOfTestResultByType(resultTypeId);
            if (typeObj != null && typeObj.getId() != null) {
                jsonWad.put("resultType", typeObj.getId());
            } else {
                jsonWad.put("resultType", resultTypeId);
            }
        } catch (Exception e) {
            System.err.println("Error mapping result type: " + e.getMessage());
            jsonWad.put("resultType", resultTypeId);
        }
    }

    private void mapOrderableFlags(JsonNode concept, ObjectNode jsonWad) {
        // Map orderable flag - default to Y
        String orderable = getText(concept, "orderable");
        jsonWad.put("orderable", orderable != null ? orderable : "Y");

        // Map notifyResults flag
        String notifyResults = getText(concept, "notifyResults");
        jsonWad.put("notifyResults", notifyResults != null ? notifyResults : "");

        // Map inLabOnly flag
        String inLabOnly = getText(concept, "inLabOnly");
        jsonWad.put("inLabOnly", inLabOnly != null ? inLabOnly : "");

        // Map antimicrobialResistance flag
        String antimicrobialResistance = getText(concept, "antimicrobialResistance");
        jsonWad.put("antimicrobialResistance", antimicrobialResistance != null ? antimicrobialResistance : "");

        // Map active flag - default to Y
        String active = getText(concept, "active");
        jsonWad.put("active", active != null ? active : "Y");
    }

    private void mapSampleTypes(JsonNode concept, ObjectNode jsonWad) {
        ArrayNode sampleTypesArray = objectMapper.createArrayNode();

        // Check if OCL concept specifies sample types
        JsonNode sampleTypes = concept.get("sampleTypes");
        if (sampleTypes != null && sampleTypes.isArray()) {
            for (JsonNode sampleType : sampleTypes) {
                String typeId = getText(sampleType, "typeId");
                if (typeId != null) {
                    ObjectNode sampleTypeObj = objectMapper.createObjectNode();
                    sampleTypeObj.put("typeId", typeId);

                    ArrayNode testsArray = objectMapper.createArrayNode();
                    ObjectNode testOrder = objectMapper.createObjectNode();
                    testOrder.put("id", 0); // New test placeholder
                    testsArray.add(testOrder);

                    sampleTypeObj.set("tests", testsArray);
                    sampleTypesArray.add(sampleTypeObj);
                }
            }
        }

        // Default to Serum (ID: 31) if no specific sample type is defined
        if (sampleTypesArray.size() == 0) {
            ObjectNode sampleType = objectMapper.createObjectNode();
            sampleType.put("typeId", "31"); // Serum

            ArrayNode testsArray = objectMapper.createArrayNode();
            ObjectNode testOrder = objectMapper.createObjectNode();
            testOrder.put("id", 0); // New test placeholder
            testsArray.add(testOrder);

            sampleType.set("tests", testsArray);
            sampleTypesArray.add(sampleType);
        }

        jsonWad.set("sampleTypes", sampleTypesArray);
    }

    private void mapNumericValidation(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        boolean isNumeric = dataType != null
                && (dataType.toLowerCase().contains("numeric") || dataType.toLowerCase().contains("number"));

        String lowValid = "";
        String highValid = "";
        String lowReporting = "";
        String highReporting = "";
        String lowCritical = "";
        String highCritical = "";
        String sigDigits = "";

        if (isNumeric) {
            // Map validation ranges
            JsonNode extras = concept.get("extras");

            if (extras != null) {
                String lowAbs = getNumericText(extras, "low_absolute");
                if (lowAbs != null)
                    lowValid = lowAbs;

                String hiNormal = getNumericText(extras, "hi_normal");
                if (hiNormal != null)
                    highValid = hiNormal;

                String sigDigitsValue = getNumericText(extras, "significant_digits");
                if (sigDigitsValue != null)
                    sigDigits = sigDigitsValue;

                String lowReportingValue = getNumericText(extras, "low_reporting");
                if (lowReportingValue != null)
                    lowReporting = lowReportingValue;

                String highReportingValue = getNumericText(extras, "high_reporting");
                if (highReportingValue != null)
                    highReporting = highReportingValue;

                String lowCriticalValue = getNumericText(extras, "low_critical");
                if (lowCriticalValue != null)
                    lowCritical = lowCriticalValue;

                String highCriticalValue = getNumericText(extras, "high_critical");
                if (highCriticalValue != null)
                    highCritical = highCriticalValue;
            }

            // Try direct fields
            String lowValidDirect = getNumericText(concept, "low_valid");
            if (lowValidDirect != null)
                lowValid = lowValidDirect;

            String highValidDirect = getNumericText(concept, "high_valid");
            if (highValidDirect != null)
                highValid = highValidDirect;

            String sigDigitsDirect = getNumericText(concept, "significant_digits");
            if (sigDigitsDirect != null)
                sigDigits = sigDigitsDirect;
        }

        jsonWad.put("lowValid", lowValid);
        jsonWad.put("highValid", highValid);
        jsonWad.put("lowReportingRange", lowReporting);
        jsonWad.put("highReportingRange", highReporting);
        jsonWad.put("lowCritical", lowCritical);
        jsonWad.put("highCritical", highCritical);
        jsonWad.put("significantDigits", sigDigits);
    }

    private void mapResultLimits(JsonNode concept, ObjectNode jsonWad) {
        // Create the exact structure as shown in your target format
        String resultLimitsJson = "["
                + "{\"highAgeRange\": \"30\", \"gender\": false, \"lowNormal\": \"-Infinity\", \"highNormal\": \"Infinity\", \"lowCritical\": \"-Infinity\", \"highCritical\": \"Infinity\", \"reportingRange\": \"\"}, "
                + "{\"highAgeRange\": \"365\", \"gender\": false, \"lowNormal\": \"-Infinity\", \"highNormal\": \"Infinity\", \"lowCritical\": \"-Infinity\", \"highCritical\": \"Infinity\", \"reportingRange\": \"\"}, "
                + "{\"highAgeRange\": \"1825\", \"gender\": false, \"lowNormal\": \"-Infinity\", \"highNormal\": \"Infinity\", \"lowCritical\": \"-Infinity\", \"highCritical\": \"Infinity\", \"reportingRange\": \"\"}, "
                + "{\"highAgeRange\": \"5110\", \"gender\": false, \"lowNormal\": \"-Infinity\", \"highNormal\": \"Infinity\", \"lowCritical\": \"-Infinity\", \"highCritical\": \"Infinity\", \"reportingRange\": \"\"}, "
                + "{\"highAgeRange\": \"Infinity\", \"gender\": false, \"lowNormal\": \"-Infinity\", \"highNormal\": \"Infinity\", \"lowCritical\": \"-Infinity\", \"highCritical\": \"Infinity\", \"reportingRange\": \"\"}"
                + "]";

        jsonWad.put("resultLimits", resultLimitsJson);
    }

    private void mapDictionaryResults(JsonNode concept, ObjectNode jsonWad) {
        String dataType = getText(concept, "datatype");
        boolean isDictionary = dataType != null
                && (dataType.toLowerCase().contains("coded") || dataType.toLowerCase().contains("select"));

        if (isDictionary) {
            // Map dictionary values if available
            ArrayNode dictionaryArray = objectMapper.createArrayNode();

            // Try to extract possible values from OCL concept
            JsonNode answers = concept.get("answers");
            if (answers != null && answers.isArray()) {
                for (JsonNode answer : answers) {
                    ObjectNode dictEntry = objectMapper.createObjectNode();
                    String answerId = getText(answer, "id");
                    String answerName = getText(answer, "display_name");
                    if (answerId != null) {
                        dictEntry.put("value", answerName != null ? answerName : answerId);
                        dictEntry.put("qualified", "N");
                        dictionaryArray.add(dictEntry);
                    }
                }
            }

            if (dictionaryArray.size() > 0) {
                jsonWad.put("dictionary", dictionaryArray.toString());
                // Set first entry as default
                jsonWad.put("defaultTestResult", dictionaryArray.get(0).get("value").asText());
                jsonWad.put("dictionaryReference", dictionaryArray.get(0).get("value").asText());
            }
        }
    }

    private void setDefaultValues(ObjectNode jsonWad) {
        // Ensure all required fields have values matching your target format
        if (!jsonWad.has("testNameEnglish") || jsonWad.get("testNameEnglish").asText().isEmpty()) {
            jsonWad.put("testNameEnglish", "");
        }

        if (!jsonWad.has("testNameFrench") || jsonWad.get("testNameFrench").asText().isEmpty()) {
            jsonWad.put("testNameFrench", "");
        }

        if (!jsonWad.has("testReportNameEnglish") || jsonWad.get("testReportNameEnglish").asText().isEmpty()) {
            jsonWad.put("testReportNameEnglish", "");
        }

        if (!jsonWad.has("testReportNameFrench") || jsonWad.get("testReportNameFrench").asText().isEmpty()) {
            jsonWad.put("testReportNameFrench", "");
        }

        if (!jsonWad.has("testSection") || jsonWad.get("testSection").asText().isEmpty()) {
            jsonWad.put("testSection", "");
        }

        if (!jsonWad.has("panels")) {
            jsonWad.set("panels", objectMapper.createArrayNode());
        }

        if (!jsonWad.has("uom")) {
            jsonWad.put("uom", "");
        }

        if (!jsonWad.has("loinc")) {
            jsonWad.put("loinc", "");
        }

        if (!jsonWad.has("resultType")) {
            jsonWad.put("resultType", "");
        }

        if (!jsonWad.has("orderable")) {
            jsonWad.put("orderable", "Y");
        }

        if (!jsonWad.has("notifyResults")) {
            jsonWad.put("notifyResults", "");
        }

        if (!jsonWad.has("inLabOnly")) {
            jsonWad.put("inLabOnly", "");
        }

        if (!jsonWad.has("antimicrobialResistance")) {
            jsonWad.put("antimicrobialResistance", "");
        }

        if (!jsonWad.has("active")) {
            jsonWad.put("active", "Y");
        }

        if (!jsonWad.has("sampleTypes")) {
            jsonWad.set("sampleTypes", objectMapper.createArrayNode());
        }

        // Ensure all numeric validation fields exist
        if (!jsonWad.has("lowValid")) {
            jsonWad.put("lowValid", "");
        }
        if (!jsonWad.has("highValid")) {
            jsonWad.put("highValid", "");
        }
        if (!jsonWad.has("lowReportingRange")) {
            jsonWad.put("lowReportingRange", "");
        }
        if (!jsonWad.has("highReportingRange")) {
            jsonWad.put("highReportingRange", "");
        }
        if (!jsonWad.has("lowCritical")) {
            jsonWad.put("lowCritical", "");
        }
        if (!jsonWad.has("highCritical")) {
            jsonWad.put("highCritical", "");
        }
        if (!jsonWad.has("significantDigits")) {
            jsonWad.put("significantDigits", "");
        }

        // Ensure resultLimits exists
        if (!jsonWad.has("resultLimits")) {
            mapResultLimits(null, jsonWad);
        }
    }

    private String inferSectionFromTestId(String testId) {
        String testIdUpper = testId.toUpperCase();

        if (testIdUpper.contains("HEMA") || testIdUpper.contains("CBC") || testIdUpper.contains("BLOOD")) {
            return "36"; // Hematology
        } else if (testIdUpper.contains("BIOCHEM") || testIdUpper.contains("CHEM")) {
            return "56"; // Biochemistry
        } else if (testIdUpper.contains("IMMUNO") || testIdUpper.contains("HIV")) {
            return "59"; // Immunology
        } else if (testIdUpper.contains("MICRO") || testIdUpper.contains("CULTURE")) {
            return "163"; // Pathology
        } else if (testIdUpper.contains("SERO")) {
            return "97"; // Serology
        } else if (testIdUpper.contains("PANEL")) {
            return "36"; // Default panels to Hematology
        }

        return null;
    }

    private String getText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    private String getNumericText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()
                    && (value.isNumber() || (value.isTextual() && !value.asText().isEmpty()))) {
                return value.asText().trim();
            }
        }
        return null;
    }
}