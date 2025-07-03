package org.openelisglobal.analyzerresults;

import static org.junit.Assert.*;

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
        propertyValues.put("completedDate", Timestamp.valueOf("2025-07-01 09:15:00"));
        orderProperties = new ArrayList<>();
        orderProperties.add("accessionNumber");
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "1002", "lastupdated", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("2", analyzerResultsList.get(1).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingPropertyNameAndValueAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered("analyzerId", "1002", orderProperties, true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("2", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAnOrderProperty() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, "patientTypeId", true);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("2", analyzerResultsList.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnAllMatchingOrderedAnalyzerResults_UsingAMapAndAList() {
        analyzerResultsList = analyzerResultsService.getAllMatchingOrdered(propertyValues, orderProperties, false);
        assertNotNull(analyzerResultsList);
        assertEquals(2, analyzerResultsList.size());
        assertEquals("1", analyzerResultsList.get(0).getId());
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
    public void delete_ShouldDeleteAPatientType() {
        analyzerResultsList = analyzerResultsService.getAll();
        assertEquals(3, analyzerResultsList.size());
        AnalyzerResults analyzerResults = analyzerResultsService.get("2");
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
