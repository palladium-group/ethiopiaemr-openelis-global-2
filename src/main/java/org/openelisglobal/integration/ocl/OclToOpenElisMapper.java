package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.TestAddParams;
import java.util.ArrayList;
import java.util.List;

public class OclToOpenElisMapper {
    public List<TestAddParams> mapConceptToTestParams(JsonNode concept) {
        try {
            List<TestAddParams> paramsList = new ArrayList<>();
            TestAddParams params = new org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.TestAddParams();

            // --- Map only required fields with explicit type checks ---

            // 1. Map names by locale (English/French only)
            JsonNode names = concept.get("names");
            if (names != null && names.isArray()) {
                for (JsonNode nameNode : names) {
                    String locale = getText(nameNode, "locale");
                    String value = getText(nameNode, "name");
                    if ("en".equals(locale)) {
                        params.testNameEnglish = value;
                        params.testReportNameEnglish = value;
                    } else if ("fr".equals(locale)) {
                        params.testNameFrench = value;
                        params.testReportNameFrench = value;
                    }
                }
            }

            // 2. OCL ID (string only)
            if (concept.has("id") && concept.get("id").isTextual()) {
                params.testId = concept.get("id").asText();
            }

            // 3. LOINC code from mappings (string only)
            JsonNode mappings = concept.get("mappings");
            if (mappings != null && mappings.isArray()) {
                for (JsonNode mapping : mappings) {
                    if ("SAME-AS".equals(getText(mapping, "map_type"))) {
                        if (mapping.has("to_concept_code") && mapping.get("to_concept_code").isTextual()) {
                            params.loinc = mapping.get("to_concept_code").asText();
                        }
                        break;
                    }
                }
            }

            // 4. Data type/result type
            String dataType = getText(concept, "datatype");
            if ("Numeric".equalsIgnoreCase(dataType)) {
                params.resultTypeId = "N";
            } else if ("Coded".equalsIgnoreCase(dataType)) {
                params.resultTypeId = "D";
            } else {
                params.resultTypeId = "T";
            }

            // 5. Ranges, units, significant digits from extras (only if string/number)
            JsonNode extras = concept.get("extras");
            if (extras != null) {
                if (extras.has("low_absolute") && (extras.get("low_absolute").isNumber() || extras.get("low_absolute").isTextual())) {
                    params.lowValid = extras.get("low_absolute").asText();
                }
                if (extras.has("hi_normal") && (extras.get("hi_normal").isNumber() || extras.get("hi_normal").isTextual())) {
                    params.highValid = extras.get("hi_normal").asText();
                }
                if (extras.has("significant_digits") && (extras.get("significant_digits").isInt() || extras.get("significant_digits").isTextual())) {
                    params.significantDigits = extras.get("significant_digits").asText();
                }
                if (extras.has("units") && extras.get("units").isTextual()) {
                    params.uomId = extras.get("units").asText();
                }
            }

            // 6. Set default values (as before)
            params.active = "Y";
            params.orderable = "Y";
            params.notifyResults = "Y";
            params.inLabOnly = "N";
            params.antimicrobialResistance = "N";

            // 7. Map concept class to test section (Lab Set = PANEL, else TEST)
            String conceptClass = getText(concept, "concept_class");
            if ("Lab Set".equalsIgnoreCase(conceptClass)) {
                params.testSectionId = "PANEL";
                // If components exist, map them as individual tests
                JsonNode components = concept.get("components");
                if (components != null && components.isArray()) {
                    for (JsonNode component : components) {
                        TestAddParams componentParams = new org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.TestAddParams();
                        JsonNode componentNames = component.get("names");
                        if (componentNames != null && componentNames.isArray()) {
                            for (JsonNode componentNameNode : componentNames) {
                                String locale = getText(componentNameNode, "locale");
                                String value = getText(componentNameNode, "name");
                                if ("en".equals(locale)) {
                                    componentParams.testNameEnglish = value;
                                    componentParams.testReportNameEnglish = value;
                                } else if ("fr".equals(locale)) {
                                    componentParams.testNameFrench = value;
                                    componentParams.testReportNameFrench = value;
                                }
                            }
                        }
                        componentParams.testSectionId = "TEST";
                        // Use component's own id if present
                        if (component.has("id") && component.get("id").isTextual()) {
                            componentParams.testId = component.get("id").asText();
                        }
                        paramsList.add(componentParams);
                    }
                }
            } else {
                params.testSectionId = "TEST";
            }

            // 8. Only map fields in the mapping table; skip all others
            // DEBUG LOGGING: Print all mapped fields and their types for concept id '1006'
            if (params.testId != null && params.testId.equals("1006")) {
                System.out.println("DEBUG OCL MAPPING FOR 1006:");
                System.out.println("  testId: " + params.testId + " (" + (params.testId != null ? params.testId.getClass() : "null") + ")");
                System.out.println("  loinc: " + params.loinc + " (" + (params.loinc != null ? params.loinc.getClass() : "null") + ")");
                System.out.println("  testNameEnglish: " + params.testNameEnglish + " (" + (params.testNameEnglish != null ? params.testNameEnglish.getClass() : "null") + ")");
                System.out.println("  testNameFrench: " + params.testNameFrench + " (" + (params.testNameFrench != null ? params.testNameFrench.getClass() : "null") + ")");
                System.out.println("  significantDigits: " + params.significantDigits + " (" + (params.significantDigits != null ? params.significantDigits.getClass() : "null") + ")");
                System.out.println("  lowValid: " + params.lowValid + " (" + (params.lowValid != null ? params.lowValid.getClass() : "null") + ")");
                System.out.println("  highValid: " + params.highValid + " (" + (params.highValid != null ? params.highValid.getClass() : "null") + ")");
                System.out.println("  uomId: " + params.uomId + " (" + (params.uomId != null ? params.uomId.getClass() : "null") + ")");
                System.out.println("  resultTypeId: " + params.resultTypeId + " (" + (params.resultTypeId != null ? params.resultTypeId.getClass() : "null") + ")");
                System.out.println("  testSectionId: " + params.testSectionId + " (" + (params.testSectionId != null ? params.testSectionId.getClass() : "null") + ")");
            }
            paramsList.add(params);
            return paramsList;
        } catch (Exception e) {
            // log error if you have a logger
            return null;
        }
    }

    private String getText(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }
}
