// Java standard library
package org.openelisglobal.integration.ocl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;

// OpenELIS model and service classes
import org.openelisglobal.testconfiguration.controller.TestAddController.TestSet;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.testconfiguration.service.TestAddService;
import org.openelisglobal.integration.ocl.OclToOpenElisMapper;
import org.openelisglobal.integration.ocl.OclZipImporter;
import com.fasterxml.jackson.databind.JsonNode;

import org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.TestAddParams;
import org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.DictionaryParams;
import org.openelisglobal.testconfiguration.controller.rest.TestAddRestController.ResultLimitParams;

import org.openelisglobal.unitofmeasure.service.UnitOfMeasureService;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultService;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.testconfiguration.controller.TestAddController;

@Component
public class OclImportInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger log = LoggerFactory.getLogger(OclImportInitializer.class);
    @Autowired
    private OclZipImporter oclZipImporter;

    @Autowired
    private TestAddService testAddService; // Persistence service

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // This triggers the import when the Spring context is refreshed
        try {
            List<JsonNode> oclNodes = oclZipImporter.importOclZip();
            log.info("OCL Import: Found {} nodes to process.", oclNodes.size());
            int conceptCount = 0;
            OclToOpenElisMapper mapper = new OclToOpenElisMapper();
            for (JsonNode node : oclNodes) {
                // If the node is a Collection Version, get its concepts array
                if (node.has("concepts") && node.get("concepts").isArray()) {
                    log.info("OCL Import: Node has a concepts array of size {}.", node.get("concepts").size());
                    for (JsonNode concept : node.get("concepts")) {
                        conceptCount++;
                        log.info("OCL Import: Processing concept #{} with id: {}, display_name: {}",
                                conceptCount, concept.get("id"), concept.get("display_name"));

                        List<TestAddParams> paramsList = mapper.mapConceptToTestParams(concept);
                        if (paramsList == null || paramsList.isEmpty()) {
                            log.warn("OCL Import: Skipped concept id {} (no params mapped).", concept.get("id"));
                            continue;
                        }

                        log.info("OCL Import: Mapped concept id {} to {} TestAddParams.", concept.get("id"), paramsList.size());

                        for (TestAddParams params : paramsList) {
                            try {
                                log.info("OCL Import: Saving test: {}", params.testNameEnglish);
                                // Adapted from TestAddRestController
                                // 1. Convert TestAddParams to TestSet(s)
                                List<TestSet> testSets = createTestSets(params);
                                // 2. Create localizations
                                Localization nameLoc = LocalizationServiceImpl.createNewLocalization(
                                    params.testNameEnglish, params.testNameFrench,
                                    LocalizationServiceImpl.LocalizationType.TEST_NAME
                                );
                                Localization reportLoc = LocalizationServiceImpl.createNewLocalization(
                                    params.testReportNameEnglish, params.testReportNameFrench,
                                    LocalizationServiceImpl.LocalizationType.REPORTING_TEST_NAME
                                );
                                // 3. Persist using addTests (use system user)
                                testAddService.addTests(testSets, nameLoc, reportLoc, "system");
                                log.info("Persisted test via addTests: {} ({})", params.testNameEnglish, params.testId);
                            } catch (Exception ex) {
                                log.error("Failed to persist test: {} ({})", params.testNameEnglish, params.testId, ex);
                            }
                        }
                    }
                } else {
                    log.warn("OCL Import: Node does not have a 'concepts' array or it's not an array, skipping node.");
                }
            }
            log.info("OCL Import: Finished processing. Total concepts processed: {}", conceptCount);
        } catch (java.io.IOException e) {
            log.error("OCL Import failed during file processing", e);
        }
        // --- Refresh display lists and clear caches as in TestAddRestController ---
        try {
            DisplayListService displayListService = SpringContext.getBean(DisplayListService.class);
            displayListService.refreshList(DisplayListService.ListType.PANELS_INACTIVE);
            displayListService.refreshList(DisplayListService.ListType.PANELS);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_ACTIVE);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_BY_NAME);
            displayListService.refreshList(DisplayListService.ListType.TEST_SECTION_INACTIVE);
            SpringContext.getBean(TypeOfSampleService.class).clearCache();
        } catch (Exception e) {
            log.error("Failed to refresh display lists or clear cache after OCL import", e);
        }
    }

    // --- Adapted helpers from TestAddRestController ---
    private List<TestSet> createTestSets(TestAddParams testAddParams) {
        Double lowValid = null;
        Double highValid = null;
        Double lowReportingRange = null;
        Double highReportingRange = null;
        Double lowCritical = null;
        Double highCritical = null;
        String significantDigits = testAddParams.significantDigits;
        boolean numericResults = TypeOfTestResultServiceImpl.ResultType.isNumericById(testAddParams.resultTypeId);
        boolean dictionaryResults = TypeOfTestResultServiceImpl.ResultType.isDictionaryVarientById(testAddParams.resultTypeId);
        List<TestSet> testSets = new ArrayList<>();
        UnitOfMeasure uom = null;
        if (testAddParams.uomId != null && !testAddParams.uomId.isEmpty()) {
            UnitOfMeasureService uomService = SpringContext.getBean(UnitOfMeasureService.class);
            UnitOfMeasure uomQuery = new UnitOfMeasure();
            uomQuery.setName(testAddParams.uomId);
            uom = uomService.getUnitOfMeasureByName(uomQuery);
            if (uom != null) {
                testAddParams.uomId = uom.getId(); // Set to the numeric ID for downstream use
            } else {
                log.warn("No UnitOfMeasure found for symbol/name: {}", testAddParams.uomId);
            }
        }
        TestSection testSection = null;
        if (testAddParams.testSectionId != null && !testAddParams.testSectionId.isEmpty()) {
            TestSectionService testSectionService = SpringContext.getBean(TestSectionService.class);
            // The testSectionId from OCL mapping is a name (e.g., "TEST", "PANEL"), not a numeric ID.
            // Use getTestSectionByName to retrieve the TestSection by its name.
            testSection = testSectionService.getTestSectionByName(testAddParams.testSectionId);
            if (testSection != null) {
                testAddParams.testSectionId = testSection.getId(); // Use the actual ID for persistence
            } else {
                log.warn("No TestSection found for name: {}", testAddParams.testSectionId);
            }
        }
        if (numericResults) {
            lowValid = StringUtil.doubleWithInfinity(testAddParams.lowValid);
            highValid = StringUtil.doubleWithInfinity(testAddParams.highValid);
            lowReportingRange = StringUtil.doubleWithInfinity(testAddParams.lowReportingRange);
            highReportingRange = StringUtil.doubleWithInfinity(testAddParams.highReportingRange);
            lowCritical = StringUtil.doubleWithInfinity(testAddParams.lowCritical);
            highCritical = StringUtil.doubleWithInfinity(testAddParams.highCritical);
        }
        for (int i = 0; i < testAddParams.sampleList.size(); i++) {
            TypeOfSample typeOfSample = SpringContext.getBean(TypeOfSampleService.class).getTypeOfSampleById(testAddParams.sampleList.get(i).sampleTypeId);
            if (typeOfSample == null) {
                continue;
            } else {
                typeOfSample.setActive("Y".equals(testAddParams.active));
            }
            TestSet testSet = new TestAddController().new TestSet();
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
            test.setIsReportable("N");
            test.setTestSection(testSection);
            test.setGuid(java.util.UUID.randomUUID().toString());
            List<String> orderedTests = testAddParams.sampleList.get(i).orderedTests;
            for (int j = 0; j < orderedTests.size(); j++) {
                if ("0".equals(orderedTests.get(j))) {
                    test.setSortOrder(String.valueOf(j));
                } else {
                    Test orderedTest = SpringContext.getBean(TestService.class).get(orderedTests.get(j));
                    orderedTest.setSortOrder(String.valueOf(j));
                    testSet.sortedTests.add(orderedTest);
                }
            }
            testSet.test = test;
            TypeOfSampleTest typeOfSampleTest = new TypeOfSampleTest();
            typeOfSampleTest.setTypeOfSampleId(typeOfSample.getId());
            testSet.sampleTypeTest = typeOfSampleTest;
            createPanelItems(testSet.panelItems, testAddParams);
            createTestResults(testSet.testResults, significantDigits, testAddParams);
            if (numericResults) {
                testSet.resultLimits = new ArrayList<>(createResultLimits(lowValid, highValid, lowReportingRange, highReportingRange, testAddParams, highCritical, lowCritical));
            } else if (dictionaryResults) {
                testSet.resultLimits = new ArrayList<>(createDictionaryResultLimit(testAddParams));
            }
            testSets.add(testSet);
        }
        return testSets;
    }

    private void createPanelItems(List<PanelItem> panelItems, TestAddParams testAddParams) {
        for (String panelId : testAddParams.panelList) {
            PanelItem panelItem = new PanelItem();
            PanelService panelService = SpringContext.getBean(PanelService.class);
            panelItem.setPanel(panelService.getPanelById(panelId));
            panelItems.add(panelItem);
        }
    }

    private void createTestResults(List<TestResult> testResults, String significantDigits, TestAddParams testAddParams) {
        TypeOfTestResultService typeOfTestResultService = SpringContext.getBean(TypeOfTestResultService.class);
        TypeOfTestResultServiceImpl.ResultType type = typeOfTestResultService.getResultTypeById(testAddParams.resultTypeId);
        if (TypeOfTestResultServiceImpl.ResultType.isTextOnlyVariant(type) || TypeOfTestResultServiceImpl.ResultType.isNumeric(type)) {
            TestResult testResult = new TestResult();
            testResult.setTestResultType(type.getCharacterValue());
            testResult.setSortOrder("1");
            testResult.setIsActive(true);
            testResult.setSignificantDigits(significantDigits);
            testResults.add(testResult);
        } else if (TypeOfTestResultServiceImpl.ResultType.isDictionaryVariant(type.getCharacterValue())) {
            int sortOrder = 10;
            for (DictionaryParams params : testAddParams.dictionaryParamList) {
                TestResult testResult = new TestResult();
                testResult.setTestResultType(type.getCharacterValue());
                testResult.setSortOrder(String.valueOf(sortOrder));
                testResult.setIsActive(true);
                testResult.setValue(params.dictionaryId);
                testResult.setDefault(params.isDefault);
                testResult.setIsQuantifiable(params.isQuantifiable);
                testResults.add(testResult);
                sortOrder += 10;
            }
        }
    }

    private List<ResultLimit> createResultLimits(Double lowValid, Double highValid, Double lowReportingRange, Double highReportingRange, TestAddParams testAddParams, Double highCritical, Double lowCritical) {
        List<ResultLimit> resultLimits = new ArrayList<>();
        for (ResultLimitParams params : testAddParams.limits) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setGender(params.gender);
            limit.setMinAge(StringUtil.doubleWithInfinity(params.lowAge));
            limit.setMaxAge(StringUtil.doubleWithInfinity(params.highAge));
            limit.setLowNormal(StringUtil.doubleWithInfinity(params.lowNormalLimit));
            limit.setHighNormal(StringUtil.doubleWithInfinity(params.highNormalLimit));
            limit.setLowValid(lowValid);
            limit.setHighValid(highValid);
            if (lowCritical != null && highCritical != null) {
                limit.setLowReportingRange(lowReportingRange);
                limit.setHighReportingRange(highReportingRange);
                limit.setLowCritical(lowCritical);
                limit.setHighCritical(highCritical);
            }
            resultLimits.add(limit);
        }
        return resultLimits;
    }

    private List<ResultLimit> createDictionaryResultLimit(TestAddParams testAddParams) {
        List<ResultLimit> resultLimits = new ArrayList<>();
        if (testAddParams.dictionaryReferenceId != null && !testAddParams.dictionaryReferenceId.isEmpty()) {
            ResultLimit limit = new ResultLimit();
            limit.setResultTypeId(testAddParams.resultTypeId);
            limit.setDictionaryNormalId(testAddParams.dictionaryReferenceId);
            resultLimits.add(limit);
        }
        return resultLimits;
    }
}
