package org.openelisglobal.analysisqaeventaction;

import static org.junit.Assert.assertEquals;
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
import org.openelisglobal.analysisqaeventaction.service.AnalysisQaEventActionService;
import org.openelisglobal.analysisqaeventaction.valueholder.AnalysisQaEventAction;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;

public class AnalysisQaEventActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AnalysisQaEventActionService analysisQaEventActionService;

    private List<AnalysisQaEventAction> analysisQaEventActions;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 1;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/analysis-qa-event-action.xml");
        propertyValues = new HashMap<>();
        orderProperties = new ArrayList<>();
    }

    @Test
    public void testGetAllEventActions_ReturnsAllEventActions() {
        analysisQaEventActions = analysisQaEventActionService.getAll();
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void testGetAllMatching_ReturnsMatchingEventActions_UsingPropertyName() {
        analysisQaEventActions = analysisQaEventActionService.getAllMatching("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"));
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void testGetAllMatching_ReturnsMatchingEventActions_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getAllMatching(propertyValues);
        assertNotNull(analysisQaEventActions);
        assertEquals("2", analysisQaEventActions.get(0).getId());
        assertEquals("3", analysisQaEventActions.get(1).getId());
    }

    @Test
    public void testGetAllOrdered_ReturnsAllOrderedEventActions_UsingPropertyName() {
        analysisQaEventActions = analysisQaEventActionService.getAllOrdered("createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("3", analysisQaEventActions.get(2).getId());
    }

    @Test
    public void testGetAllOrdered_ReturnsAllOrderedEventActions_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventActionService.getAllOrdered(orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(3, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_ReturnsMatchingOrderedEventActions_Using() {
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), "createdDate", true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_ReturnsMatchingOrderedEventActions_UsingList() {
        orderProperties.add("createdDate");
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered("createdDate",
                Timestamp.valueOf("2025-06-23 14:15:00"), orderProperties, true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("1", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetAllMatchingOrdered_ReturnsMatchingOrderedEventActions_UsingMap() {
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getAllMatchingOrdered(propertyValues, "lastupdated",
                true);
        assertNotNull(analysisQaEventActions);
        assertEquals(2, analysisQaEventActions.size());
        assertEquals("2", analysisQaEventActions.get(0).getId());
    }

    @Test
    public void testGetPage_ReturnsAPageOfResults_UsingPageNumber() {
        analysisQaEventActions = analysisQaEventActionService.getPage(1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingPage_ReturnsAPageOfResults_UsingPropertyNameAndValue() {
        analysisQaEventActions = analysisQaEventActionService.getMatchingPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingPage_ReturnsAPageOfResults_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getMatchingPage(propertyValues, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetOrderedPage_ReturnsAPageOfResults_UsingOrderProperty() {
        analysisQaEventActions = analysisQaEventActionService.getOrderedPage("lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetOrderedPage_ReturnsAPageOfResults_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getOrderedPage(orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_ReturnsAPageOfResults_UsingPropertyNameAndValue() {
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), "lastupdated", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_ReturnsAPageOfResults_UsingList() {
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_ReturnsAPageOfResults_UsingMap() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage(propertyValues, "createdDate",
                true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void testGetMatchingOrderedPage_ReturnsAPageOfResults_UsingMapAndList() {
        propertyValues.put("createdDate", Timestamp.valueOf("2025-06-22 11:30:00"));
        orderProperties.add("lastupdated");
        analysisQaEventActions = analysisQaEventActionService.getMatchingOrderedPage(propertyValues, orderProperties,
                true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= analysisQaEventActions.size());
    }

    @Test
    public void deleteAnalysisQaEventActions_DeletesAnalysisQaEventActionPassedAsParameter() {
        AnalysisQaEventAction analysisQaEventAction = analysisQaEventActionService.getAll().get(0);
        analysisQaEventActionService.delete(analysisQaEventAction);
        List<AnalysisQaEventAction> deletedAnalysisQaEventAction = analysisQaEventActionService.getAll();
        assertEquals(2, deletedAnalysisQaEventAction.size());
    }

    @Test
    public void deleteAllAnalysisQaEvents_DeletesAllAnalysisQaEvents() {
        analysisQaEventActionService.deleteAll(analysisQaEventActionService.getAll());
        List<AnalysisQaEventAction> delectedAnalysisQaEventAction = analysisQaEventActionService.getAll();
        assertNotNull(delectedAnalysisQaEventAction);
        assertEquals(0, delectedAnalysisQaEventAction.size());
    }
}
