// File: OclToOpenElisMapper.java (Corrected field name "concept class")
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
        RESULT_TYPE_MAPPING.put("N/A", "T"); // Assuming N/A maps to Text
    }

    /**
     * Creates a wrapper node with concepts array for a single concept
     * 
     * @param singleConcept The JSON node representing a single OCL concept.
     * @return A JSON node wrapper containing the single concept in a "concepts"
     *         array.
     */
    public JsonNode createConceptWrapper(JsonNode singleConcept) {
        ObjectNode wrapper = objectMapper.createObjectNode();
        ArrayNode conceptsArray = objectMapper.createArrayNode();
        conceptsArray.add(singleConcept);
        wrapper.set("concepts", conceptsArray);
        return wrapper;
    }

    /**
     * Maps OCL concepts to TestAddForm objects ready for submission, applying
     * filters for datatype and concept_class.
     * 
     * @param rootNode The root JSON node from OCL export, containing a "concepts" array.
     * @return A list of TestAddForm objects that pass the filters.
     */
    public List<TestAddForm> mapConceptsToTestAddForms(JsonNode rootNode) {
        try {
            List<TestAddForm> forms = new ArrayList<>();

            // Validate root node structure
            if (!rootNode.has("type") || !"Collection Version".equals(getText(rootNode, "type"))) {
                System.err.println("Error: Invalid OCL export format. Expected Collection Version type.");
                return forms;
            }

            // Handle concepts array from OCL export
            JsonNode concepts = rootNode.get("concepts");
            if (concepts != null && concepts.isArray()) {
                System.out.println("Processing " + concepts.size() + " concepts from collection: " + 
                                 getText(rootNode, "full_name"));
                
                for (JsonNode conceptNode : concepts) {
                    String conceptId = getText(conceptNode, "id");
                    String displayName = getText(conceptNode, "display_name");
                    System.out.println("\nProcessing concept: " + displayName + " (ID: " + conceptId + ")");
                    
                    TestAddForm form = mapSingleConceptToForm(conceptNode);
                    if (form != null) {
                        forms.add(form);
                    }
                }
            } else {
                System.err.println("Error: Expected 'concepts' array in OCL export");
            }

            return forms;
        } catch (Exception e) {
            System.err.println("Error mapping OCL concepts to TestAddForm: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Maps a single OCL concept to a TestAddForm, applying filters and specific
     * mappings.
     * 
     * @param concept The JSON node representing a single OCL concept.
     * @return A TestAddForm object if the concept passes all filters, otherwise
     *         null.
     */
    private TestAddForm mapSingleConceptToForm(JsonNode concept) {
        try {
            String conceptId = getText(concept, "id");
            String displayName = getText(concept, "display_name");
            String dataType = getText(concept, "datatype");
            String conceptClass = getText(concept, "concept_class");

            // Log detailed concept information for debugging
            logConceptDetails(concept);

            // Datatype filtering: Only map specific test types
            if (dataType == null || !(dataType.equalsIgnoreCase("NUMERIC") || dataType.equalsIgnoreCase("NUMBER")
                    || dataType.equalsIgnoreCase("TEXT") || dataType.equalsIgnoreCase("STRING")
                    || dataType.equalsIgnoreCase("CODED") || dataType.equalsIgnoreCase("SELECT")
                    || dataType.equalsIgnoreCase("N/A"))) {
                System.out.println(
                        "Skipping concept " + conceptId + " due to unsupported datatype: " + dataType);
                return null;
            }

            // Concept Class filtering: Only import specific concept classes.
            boolean isAllowedConceptClass = false;

            // Log concept class information
            System.out.println("Processing concept ID: " + conceptId + ", Name: " + displayName);
            System.out.println("Concept Class: " + conceptClass + ", Data Type: " + dataType);

            if (conceptClass != null) {
                // Normal concept class validation
                String normalizedConceptClass = conceptClass.toUpperCase();
                if (normalizedConceptClass.equals("TEST") || 
                    normalizedConceptClass.equals("LAB SET") ||
                    normalizedConceptClass.equals("LABSET") ||
                    normalizedConceptClass.equals("MISC") || 
                    normalizedConceptClass.equals("FINDING") ||
                    normalizedConceptClass.equals("PROCEDURE")) {
                    isAllowedConceptClass = true;
                    System.out.println("✓ Concept class '" + conceptClass + "' is allowed for concept " + conceptId);
                }
            }

            // If concept_class is null or not allowed, try to infer
            if (!isAllowedConceptClass) {
                System.out.println("Attempting to infer concept class for concept: " + conceptId);

                // Infer concept class based on available information
                if (dataType != null && 
                    (dataType.equalsIgnoreCase("Numeric") || 
                     dataType.equalsIgnoreCase("Coded") ||
                     dataType.equalsIgnoreCase("Text"))) {
                    conceptClass = "Test";
                    isAllowedConceptClass = true;
                    System.out.println("✓ Inferred concept_class 'Test' for concept " + conceptId + 
                                     " based on datatype: " + dataType);
                }
            }

            if (!isAllowedConceptClass) {
                System.out.println("✗ Skipping concept " + conceptId + 
                                 " (name: " + displayName + ") " +
                                 "due to unsupported concept_class: " + conceptClass);
                return null;
            }

            TestAddForm form = new TestAddForm();

            // Create the JSON structure expected by TestAddRestController
            ObjectNode jsonWad = objectMapper.createObjectNode();

            // Map all required fields in the exact format
            mapTestNames(concept, jsonWad);
            mapTestSection(concept, jsonWad); // Hardcoded to Hematology
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
            String jsonWadString = objectMapper.writeValueAsString(jsonWad);
            form.setJsonWad(jsonWadString);

            // Also set LOINC separately as it's used for validation or direct access
            String loinc = getText(concept, "loinc");
            form.setLoinc(loinc != null ? loinc : "");

            System.out.println("✓ Successfully mapped OCL concept " + conceptId + " to TestAddForm");
            System.out.println("  Generated JSON: " + jsonWadString);

            return form;
        } catch (Exception e) {
            System.err.println("✗ Error mapping single OCL concept: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void mapTestNames(JsonNode concept, ObjectNode jsonWad) {
        String englishName = null;
        String frenchName = null;

        // Get display_name as initial English name
        englishName = getText(concept, "display_name");
        
        // Process names array for best matching names
        JsonNode names = concept.get("names");
        if (names != null && names.isArray()) {
            // Process names for both FULLY_SPECIFIED and preferred names
            for (JsonNode nameNode : names) {
                String locale = getText(nameNode, "locale");
                String name = getText(nameNode, "name");
                String nameType = getText(nameNode, "name_type");
                boolean isPreferred = nameNode.has("locale_preferred") && nameNode.get("locale_preferred").asBoolean();

                // Handle FULLY_SPECIFIED preferred names first
                if (name != null && "FULLY_SPECIFIED".equals(nameType) && isPreferred) {
                    if ("en".equals(locale)) {
                        englishName = name;  // Overwrite display_name with preferred English name
                    } else if ("fr".equals(locale)) {
                        frenchName = name;
                    }
                }
                // Handle other cases
                else if (name != null) {
                    // For English, prefer FULLY_SPECIFIED name if display_name wasn't found
                    if ("en".equals(locale) && englishName == null && 
                        ("FULLY_SPECIFIED".equals(nameType) || isPreferred)) {
                        englishName = name;
                    }
                    // For French, prefer FULLY_SPECIFIED name
                    else if ("fr".equals(locale) && 
                            ("FULLY_SPECIFIED".equals(nameType) || isPreferred)) {
                        frenchName = name;
                    }
                }
            }
        }

        // Further fallbacks if still no English name
        if (englishName == null) {
            JsonNode descriptions = concept.get("descriptions");
            if (descriptions != null && descriptions.isArray() && descriptions.size() > 0) {
                englishName = getText(descriptions.get(0), "description");
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
        // Requirement: Map all OCL concepts to Hematology (ID: "36")
        jsonWad.put("testSection", "36");
    }

    private void mapPanels(JsonNode concept, ObjectNode jsonWad) {
        // Initialize as empty array - can be enhanced to map OCL panel relationships
        ArrayNode panelsArray = objectMapper.createArrayNode();
        jsonWad.set("panels", panelsArray);
    }

    private void mapUnits(JsonNode concept, ObjectNode jsonWad) {
        String units = null;

        // Try extras first (OCL standard: units attribute in extras)
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

        // Try mappings array first (OCL standard format for LOINC)
        JsonNode mappings = concept.get("mappings");
        if (mappings != null && mappings.isArray()) {
            for (JsonNode mapping : mappings) {
                String mapType = getText(mapping, "map_type");
                String toSourceName = getText(mapping, "to_source_name");
                
                // Look for LOINC mappings specifically
                if (("SAME-AS".equals(mapType) || "LOINC".equals(mapType) || "BROADER-THAN".equals(mapType)) && 
                    "LOINC".equals(toSourceName)) {
                    loinc = getText(mapping, "to_concept_code");
                    if (loinc != null) {
                        System.out.println("Found LOINC code: " + loinc + " for concept " + getText(concept, "id"));
                        break;
                    }
                }
            }
        }

        // Try extras section for LOINC
        if (loinc == null) {
            JsonNode extras = concept.get("extras");
            if (extras != null && extras.has("loinc")) {
                loinc = getText(extras, "loinc");
            }
        }

        // Fallback to direct loinc field
        if (loinc == null) {
            loinc = getText(concept, "loinc");
        }

        jsonWad.put("loinc", loinc != null ? loinc : "");
        if (loinc != null) {
            System.out.println("Mapped LOINC: " + loinc + " for concept " + getText(concept, "id"));
        }
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

        // Convert to actual DB ID if TypeOfTestResultService is available via
        // SpringContext
        try {
            TypeOfTestResultService service = SpringContext.getBean(TypeOfTestResultService.class);
            TypeOfTestResult typeObj = service.getTypeOfTestResultByType(resultTypeId);
            if (typeObj != null && typeObj.getId() != null) {
                jsonWad.put("resultType", typeObj.getId());
            } else {
                jsonWad.put("resultType", resultTypeId); // Fallback to character value if ID not found
            }
        } catch (Exception e) {
            System.err.println(
                    "Error mapping result type (Spring context not available or service failed): " + e.getMessage());
            jsonWad.put("resultType", resultTypeId); // Use character value as fallback
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
                    testOrder.put("id", 0); // New test placeholder (ID 0 usually means the test being added)
                    testsArray.add(testOrder);

                    sampleTypeObj.set("tests", testsArray);
                    sampleTypesArray.add(sampleTypeObj);
                }
            }
        }

        // Default to Serum (ID: 31) if no specific sample type is defined, as per
        // typical OpenELIS usage
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
            // Map validation ranges from 'extras' (OCL standard attributes)
            JsonNode extras = concept.get("extras");

            if (extras != null) {
                String lowAbs = getNumericText(extras, "low_absolute");
                if (lowAbs != null) {
                    lowValid = lowAbs;
                    System.out.println("Found low_absolute: " + lowAbs + " for concept " + getText(concept, "id"));
                }

                String hiNormal = getNumericText(extras, "hi_normal");
                if (hiNormal != null) {
                    highValid = hiNormal;
                    System.out.println("Found hi_normal: " + hiNormal + " for concept " + getText(concept, "id"));
                }

                // Check for allow_decimal which indicates significant digits
                String allowDecimal = getNumericText(extras, "allow_decimal");
                if (allowDecimal != null) {
                    if ("true".equalsIgnoreCase(allowDecimal)) {
                        sigDigits = "2"; // Default to 2 decimal places if decimals are allowed
                    } else if ("false".equalsIgnoreCase(allowDecimal)) {
                        sigDigits = "0"; // No decimal places
                    } else {
                        sigDigits = allowDecimal; // Use as-is if it's a number
                    }
                    System.out.println("Found allow_decimal: " + allowDecimal + " -> sigDigits: " + sigDigits + 
                                     " for concept " + getText(concept, "id"));
                }

                // Look for other validation ranges
                String lowReportingValue = getNumericText(extras, "low_reporting");
                if (lowReportingValue != null) lowReporting = lowReportingValue;

                String highReportingValue = getNumericText(extras, "high_reporting");
                if (highReportingValue != null) highReporting = highReportingValue;

                String lowCriticalValue = getNumericText(extras, "low_critical");
                if (lowCriticalValue != null) lowCritical = lowCriticalValue;

                String highCriticalValue = getNumericText(extras, "high_critical");
                if (highCriticalValue != null) highCritical = highCriticalValue;
            }

            // Try direct fields as fallback
            if (lowValid.isEmpty()) {
                String lowValidDirect = getNumericText(concept, "low_valid");
                if (lowValidDirect != null) lowValid = lowValidDirect;
            }

            if (highValid.isEmpty()) {
                String highValidDirect = getNumericText(concept, "high_valid");
                if (highValidDirect != null) highValid = highValidDirect;
            }

            if (sigDigits.isEmpty()) {
                String sigDigitsDirect = getNumericText(concept, "significant_digits");
                if (sigDigitsDirect != null) sigDigits = sigDigitsDirect;
            }

            // Log the numeric validation setup
            if (!lowValid.isEmpty() || !highValid.isEmpty() || !sigDigits.isEmpty()) {
                System.out.println("Numeric validation for concept " + getText(concept, "id") + 
                                 ": lowValid=" + lowValid + ", highValid=" + highValid + 
                                 ", sigDigits=" + sigDigits);
            }
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
        // This is a fixed set of age ranges and normal/critical limits
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

            // Try to extract possible values from OCL concept's "answers"
            JsonNode answers = concept.get("answers");
            if (answers != null && answers.isArray()) {
                for (JsonNode answer : answers) {
                    ObjectNode dictEntry = objectMapper.createObjectNode();
                    String answerId = getText(answer, "id");
                    String answerName = getText(answer, "display_name");
                    if (answerId != null) {
                        dictEntry.put("value", answerName != null ? answerName : answerId);
                        dictEntry.put("qualified", "N"); // Default to non-quantifiable
                        dictionaryArray.add(dictEntry);
                    }
                }
            }

            if (dictionaryArray.size() > 0) {
                jsonWad.put("dictionary", dictionaryArray.toString());
                // Set first entry as default for dictionaryReference and defaultTestResult
                jsonWad.put("defaultTestResult", dictionaryArray.get(0).get("value").asText());
                jsonWad.put("dictionaryReference", dictionaryArray.get(0).get("value").asText());
            }
        }
    }

    private void setDefaultValues(ObjectNode jsonWad) {
        // Ensure all required fields have values matching your target format,
        // if they haven't been mapped from the OCL concept.
        if (!jsonWad.has("testNameEnglish") || jsonWad.get("testNameEnglish").asText().isEmpty()) {
            jsonWad.put("testNameEnglish", "Unknown Test");
            System.out.println("Warning: Set default testNameEnglish");
        }

        if (!jsonWad.has("testNameFrench") || jsonWad.get("testNameFrench").asText().isEmpty()) {
            jsonWad.put("testNameFrench", jsonWad.get("testNameEnglish").asText());
            System.out.println("Warning: Set default testNameFrench from English");
        }

        if (!jsonWad.has("testReportNameEnglish") || jsonWad.get("testReportNameEnglish").asText().isEmpty()) {
            jsonWad.put("testReportNameEnglish", jsonWad.get("testNameEnglish").asText());
            System.out.println("Warning: Set default testReportNameEnglish from testNameEnglish");
        }

        if (!jsonWad.has("testReportNameFrench") || jsonWad.get("testReportNameFrench").asText().isEmpty()) {
            jsonWad.put("testReportNameFrench", jsonWad.get("testNameFrench").asText());
            System.out.println("Warning: Set default testReportNameFrench from testNameFrench");
        }

        // testSection is now hardcoded in mapTestSection, but keep this for robustness
        if (!jsonWad.has("testSection") || jsonWad.get("testSection").asText().isEmpty()) {
            jsonWad.put("testSection", "36"); // Default to Hematology
            System.out.println("Warning: Set default testSection to Hematology (36)");
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

        if (!jsonWad.has("resultType") || jsonWad.get("resultType").asText().isEmpty()) {
            jsonWad.put("resultType", "T"); // Default to Text result type
            System.out.println("Warning: Set default resultType to Text (T)");
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
            // Create default sample type array with Serum
            ArrayNode sampleTypesArray = objectMapper.createArrayNode();
            ObjectNode sampleType = objectMapper.createObjectNode();
            sampleType.put("typeId", "31"); // Serum ID

            ArrayNode testsArray = objectMapper.createArrayNode();
            ObjectNode testOrder = objectMapper.createObjectNode();
            testOrder.put("id", 0); // New test placeholder
            testsArray.add(testOrder);

            sampleType.set("tests", testsArray);
            sampleTypesArray.add(sampleType);
            jsonWad.set("sampleTypes", sampleTypesArray);
            System.out.println("Warning: Set default sampleTypes to Serum (31)");
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

        // Ensure dictionary field exists for coded results
        if (!jsonWad.has("dictionary")) {
            jsonWad.put("dictionary", "");
        }
        if (!jsonWad.has("dictionaryReference")) {
            jsonWad.put("dictionaryReference", "");
        }
        if (!jsonWad.has("defaultTestResult")) {
            jsonWad.put("defaultTestResult", "");
        }
    }

    /**
     * Helper to safely extract text from a JsonNode field.
     * 
     * @param node  The JsonNode to extract from.
     * @param field The name of the field.
     * @return The text value, or null if the node or field is missing/null.
     */
    private String getText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText().trim();
            }
        }
        return null;
    }

    /**
     * Helper method to log detailed concept information for debugging
     */
    private void logConceptDetails(JsonNode concept) {
        try {
            StringBuilder details = new StringBuilder("\nConcept Details:\n");
            details.append("ID: ").append(getText(concept, "id")).append("\n");
            details.append("Display Name: ").append(getText(concept, "display_name")).append("\n");
            details.append("Concept Class: ").append(getText(concept, "concept_class")).append("\n");
            details.append("DataType: ").append(getText(concept, "datatype")).append("\n");
            
            // Log names
            JsonNode names = concept.get("names");
            if (names != null && names.isArray()) {
                details.append("Names:\n");
                for (JsonNode name : names) {
                    details.append("  - ").append(getText(name, "name"))
                           .append(" (").append(getText(name, "locale")).append(")")
                           .append(" [").append(getText(name, "name_type")).append("]\n");
                }
            }

            System.out.println(details.toString());
        } catch (Exception e) {
            System.err.println("Error logging concept details: " + e.getMessage());
        }
    }

    /**
     * Helper to safely extract numeric text from a JsonNode field, handling both
     * number and text nodes.
     * 
     * @param node  The JsonNode to extract from.
     * @param field The name of the field.
     * @return The numeric text value, or null if the node or field is
     *         missing/null/empty.
     */
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