package org.openelisglobal.analyzerresults;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.result.controller.AnalyzerResultsController;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalyzerResultsServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    private List<AnalyzerResults> analyzerResultsList;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analyzer-results.xml");

        propertyValues = new HashMap<>();
        propertyValues.put("completeDate", Timestamp.valueOf("2025-07-01 09:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("accessionNumber");
    }

    @Test
    public void readAnalyzerResults_ShouldReturnAnalyzerResults_UsingAnId() {
        AnalyzerResults analyzerResults = analyzerResultsService.readAnalyzerResults("1002");
        assertNotNull(analyzerResults);
        assertEquals(Timestamp.valueOf("2025-07-01 09:15:00"), analyzerResults.getCompleteDate());
        assertTrue(analyzerResults.isReadOnly());
        assertFalse(analyzerResults.getIsControl());
    }

    @Test
    public void getResultsByAnalyzer_ShouldReturnAnalyzerResults_UsingAnAnalyzerId() {
        analyzerResultsList = analyzerResultsService.getResultsbyAnalyzer("2001");
        assertNotNull(analyzerResultsList);
        assertEquals("ACC123456", analyzerResultsList.get(0).getAccessionNumber());
        assertFalse(analyzerResultsList.get(0).isReadOnly());
    }

    @Test
    public void insertAnalyzerResults_ShouldInsertAListOfAnalyzerResults() {
        List<AnalyzerResults> analyzerResultsList = analyzerResultsService.getAll();
        assertFalse(analyzerResultsList.isEmpty());
        analyzerResultsService.deleteAll(analyzerResultsList);
        List<AnalyzerResults> newAnalyzerResultsList = analyzerResultsService.getAll();
        assertTrue(newAnalyzerResultsList.isEmpty());
        AnalyzerResults analyzerResults = new AnalyzerResults();
        analyzerResults.setAnalyzerId("2001");
        analyzerResults.setTestName("Body mass");
        analyzerResults.setAccessionNumber("QAN23L");
        analyzerResults.setResult("278");
        analyzerResults.setIsControl(false);
        List<AnalyzerResults> insertAnalyzerResults = new ArrayList<>();
        insertAnalyzerResults.add(analyzerResults);
        analyzerResultsService.insertAnalyzerResults(insertAnalyzerResults, "1006");
        assertFalse(insertAnalyzerResults.isEmpty());
        assertEquals(1, insertAnalyzerResults.size());
    }

    @Test
    public void persistAnalyzerResults_ShouldPersistAListOfAnalyzerResults() {
        List<AnalyzerResults> deletableAnalyzerResults = new ArrayList<>();
        AnalyzerResults analyzerResults = analyzerResultsService.get("1002");
        deletableAnalyzerResults.add(analyzerResults);
        List<AnalyzerResultsController.SampleGrouping> sampleGroupList = new ArrayList<>();

        analyzerResultsService.persistAnalyzerResults(deletableAnalyzerResults, sampleGroupList, "1006");

    }

    @Test
    public void getAll_ShouldReturnAllAnalyzerResults() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertNotNull(analyzerResultsList);
        assertEquals(3, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(2).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingAnalyzerResults_UsingPropertyNameAndValue() {
        analyzerResultsList = analyzerResultsService.getAllMatching("analyzerId", "2001");
        assertNotNull(analyzerResultsList);
        assertEquals(1, analyzerResultsList.size());
        assertEquals("1001", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnAllMatchingAnalyzerResults_UsingAMap() {
        analyzerResultsList = analyzerResultsService.getAllMatching(propertyValues);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedAnalyzerResults_UsingAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllOrdered("accessionNumber", false);
        assertNotNull(analyzerResultsList);
        assertEquals(3, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(2).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrdered_UsingAList() {
        analyzerResultsList = analyzerResultsService.getAllOrdered(orderProperties, true);
        assertNotNull(analyzerResultsList);
        assertEquals(3, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "2002", "lastupdated", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "2002", orderProperties, true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1002", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, "result", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1003", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfAnalyzerResults_UsingAPageNumber() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getPage(1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfAnalyzerResults_UsingAPropertyNameAndValue() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingPage("analyzerId", "1001", 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfAnalyzerResults_UsingAMap() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingPage(propertyValues, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfAnalyzerResults_UsingAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getOrderedPage("lastupdated", true, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAnOrderedPageOfAnalyzerResults_UsingAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getOrderedPage(orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAPropertyNameAndValueAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage("analyzerId", "1002", "lastupdated", true,
                1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAPropertyNameAndValueAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage("analyzerId", "1002", orderProperties, true,
                1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAMapAndAnOrderProperty() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage(propertyValues, "analyzerId", false, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAMatchingOrderedPageOfAnalyzerResults_UsingAMapAndAList() {
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        analyzerResultsList = analyzerResultsService.getMatchingOrderedPage(propertyValues, orderProperties, false, 1);
        assertTrue(NUMBER_OF_PAGES >= analyzerResultsList.size());
    }

    @Test
    public void delete_ShouldDeleteAnAnalyzerResult() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertEquals(3, analyzerResultsList.size());
        AnalyzerResults analyzerResults = analyzerResultsService.get("1002");
        analyzerResultsService.delete(analyzerResults);
        List<AnalyzerResults> newAnalyzerResultsList = analyzerResultsService.getAll();
        assertEquals(2, newAnalyzerResultsList.size());
    }

    @Test
    public void deleteAll_ShouldDeleteAllAnalyzerResults() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertEquals(3, analyzerResultsList.size());
        analyzerResultsService.deleteAll(analyzerResultsList);
        List<AnalyzerResults> updatedAnalyzerResultsList = analyzerResultsService.getAll();
        assertTrue(updatedAnalyzerResultsList.isEmpty());
    }
}
