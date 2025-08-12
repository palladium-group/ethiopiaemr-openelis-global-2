// File: OclImportInitializer.java
package org.openelisglobal.integration.ocl;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testconfiguration.controller.TestAddController;
import org.openelisglobal.testconfiguration.form.TestAddForm;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class OclImportInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(OclImportInitializer.class);
    private static final String CHECKSUM_FILE = "ocl_import_checksums.txt";
    
    @Value("${org.openelisglobal.ocl.import.directory:src/main/resources/configurations/ocl}")
    private String oclImportDirectory;

    @Autowired
    private OclZipImporter oclZipImporter;

    // Autowire necessary services directly as we are copying controller logic
    @Autowired
    private TestAddService testAddService;
    @Autowired
    private TestService testService;
    @Autowired
    private DisplayListService displayListService;
    @Autowired
    private UnitOfMeasureService unitOfMeasureService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private PanelService panelService;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private TestResultService testResultService;
    // TestAddController is needed only for its inner TestSet class, not for its
    // methods,
    // so we can keep it as is or refactor TestSet out.
    @Autowired
    private TestAddController testAddController;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // Check if OCL files have already been processed
            if (hasAlreadyProcessedOclFiles()) {
                log.info("OCL Import: Files already processed (checksum match found). Skipping import.");
                return;
            }
            
            List<JsonNode> oclNodes = oclZipImporter.importOclZip();
            log.info("OCL Import: Found {} nodes to process.", oclNodes.size());

            int conceptCount = 0;
            int testsCreated = 0;
            int testsSkipped = 0;

            OclToOpenElisMapper mapper = new OclToOpenElisMapper();

            for (JsonNode node : oclNodes) {
                // If the node is a Collection Version, get its concepts array
                if (node.has("concepts") && node.get("concepts").isArray()) {
                    log.info("OCL Import: Node has a concepts array of size {}.", node.get("concepts").size());

                    // Map all concepts in this node to TestAddForms
                    List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(node);

                    for (TestAddForm form : testForms) {
                        conceptCount++;

                        try {
                            log.info("OCL Import: Processing concept #{} - attempting to create test", conceptCount);

                            // Directly use the copied controller logic
                            boolean success = createTestFromFormLogic(form, "OCL_IMPORTER"); // Pass a specific user ID
                            if (success) {
                                testsCreated++;
                                log.info("OCL Import: Successfully created test #{}", testsCreated);
                            } else {
                                testsSkipped++;
                                log.warn("OCL Import: Failed to create test for concept #{} (form: {})", conceptCount,
                                        form);
                            }

                        } catch (Exception ex) {
                            testsSkipped++;
                            log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                        }
                    }
                } else if (node.isArray()) {
                    // Handle direct array of concepts
                    log.info("OCL Import: Node is a direct array of size {}.", node.size());

                    for (JsonNode concept : node) {
                        conceptCount++;

                        try {
                            log.info("OCL Import: Processing concept #{} with id: {}", conceptCount,
                                    concept.has("id") ? concept.get("id").asText() : "unknown");

                            // Create a wrapper node for the single concept
                            JsonNode wrapperNode = mapper.createConceptWrapper(concept);
                            List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(wrapperNode);

                            for (TestAddForm form : testForms) {
                                try {
                                    boolean success = createTestFromFormLogic(form, "OCL_IMPORTER");
                                    if (success) {
                                        testsCreated++;
                                        log.info("OCL Import: Successfully created test #{}", testsCreated);
                                    } else {
                                        testsSkipped++;
                                        log.warn("OCL Import: Failed to create test for concept #{} (form: {})",
                                                conceptCount, form);
                                    }
                                } catch (Exception ex) {
                                    testsSkipped++;
                                    log.error("OCL Import: Failed to create test for concept #{}", conceptCount, ex);
                                }
                            }

                        } catch (Exception ex) {
                            testsSkipped++;
                            log.error("OCL Import: Failed to process concept #{}", conceptCount, ex);
                        }
                    }
                } else {
                    // Handle single concept
                    conceptCount++;

                    try {
                        log.info("OCL Import: Processing single concept with id: {}",
                                node.has("id") ? node.get("id").asText() : "unknown");

                        // Create a wrapper node for the single concept
                        JsonNode wrapperNode = mapper.createConceptWrapper(node);
                        List<TestAddForm> testForms = mapper.mapConceptsToTestAddForms(wrapperNode);

                        for (TestAddForm form : testForms) {
                            try {
                                boolean success = createTestFromFormLogic(form, "OCL_IMPORTER");
                                if (success) {
                                    testsCreated++;
                                    log.info("OCL Import: Successfully created test #{}", testsCreated);
                                } else {
                                    testsSkipped++;
                                    log.warn("OCL Import: Failed to create test for single concept (form: {})", form);
                                }
                            } catch (Exception ex) {
                                testsSkipped++;
                                log.error("OCL Import: Failed to create test for single concept", ex);
                            }
                        }

                    } catch (Exception ex) {
                        testsSkipped++;
                        log.error("OCL Import: Failed to process single concept", ex);
                    }
                }
            }

            log.info(
                    "OCL Import: Finished processing. Total concepts processed: {}, Tests created: {}, Tests skipped: {}",
                    conceptCount, testsCreated, testsSkipped);
            
            // Save checksum to prevent re-processing
            saveCurrentOclChecksum();

        } catch (java.io.IOException e) {
            log.error("OCL Import failed during file processing", e);
        }

        // Refresh display lists and clear caches
        try {
            // These services are already autowired above if needed in the copied logic
            displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
            SpringContext.getBean(TypeOfSampleService.class).clearCache();

            log.info("OCL Import: Successfully refreshed display lists and cleared caches");
        } catch (Exception e) {
            log.error("Failed to refresh display lists or clear cache after OCL import", e);
        }
    }

    /**
     * Copied and modified logic from TestAddRestController.postTestAdd to be used
     * directly in initializer. This method assumes validation and filtering for
     * datatype and concept_class has already occurred in the OclToOpenElisMapper.
     *
     * @param form          The TestAddForm containing parsed OCL concept data.
     * @param currentUserId The user ID to associate with the test creation (e.g.,
     *                      "OCL_IMPORTER").
     * @return true if the test was successfully added, false otherwise.
     */
    private boolean createTestFromFormLogic(TestAddForm form, String currentUserId) {
        String jsonString = form.getJsonWad();
        JSONParser parser = new JSONParser();
        JSONObject obj = null;
        try {
            obj = (JSONObject) parser.parse(jsonString);
        } catch (ParseException e) {
            LogEvent.logError("Failed to parse JSON for TestAddForm: " + e.getMessage(), e);
            return false;
        }

        TestAddParams testAddParams = extractTestAddParms(obj, parser);

        // LOINC validation check: If LOINC is already in use, skip this test.
        // This is a direct check without Spring's BindingResult, simplifying the
        // integration.
        if (!GenericValidator.isBlankOrNull(testAddParams.loinc) && isLoincAlreadyUsed(testAddParams.loinc)) {
            log.warn("Skipping test creation for LOINC code '{}' as it is already in use.", testAddParams.loinc);
            return false;
        }

        List<TestAddController.TestSet> testSets = createTestSets(testAddParams);
        Localization nameLocalization = createNameLocalization(testAddParams);
        Localization reportingNameLocalization = createReportingNameLocalization(testAddParams);

        try {
            testAddService.addTests(testSets, nameLocalization, reportingNameLocalization, currentUserId);
            return true;
        } catch (HibernateException e) {
            LogEvent.logError("Failed to add tests to database: " + e.getMessage(), e);
            return false;
        } finally {
            // Refresh display lists and caches after each successful or failed attempt to
            // ensure data consistency.
            // These operations are safe to call even if adding tests failed.
            testService.refreshTestNames();
            displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.SAMPLE_TYPE_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
            SpringContext.getBean(TypeOfSampleService.class).clearCache();
        }
    }

    /**
     * Helper method to check if a LOINC code is already used.
     * 
     * @param loincCode The LOINC code to check.
     * @return true if the LOINC code is already associated with an existing test,
     *         false otherwise.
     */
    private boolean isLoincAlreadyUsed(String loincCode) {
        if (GenericValidator.isBlankOrNull(loincCode)) {
            return false; // Cannot validate if LOINC code is blank
        }
        List<Test> tests = testService.getTestsByLoincCode(loincCode);
        for (Test test : tests) {
            if (test.getLoinc() != null && test.getLoinc().equals(loincCode)) {
                return true;
            }
        }
        return false;
    }

    // --- Copied Helper Methods from TestAddRestController (made private here) ---
    // These now use the local nested classes directly or are fully qualified if
    // still referring to TestAddController's inner classes

    private Localization createNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testNameEnglish,
                testAddParams.testNameFrench, LocalizationServiceImpl.LocalizationType.TEST_NAME);
    }

    private Localization createReportingNameLocalization(TestAddParams testAddParams) {
        return LocalizationServiceImpl.createNewLocalization(testAddParams.testReportNameEnglish,
                testAddParams.testReportNameFrench, LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME);
    }

    private List<TestAddController.TestSet> createTestSets(TestAddParams testAddParams) {
        Double lowValid = null;
        Double highValid = null;
        Double lowReportingRange = null;
        Double highReportingRange = null;
        Double lowCritical = null;
        Double highCritical = null;
        String significantDigits = testAddParams.significantDigits;
        boolean numericResults = TypeOfTestResultServiceImpl.ResultType.isNumericById(testAddParams.resultTypeId);
        boolean dictionaryResults = TypeOfTestResultServiceImpl.ResultType
                .isDictionaryVarientById(testAddParams.resultTypeId);
        List<TestAddController.TestSet> testSets = new ArrayList<>();
        UnitOfMeasure uom = null;
        if (!GenericValidator.isBlankOrNull(testAddParams.uomId) && !"0".equals(testAddParams.uomId)) {
            uom = unitOfMeasureService.getUnitOfMeasureById(testAddParams.uomId);
        }
        TestSection testSection = testSectionService.get(testAddParams.testSectionId);

        if (numericResults) {
            // Ensure that StringUtil.doubleWithInfinity results are explicitly handled if
            // null
            // before being passed to a setter that might auto-unbox.
            lowValid = parseNumericValue(testAddParams.lowValid, Double.NEGATIVE_INFINITY);
            highValid = parseNumericValue(testAddParams.highValid, Double.POSITIVE_INFINITY);
            lowReportingRange = parseNumericValue(testAddParams.lowReportingRange, Double.NEGATIVE_INFINITY);
            highReportingRange = parseNumericValue(testAddParams.highReportingRange, Double.POSITIVE_INFINITY);
            lowCritical = parseNumericValue(testAddParams.lowCritical, Double.NEGATIVE_INFINITY);
            highCritical = parseNumericValue(testAddParams.highCritical, Double.POSITIVE_INFINITY);
        }
        // The number of test sets depend on the number of sampleTypes
        for (int i = 0; i < testAddParams.sampleList.size(); i++) {
            TypeOfSample typeOfSample = typeOfSampleService
                    .getTypeOfSampleById(testAddParams.sampleList.get(i).sampleTypeId);
            if (typeOfSample == null) {
                continue;
            } else {
                typeOfSample.setActive("Y".equals(testAddParams.active));
            }
            TestAddController.TestSet testSet = testAddController.new TestSet(); // Accessing inner class via instance
            testSet.typeOfSample = typeOfSample;
            Test test = new Test();
            test.setUnitOfMeasure(uom);
            test.setLoinc(testAddParams.loinc);
            test.setDescription(testAddParams.testNameEnglish + "(" + typeOfSample.getDescription() + ")");
            test.setName(testAddParams.testNameEnglish);
            test.setLocalCode(testAddParams.testNameEnglish);
            test.setIsActive(testAddParams.active);
            test.setOrderable("Y".equals(testAddParams.orderable));
            test.setNotifyResults("Y".equals(testAddParams.notifyResults));
            test.setInLabOnly("Y".equals(testAddParams.inLabOnly));
            test.setAntimicrobialResistance("Y".equals(testAddParams.antimicrobialResistance));
            test.setIsReportable("N"); // Default to Not Reportable unless specified otherwise
            test.setTestSection(testSection);
            test.setGuid(String.valueOf(UUID.randomUUID()));
            ArrayList<String> orderedTests = testAddParams.sampleList.get(i).orderedTests;
            for (int j = 0; j < orderedTests.size(); j++) {
                if ("0".equals(orderedTests.get(j))) {
                    test.setSortOrder(String.valueOf(j));
                } else {
                    Test orderedTest = SpringContext.getBean(TestService.class).get(orderedTests.get(j));
                    if (orderedTest != null) { // Ensure orderedTest is not null
                        orderedTest.setSortOrder(String.valueOf(j));
                        testSet.sortedTests.add(orderedTest);
                    }
                }
            }

            testSet.test = test;

            TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
            typeOfSampleTest.setTypeOfSampleId(typeOfSample.getId());
            testSet.sampleTypeTest = typeOfSampleTest;

            createPanelItems(testSet.panelItems, testAddParams);
            createTestResults(testSet.testResults, significantDigits, testAddParams);
            if (numericResults) {
                testSet.resultLimits = createResultLimits(lowValid, highValid, lowReportingRange, highReportingRange,
                        testAddParams, highCritical, lowCritical);
            } else if (dictionaryResults) {
                testSet.resultLimits = createDictionaryResultLimit(testAddParams);
            }

            testSets.add(testSet);
        }

        return testSets;
    }

    /**
     * Helper to parse a numeric string, providing a default if parsing results in
     * null.
     */
    private Double parseNumericValue(String value, Double defaultValue) {
        Double parsed = StringUtil.doubleWithInfinity(value);
        return parsed != null ? parsed : defaultValue;
    }

    private ArrayList<ResultLimit> createDictionaryResultLimit(TestAddParams testAddParams) {
        ArrayList<ResultLimit> resultLimits = new ArrayList<>();
        if (!GenericValidator.isBlankOrNull(testAddParams.dictionaryReferenceId)) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setDictionaryNormalId(testAddParams.dictionaryReferenceId);
            resultLimits.add(limit);
        }

        return resultLimits;
    }

    private ArrayList<ResultLimit> createResultLimits(Double lowValid, Double highValid, Double lowReportingRange,
            Double highReportingRange, TestAddParams testAddParams, Double highCritical, Double lowCritical) {
        ArrayList<ResultLimit> resultLimits = new ArrayList<>();
        for (ResultLimitParams params : testAddParams.limits) { // Use local nested class
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setGender(params.gender);
            limit.setMinAge(StringUtil.doubleWithInfinity(params.lowAge));
            limit.setMaxAge(StringUtil.doubleWithInfinity(params.highAge));
            // FIX: Explicitly convert Double objects to primitive double or provide default
            // if null
            // This prevents NullPointerException during auto-unboxing if the ResultLimit
            // setters expect primitive double.
            limit.setLowNormal(parseNumericValue(params.lowNormalLimit, Double.NEGATIVE_INFINITY));
            limit.setHighNormal(parseNumericValue(params.highNormalLimit, Double.POSITIVE_INFINITY));

            // These are already Double objects passed as parameters from createTestSets.
            // If ResultLimit setters for these expect primitive doubles, then they need
            // to be handled like lowNormal/highNormal above.
            // Assuming ResultLimit.setLowValid and setHighValid expect Double,
            // or if they auto-unbox, they should be robust.
            // However, the error indicated lowValid was null, so we must be sure.
            // Let's apply the same parseNumericValue for safety.
            limit.setLowValid(
                    parseNumericValue(lowValid != null ? String.valueOf(lowValid) : null, Double.NEGATIVE_INFINITY));
            limit.setHighValid(
                    parseNumericValue(highValid != null ? String.valueOf(highValid) : null, Double.POSITIVE_INFINITY));

            if (lowCritical != null && highCritical != null) {
                limit.setLowReportingRange(
                        parseNumericValue(lowReportingRange != null ? String.valueOf(lowReportingRange) : null,
                                Double.NEGATIVE_INFINITY));
                limit.setHighReportingRange(
                        parseNumericValue(highReportingRange != null ? String.valueOf(highReportingRange) : null,
                                Double.POSITIVE_INFINITY));
                limit.setLowCritical(parseNumericValue(lowCritical != null ? String.valueOf(lowCritical) : null,
                        Double.NEGATIVE_INFINITY));
                limit.setHighCritical(parseNumericValue(highCritical != null ? String.valueOf(highCritical) : null,
                        Double.POSITIVE_INFINITY));
            }
            resultLimits.add(limit);
        }

        return resultLimits;
    }

    private void createPanelItems(ArrayList<PanelItem> panelItems, TestAddParams testAddParams) {
        for (String panelId : testAddParams.panelList) {
            PanelItem panelItem = new PanelItem();
            panelItem.setPanel(panelService.getPanelById(panelId));
            panelItems.add(panelItem);
        }
    }

    private void createTestResults(ArrayList<TestResult> testResults, String significantDigits,
            TestAddParams testAddParams) {
        TypeOfTestResultServiceImpl.ResultType type = SpringContext.getBean(TypeOfTestResultService.class)
                .getResultTypeById(testAddParams.resultTypeId);

        if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(type)
                || TypeOfTestResultServiceImpl.ResultType.isNumeric(type)) {
            TestResult testResult = new TestResult();
            testResult.setTestResultType(type.getCharacterValue());
            testResult.setSortOrder("1");
            testResult.setIsActive(true);
            testResult.setSignificantDigits(significantDigits);
            testResults.add(testResult);
        } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type.getCharacterValue())) {
            int sortOrder = 10;
            for (DictionaryParams params : testAddParams.dictionaryParamList) { // Use local nested class
                TestResult testResult = new TestResult();
                testResult.setTestResultType(type.getCharacterValue());
                testResult.setSortOrder(String.valueOf(sortOrder));
                sortOrder += 10;
                testResult.setIsActive(true);
                testResult.setValue(params.dictionaryId);
                testResult.setDefault(params.isDefault);
                testResult.setIsQuantifiable(params.isQuantifiable);
                testResults.add(testResult);
            }
        }
    }

    // Copied TestAddParams and its inner classes as static nested classes for
    // direct use
    public static class TestAddParams {
        public String testId;
        public String testNameEnglish;
        public String testNameFrench;
        public String testReportNameEnglish;
        public String testReportNameFrench;
        public String testSectionId;
        public ArrayList<String> panelList = new ArrayList<>();
        public String uomId;
        public String loinc;
        public String resultTypeId;
        public ArrayList<SampleTypeListAndTestOrder> sampleList = new ArrayList<>();
        public String active;
        public String orderable;
        public String notifyResults;
        public String inLabOnly;
        public String antimicrobialResistance;
        public String lowValid;
        public String highValid;
        public String lowReportingRange;
        public String highReportingRange;
        public String lowCritical;
        public String highCritical;
        public String significantDigits;
        public String dictionaryReferenceId;
        public ArrayList<ResultLimitParams> limits = new ArrayList<>();
        public ArrayList<DictionaryParams> dictionaryParamList = new ArrayList<>();

        public String getTestId() {
            return testId;
        }

        public String getTestNameEnglish() {
            return testNameEnglish;
        }

        public String getTestNameFrench() {
            return testNameFrench;
        }

        public String getTestReportNameEnglish() {
            return testReportNameEnglish;
        }

        public String getTestReportNameFrench() {
            return testReportNameFrench;
        }

        public String getTestSectionId() {
            return testSectionId;
        }

        public ArrayList<String> getPanelList() {
            return panelList;
        }

        public String getUomId() {
            return uomId;
        }

        public String getLoinc() {
            return loinc;
        }

        public String getResultTypeId() {
            return resultTypeId;
        }

        public ArrayList<SampleTypeListAndTestOrder> getSampleList() {
            return sampleList;
        }

        public String getActive() {
            return active;
        }

        public String getOrderable() {
            return orderable;
        }

        public String getNotifyResults() {
            return notifyResults;
        }

        public String getInLabOnly() {
            return inLabOnly;
        }

        public String getAntimicrobialResistance() {
            return antimicrobialResistance;
        }

        public String getLowValid() {
            return lowValid;
        }

        public String getHighValid() {
            return highValid;
        }

        public String getLowReportingRange() {
            return lowReportingRange;
        }

        public String getHighReportingRange() {
            return highReportingRange;
        }

        public String getLowCritical() {
            return lowCritical;
        }

        public String getHighCritical() {
            return highCritical;
        }

        public String getSignificantDigits() {
            return significantDigits;
        }

        public String getDictionaryReferenceId() {
            return dictionaryReferenceId;
        }

        public ArrayList<ResultLimitParams> getLimits() {
            return limits;
        }

        public ArrayList<DictionaryParams> getDictionaryParamList() {
            return dictionaryParamList;
        }

        public String getParentTestId() {
            return testId;
        } // For panel components
    }

    public static class SampleTypeListAndTestOrder {
        public String sampleTypeId;
        public ArrayList<String> orderedTests = new ArrayList<>();
    }

    public static class ResultLimitParams {
        public String gender;
        public String lowAge;
        public String highAge;
        public String lowNormalLimit;
        public String highNormalLimit;
        public String displayRange;
        public String lowCritical;
        public String highCritical;
    }

    public static class DictionaryParams {
        public boolean isDefault;
        public String dictionaryId;
        public boolean isQuantifiable = false;
    }

    // Renamed from extractTestAddParms to avoid conflict/confusion with the
    // original TestAddRestController's method
    private TestAddParams extractTestAddParms(JSONObject obj, JSONParser parser) {
        TestAddParams testAddParams = new TestAddParams();
        try {

            testAddParams.testNameEnglish = (String) obj.get("testNameEnglish");
            testAddParams.testNameFrench = (String) obj.get("testNameFrench");
            testAddParams.testReportNameEnglish = (String) obj.get("testReportNameEnglish");
            testAddParams.testReportNameFrench = (String) obj.get("testReportNameFrench");
            testAddParams.testSectionId = (String) obj.get("testSection");
            testAddParams.dictionaryReferenceId = (String) obj.get("dictionaryReference");

            // Pass the JSONObject 'obj' directly to extractPanels
            // which will now handle panels as a JSONArray directly.
            extractPanels(obj, testAddParams); // Removed 'parser' as it's no longer needed for panels

            testAddParams.uomId = (String) obj.get("uom");
            testAddParams.loinc = (String) obj.get("loinc");
            testAddParams.resultTypeId = (String) obj.get("resultType");
            extractSampleTypes(obj, parser, testAddParams);
            testAddParams.active = (String) obj.get("active");
            testAddParams.orderable = (String) obj.get("orderable");
            testAddParams.notifyResults = (String) obj.get("notifyResults");
            testAddParams.inLabOnly = (String) obj.get("inLabOnly");
            testAddParams.antimicrobialResistance = (String) obj.get("antimicrobialResistance");
            if (TypeOfTestResultServiceImpl.ResultType.isNumericById(testAddParams.resultTypeId)) {
                testAddParams.lowValid = (String) obj.get("lowValid");
                testAddParams.highValid = (String) obj.get("highValid");
                testAddParams.lowReportingRange = (String) obj.get("lowReportingRange");
                testAddParams.highReportingRange = (String) obj.get("highReportingRange");
                testAddParams.lowCritical = (String) obj.get("lowCritical");
                testAddParams.highCritical = (String) obj.get("highCritical");
                testAddParams.significantDigits = (String) obj.get("significantDigits");
                extractLimits(obj, parser, testAddParams);
            } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(testAddParams.resultTypeId)) {
                String dictionary = (String) obj.get("dictionary");
                if (dictionary != null) { // Add null check for safety
                    JSONArray dictionaryArray = (JSONArray) parser.parse(dictionary);
                    for (int i = 0; i < dictionaryArray.size(); i++) {
                        DictionaryParams params = new DictionaryParams();
                        params.dictionaryId = (String) ((JSONObject) dictionaryArray.get(i)).get("value");
                        params.isQuantifiable = "Y".equals(((JSONObject) dictionaryArray.get(i)).get("qualified"));
                        params.isDefault = params.dictionaryId.equals(obj.get("defaultTestResult"));
                        testAddParams.dictionaryParamList.add(params);
                    }
                }
            }

        } catch (ParseException e) {
            LogEvent.logDebug(e);
        }

        return testAddParams;
    }

    // Modified extractPanels method to accept JSONObject directly and handle
    // JSONArray
    private void extractPanels(JSONObject obj, TestAddParams testAddParams) { // Removed 'parser' parameter
        // Directly retrieve "panels" as a JSONArray.
        // It's already a JSON array from OclToOpenElisMapper.
        JSONArray panelArray = (JSONArray) obj.get("panels");

        if (panelArray != null) { // Always check for null before iterating
            for (int i = 0; i < panelArray.size(); i++) {
                // Ensure the element is a JSONObject before casting and accessing its 'id'
                Object panelElement = panelArray.get(i);
                if (panelElement instanceof JSONObject) {
                    testAddParams.panelList.add((String) ((JSONObject) panelElement).get("id"));
                } else {
                    // FIX: Concatenate message and class name to avoid String-to-Throwable error.
                    LogEvent.logWarn(this.getClass().getName(), "extractPanels",
                            "OCL Import: Unexpected type in panels array: "
                                    + (panelElement != null ? panelElement.getClass().getName() : "null"));
                }
            }
        }
    }

    private void extractLimits(JSONObject obj, JSONParser parser, TestAddParams testAddParams) throws ParseException {
        String lowAge = "0";
        String limits = (String) obj.get("resultLimits");
        JSONArray limitArray = (JSONArray) parser.parse(limits);
        for (int i = 0; i < limitArray.size(); i++) {
            ResultLimitParams params = new ResultLimitParams(); // Use local nested class
            Boolean gender = (Boolean) ((JSONObject) limitArray.get(i)).get("gender");
            if (gender != null && gender) { // Check for null before unboxing
                params.gender = "M";
            }
            String highAge = (String) (((JSONObject) limitArray.get(i)).get("highAgeRange"));
            params.displayRange = (String) (((JSONObject) limitArray.get(i)).get("reportingRange"));
            params.lowNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("lowNormal"));
            params.highNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("highNormal"));
            params.lowCritical = (String) (((JSONObject) limitArray.get(i)).get("lowCritical"));
            params.highCritical = (String) (((JSONObject) limitArray.get(i)).get("highCritical"));
            params.lowAge = lowAge;
            params.highAge = highAge;
            testAddParams.limits.add(params);

            if (gender != null && gender) { // If gender is true (meaning male), also add female entry
                params = new ResultLimitParams(); // New instance for female
                params.gender = "F";
                params.lowNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("lowNormalFemale"));
                params.highNormalLimit = (String) (((JSONObject) limitArray.get(i)).get("highNormalFemale"));
                params.lowAge = lowAge;
                params.highAge = highAge;
                testAddParams.limits.add(params);
            }

            lowAge = highAge;
        }
    }

    private void extractSampleTypes(JSONObject obj, JSONParser parser, TestAddParams testAddParams)
            throws ParseException {
        // FIX: Directly retrieve "sampleTypes" as a JSONArray.
        // It's already a JSON array from OclToOpenElisMapper.
        JSONArray sampleTypeArray = (JSONArray) obj.get("sampleTypes");

        if (sampleTypeArray != null) { // Add null check for safety
            for (int i = 0; i < sampleTypeArray.size(); i++) {
                // Ensure the element is a JSONObject before casting and accessing its 'typeId'
                Object sampleTypeElement = sampleTypeArray.get(i);
                if (sampleTypeElement instanceof JSONObject) {
                    SampleTypeListAndTestOrder sampleTypeTests = new SampleTypeListAndTestOrder(); // Use local nested
                                                                                                   // class
                    sampleTypeTests.sampleTypeId = (String) ((JSONObject) sampleTypeElement).get("typeId");

                    JSONArray testArray = (JSONArray) (((JSONObject) sampleTypeElement).get("tests"));
                    if (testArray != null) { // Add null check for safety
                        for (int j = 0; j < testArray.size(); j++) {
                            // Ensure the element is a JSONObject before casting and accessing its 'id'
                            Object testElement = testArray.get(j);
                            if (testElement instanceof JSONObject) {
                                sampleTypeTests.orderedTests.add(String.valueOf(((JSONObject) testElement).get("id")));
                            } else {
                                LogEvent.logWarn(this.getClass().getName(), "extractSampleTypes",
                                        "OCL Import: Unexpected type in tests array within sampleType: "
                                                + (testElement != null ? testElement.getClass().getName() : "null"));
                            }
                        }
                    }
                    testAddParams.sampleList.add(sampleTypeTests);
                } else {
                    LogEvent.logWarn(this.getClass().getName(), "extractSampleTypes",
                            "OCL Import: Unexpected type in sampleTypes array: "
                                    + (sampleTypeElement != null ? sampleTypeElement.getClass().getName() : "null"));
                }
            }
        }
    }

    /**
     * Check if OCL files have already been processed by comparing checksums
     */
    private boolean hasAlreadyProcessedOclFiles() {
        try {
            String currentChecksum = calculateOclFilesChecksum();
            if (currentChecksum == null) {
                return false; // No files to process
            }
            
            Set<String> processedChecksums = loadProcessedChecksums();
            return processedChecksums.contains(currentChecksum);
        } catch (Exception e) {
            log.warn("Error checking OCL file checksums: " + e.getMessage());
            return false; // Continue processing if checksum check fails
        }
    }

    /**
     * Save the current OCL files checksum to the processed list
     */
    private void saveCurrentOclChecksum() {
        try {
            String currentChecksum = calculateOclFilesChecksum();
            if (currentChecksum != null) {
                Files.write(Paths.get(CHECKSUM_FILE), 
                           (currentChecksum + "\n").getBytes(), 
                           StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                log.info("OCL Import: Saved checksum {} to prevent duplicate processing", currentChecksum);
            }
        } catch (Exception e) {
            log.warn("Error saving OCL checksum: " + e.getMessage());
        }
    }

    /**
     * Calculate checksum of all OCL files in the configured import directory
     */
    private String calculateOclFilesChecksum() {
        try {
            File importDir = new File(oclImportDirectory);
            
            // Also check alternative locations if primary doesn't exist
            if (!importDir.exists()) {
                // Try volume/configurations/ocl (runtime location)
                importDir = new File("volume/configurations/ocl");
                if (!importDir.exists()) {
                    // Try volume/plugins (legacy location)
                    importDir = new File("volume/plugins");
                    if (!importDir.exists()) {
                        log.warn("OCL Import: No OCL import directory found. Checked: {}, volume/configurations/ocl, volume/plugins", oclImportDirectory);
                        return null;
                    }
                }
            }
            
            log.info("OCL Import: Using import directory: {}", importDir.getAbsolutePath());
            
            StringBuilder allContent = new StringBuilder();
            File[] files = importDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
            
            if (files == null || files.length == 0) {
                log.info("OCL Import: No ZIP files found in directory: {}", importDir.getAbsolutePath());
                return null;
            }
            
            // Sort files for consistent checksum
            java.util.Arrays.sort(files);
            
            log.info("OCL Import: Found {} ZIP file(s) for checksum calculation", files.length);
            for (File file : files) {
                allContent.append(file.getName()).append(":").append(file.length()).append(";");
                log.debug("OCL Import: Including file in checksum: {} (size: {} bytes)", file.getName(), file.length());
            }
            
            // Calculate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(allContent.toString().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            String checksum = hexString.toString();
            log.debug("OCL Import: Calculated checksum: {} for content: {}", checksum, allContent.toString());
            return checksum;
        } catch (Exception e) {
            log.warn("Error calculating OCL files checksum: " + e.getMessage());
            return null;
        }
    }

    /**
     * Load previously processed checksums from file
     */
    private Set<String> loadProcessedChecksums() {
        Set<String> checksums = new HashSet<>();
        try {
            if (Files.exists(Paths.get(CHECKSUM_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(CHECKSUM_FILE));
                checksums.addAll(lines);
            }
        } catch (Exception e) {
            log.warn("Error loading processed checksums: " + e.getMessage());
        }
        return checksums;
    }
}