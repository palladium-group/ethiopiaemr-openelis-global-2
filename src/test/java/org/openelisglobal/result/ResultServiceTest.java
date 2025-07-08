package org.openelisglobal.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
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
}
