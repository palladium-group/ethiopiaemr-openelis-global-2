package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Autowired;

public class ResultServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ResultService resultService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TestAnalyteService testAnalyteService;
    @Autowired
    private TestResultService testResultService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private AnalyteService analyteService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private PanelService panelService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/result.xml");

    }

    @Test
    public void getAll_shouldReturnAllResults() {
        List<Result> results = resultService.getAll();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());

    }

    @Test
    public void getData_shouldReturnResultData() {
        Result result = resultService.get("3");
        resultService.getData(result);
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("85.0", result.getValue());

    }

    @Test
    public void getResultByAnalysis_shouldReturnResultsForAnalysis() {
        Analysis analysis = analysisService.get("1");
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultByAnalysisAndAnalyte_shouldReturnResultForAnalysisAndAnalyte() {
        Result result = resultService.get("3");
        Analysis analysis = analysisService.get("1");
        TestAnalyte testAnalyte = testAnalyteService.get("1");
        resultService.getResultByAnalysisAndAnalyte(result, analysis, testAnalyte);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getResultByTestResult_shouldReturnResultForTestResult() {
        Result result = resultService.get("3");
        TestResult testResult = testResultService.get("1");
        resultService.getResultByTestResult(result, testResult);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getResultsForSample_shouldReturnResultsForSample() {
        Sample sample = sampleService.get("1");
        List<Result> results = resultService.getResultsForSample(sample);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getUOM_shouldReturnUnitOfMeasure() {
        Result result = resultService.get("3");
        String uom = resultService.getUOM(result);
        assertNotNull(uom);
        assertEquals("mg/dL", uom);
    }

    @Test
    public void getResultById_shouldReturnResultById() {
        Result result = resultService.getResultById("3");
        assertNotNull(result);
        assertEquals("3", result.getId());
        assertEquals("85.0", result.getValue());
    }

    @Test
    public void getAllResults_shouldReturnAllResults() {
        List<Result> results = resultService.getAllResults();
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getResultById_givenResult_shouldReturnResult() {
        Result result = new Result();
        result.setId("3");
        Result fetchedResult = resultService.getResultById(result);
        assertNotNull(fetchedResult);
        assertEquals("3", fetchedResult.getId());
        assertEquals("85.0", fetchedResult.getValue());
    }

    @Test
    public void getSignature_shouldReturnSignature() {
        Result result = resultService.get("4");
        String signature = resultService.getSignature(result);
        assertNotNull(signature);
        assertEquals("External Doctor", signature);
    }

    @Test
    public void getLastUpdatedTime_shouldReturnLastUpdatedTime() {
        Result result = resultService.get("4");
        String lastUpdatedTime = resultService.getLastUpdatedTime(result);
        assertNotNull(lastUpdatedTime);
        assertEquals("07/07/2025", lastUpdatedTime);
    }

    @Test
    public void getTestType_shouldReturnTestType() {
        Result result = resultService.get("3");
        String testType = resultService.getTestType(result);
        assertNotNull(testType);
        assertEquals("N", testType);
    }

    @Test
    public void getTestTime_shouldReturnTestTime() {
        Result result = resultService.get("3");
        String testTime = resultService.getTestTime(result);
        assertNotNull(testTime);
        assertEquals("07/07/2025", testTime);
    }

    @Test
    public void getLOINCCode_shouldReturnLOINCCode() {
        Result result = resultService.get("3");
        String loincCode = resultService.getLOINCCode(result);
        assertNotNull(loincCode);
        assertEquals("123456", loincCode);
    }

    @Test
    public void getTestDescription_shouldReturnTestDescription() {
        Result result = resultService.get("3");
        String testDescription = resultService.getTestDescription(result);
        assertNotNull(testDescription);
        assertEquals("GPT/ALAT(Serum)", testDescription);
    }

    @Test
    public void getResultForAnalyteAndSampleItem_shouldReturnResultForAnalyteAndSampleItem() {
        String sampleItem = sampleItemService.get("601").getId();
        String analyte = analyteService.get("3").getId();
        Result result = resultService.getResultForAnalyteAndSampleItem(analyte, sampleItem);
        assertNotNull(result);
        assertEquals("3", result.getId());

    }

    @Test
    public void getResultsForTestAndSample_shouldReturnResultsForTestAndSample() {
        String sampleId = testService.get("1").getId();
        String testId = sampleService.get("1").getId();
        List<Result> results = resultService.getResultsForTestAndSample(sampleId, testId);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getReportableResultsByAnalysis_shouldReturnReportableResults() {
        Analysis analysis = analysisService.get("1");
        List<Result> results = resultService.getReportableResultsByAnalysis(analysis);
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForAnalysisIdList_shouldReturnResultsForAnalysisIdList() {
        List<Integer> analysisIdList = List.of(1, 2);
        List<Result> results = resultService.getResultsForAnalysisIdList(analysisIdList);
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("3", results.get(0).getId());
        assertEquals("4", results.get(1).getId());
    }

    @Test
    public void getResultForAnalyteInAnalysisSet_shouldReturnResultForAnalyteInAnalysisSet() {
        String analyteId = analyteService.get("3").getId();
        List<Integer> analysisIDList = List.of(1, 2);
        Result result = resultService.getResultForAnalyteInAnalysisSet(analyteId, analysisIDList);
        assertNotNull(result);
        assertEquals("3", result.getId());
    }

    @Test
    public void getChildResults_shouldReturnChildResults() {
        String resultId = resultService.get("3").getId();
        List<Result> childResults = resultService.getChildResults(resultId);
        assertNotNull(childResults);
        assertEquals(2, childResults.size());
        assertEquals("3", childResults.get(0).getId());
        assertEquals("4", childResults.get(1).getId());
    }

    @Test
    public void getPageOfResults_shouldReturnPageOfResults() {
        List<Result> results = resultService.getPageOfResults(1);
        int expectedPages = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(results.size() <= expectedPages);

    }

    @Test
    public void getReportingTestName_shouldReturnReportingTestName() {
        Result result = resultService.get("3");
        String reportingTestName = resultService.getReportingTestName(result);
        assertNotNull(reportingTestName);
        assertEquals("GPT/ALAT", reportingTestName);
    }

    @Test
    public void getResultsForTestSectionInDateRange_shouldReturnResultsForTestSectionInDateRange() {
        String testSectionId = testSectionService.get("1").getId();
        Date lowDate = Date.valueOf("2025-01-01");
        Date highDate = Date.valueOf("2025-12-12");

        List<Result> results = resultService.getResultsForTestSectionInDateRange(testSectionId, lowDate, highDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForPanelInDateRange_shouldReturnResultsForPanelInDateRange() {
        String panelId = panelService.get("1").getId();
        Date lowDate = Date.valueOf("2025-01-01");
        Date highDate = Date.valueOf("2025-12-12");
        List<Result> results = resultService.getResultsForPanelInDateRange(panelId, lowDate, highDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getResultsForTestInDateRange_shouldReturnResultsForTestInDateRange() {
        String testId = testService.get("1").getId();
        Date startDate = Date.valueOf("2025-01-01");
        Date endDate = Date.valueOf("2025-12-12");
        List<Result> results = resultService.getResultsForTestInDateRange(testId, startDate, endDate);
        assertNotNull(results);
        assertTrue(results.size() > 0);
        assertEquals("3", results.get(0).getId());
    }

    @Test
    public void getDisplayReferenceRange_shouldReturnDisplayReferenceRange() {
        Result result = resultService.get("3");
        String displayReferenceRange = resultService.getDisplayReferenceRange(result, false);
        assertNotNull(displayReferenceRange);
        assertEquals("70.0-100.0", displayReferenceRange);
    }

    @Test
    public void getSimpleResultValue_shouldReturnSimpleResultValue() {
        Result result = resultService.get("3");
        String simpleResultValue = resultService.getSimpleResultValue(result);
        assertNotNull(simpleResultValue);
        assertEquals("85.0", simpleResultValue);
    }

    @Test
    public void getResultValurForDisplay_shouldReturnResultValueForDisplay() {
        Result result = resultService.get("3");
        String resultValue = resultService.getResultValue(result, ", ", true, true);
        assertNotNull(resultValue);
        assertEquals("85.0 mg/dL", resultValue);
    }

}
