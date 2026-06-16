package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueItemDTO;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueResponse;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.valueholder.TestSection;

@RunWith(MockitoJUnitRunner.class)
public class DashboardQueueMapperTest {

    @Mock
    private PatientService patientService;

    @Mock
    private SampleHumanService sampleHumanService;

    @InjectMocks
    private DashboardQueueMapper dashboardQueueMapper;

    private Patient patient;

    @Before
    public void setUp() {
        Person person = new Person();
        person.setFirstName("Jane");
        person.setLastName("Doe");

        patient = new Patient();
        patient.setId("patient-1");
        patient.setPerson(person);

        when(sampleHumanService.getPatientForSample(any(Sample.class))).thenReturn(patient);
        when(patientService.getSubjectNumber(patient)).thenReturn("334-422-A");
        when(patientService.getNationalId(patient)).thenReturn("ET-123");
    }

    @Test
    public void groupAnalysesByAccession_shouldReturnOneRowPerStandaloneTestOnSameAccession() {
        List<Analysis> analyses = Arrays.asList(createAnalysis("analysis-1", "2026-001234", "Chemistry"),
                createAnalysis("analysis-2", "2026-001234", "Hematology"),
                createAnalysis("analysis-3", "2026-001234", "Serology"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals("Each standalone test should be its own row", 3, queueItems.size());
        assertEquals(3, queueItems.stream().filter(item -> "2026-001234".equals(item.getAccessionNumber())).count());
        assertEquals("Jane Doe", queueItems.get(0).getPatientName());
        assertEquals("334-422-A", queueItems.get(0).getSubjectNumber());
        assertEquals("ET-123", queueItems.get(0).getPatientNationalId());
    }

    @Test
    public void groupAnalysesByAccession_shouldReturnSeparateRowsForDifferentAccessions() {
        List<Analysis> analyses = Arrays.asList(createAnalysis("analysis-1", "2026-001234", "Chemistry"),
                createAnalysis("analysis-2", "2026-005678", "Chemistry"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals(2, queueItems.size());
    }

    @Test
    public void buildQueueResponse_shouldPaginateWithDefaults() {
        DashboardQueueItemDTO item = new DashboardQueueItemDTO();
        item.setAccessionNumber("2026-001234");

        DashboardQueueResponse response = dashboardQueueMapper.buildQueueResponse(Arrays.asList(item));

        assertEquals(DashboardQueueMapper.DEFAULT_PAGE, response.getPage());
        assertEquals(DashboardQueueMapper.DEFAULT_PAGE_SIZE, response.getPageSize());
        assertEquals(1, response.getTotalItems());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getItems().size());
    }

    @Test
    public void buildQueueResponse_shouldReturnRequestedPageSlice() {
        List<DashboardQueueItemDTO> items = Arrays.asList(createQueueItem("2026-001", 3),
                createQueueItem("2026-002", 2), createQueueItem("2026-003", 1));

        DashboardQueueResponse response = dashboardQueueMapper.buildQueueResponse(items, 2, 1);

        assertEquals(2, response.getPage());
        assertEquals(1, response.getPageSize());
        assertEquals(3, response.getTotalItems());
        assertEquals(3, response.getTotalPages());
        assertEquals("2026-002", response.getItems().get(0).getAccessionNumber());
    }

    @Test
    public void sortByOrderDateDesc_shouldPlaceNewestAccessionFirst() {
        List<DashboardQueueItemDTO> items = Arrays.asList(createQueueItem("older", 1), createQueueItem("newer", 3));

        List<DashboardQueueItemDTO> sorted = dashboardQueueMapper.sortByOrderDateDesc(items);

        assertEquals("newer", sorted.get(0).getAccessionNumber());
        assertEquals("older", sorted.get(1).getAccessionNumber());
    }

    @Test
    public void filterQueueItems_shouldMatchPartialPatientName() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001", "Jane Doe", "334-422-A", "ET-123"),
                createSearchableQueueItem("2026-002", "John Smith", "555-111-B", "ET-456"));

        List<DashboardQueueItemDTO> filtered = dashboardQueueMapper.filterQueueItems(items, "doe", null);

        assertEquals(1, filtered.size());
        assertEquals("2026-001", filtered.get(0).getAccessionNumber());
    }

    @Test
    public void filterQueueItems_shouldMatchSubjectNumberOrNationalId() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001", "Jane Doe", "334-422-A", "ET-123"),
                createSearchableQueueItem("2026-002", "John Smith", "555-111-B", "ET-456"));

        assertEquals(1, dashboardQueueMapper.filterQueueItems(items, "334-422", null).size());
        assertEquals(1, dashboardQueueMapper.filterQueueItems(items, "et-456", null).size());
    }

    @Test
    public void filterQueueItems_shouldMatchPartialLabNumber() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001234", "Jane Doe", "334-422-A", "ET-123"),
                createSearchableQueueItem("2026-005678", "John Smith", "555-111-B", "ET-456"));

        List<DashboardQueueItemDTO> filtered = dashboardQueueMapper.filterQueueItems(items, null, "1234");

        assertEquals(1, filtered.size());
        assertEquals("2026-001234", filtered.get(0).getAccessionNumber());
    }

    @Test
    public void filterQueueItemsByTestSection_shouldReturnAllItemsWhenSectionNotProvided() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001234", "Jane Doe", "A", "ET-1"),
                createSearchableQueueItem("2026-005678", "John Smith", "B", "ET-2"));
        items.get(0).setTestSectionId("9001");
        items.get(1).setTestSectionId("9002");

        List<DashboardQueueItemDTO> filtered = dashboardQueueMapper.filterQueueItemsByTestSection(items, null);

        assertEquals(2, filtered.size());
    }

    @Test
    public void filterQueueItemsByTestSection_shouldFilterBySectionId() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001234", "Jane Doe", "A", "ET-1"),
                createSearchableQueueItem("2026-005678", "John Smith", "B", "ET-2"));
        items.get(0).setTestSectionId("9001");
        items.get(1).setTestSectionId("9002");

        List<DashboardQueueItemDTO> filtered = dashboardQueueMapper.filterQueueItemsByTestSection(items, "9002");

        assertEquals(1, filtered.size());
        assertEquals("2026-005678", filtered.get(0).getAccessionNumber());
    }

    @Test
    public void filterQueueItems_shouldApplyPatientAndLabNumberTogether() {
        List<DashboardQueueItemDTO> items = Arrays.asList(
                createSearchableQueueItem("2026-001234", "Jane Doe", "334-422-A", "ET-123"),
                createSearchableQueueItem("2026-001999", "Jane Roe", "334-422-C", "ET-789"));

        List<DashboardQueueItemDTO> filtered = dashboardQueueMapper.filterQueueItems(items, "jane", "1234");

        assertEquals(1, filtered.size());
        assertEquals("2026-001234", filtered.get(0).getAccessionNumber());
    }

    @Test
    public void usesQueueView_shouldIncludePrimaryDashboardTiles() {
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_IN_PROGRESS));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_READY_FOR_VALIDATION));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_COMPLETED_TODAY));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.PENDING_RECEPTION));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_PATIALLY_COMPLETED_TODAY));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_REJECTED_TODAY));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.UN_PRINTED_RESULTS));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.DELAYED_TURN_AROUND));
        assertEquals(true, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_FOR_USER));
        assertEquals(false, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.INCOMING_ORDERS));
        assertEquals(false, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.ORDERS_ENTERED_BY_USER_TODAY));
        assertEquals(false, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.AVERAGE_TURN_AROUND_TIME));
    }

    @Test
    public void groupAnalysesByAccession_shouldReturnSeparateRowsForPanelsAndStandaloneTests() {
        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn("panel-1");
        when(panel.getLocalizedName()).thenReturn("Complete Blood Count");

        List<Analysis> analyses = Arrays.asList(createPanelAnalysis("analysis-1", "2026-001234", "Hemoglobin", panel),
                createPanelAnalysis("analysis-2", "2026-001234", "WBC", panel),
                createAnalysis("analysis-3", "2026-001234", "Glucose"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals(2, queueItems.size());
        DashboardQueueItemDTO panelRow = queueItems.stream()
                .filter(item -> "Complete Blood Count".equals(item.getTestNames())).findFirst().orElse(null);
        DashboardQueueItemDTO glucoseRow = queueItems.stream().filter(item -> "Glucose".equals(item.getTestNames()))
                .findFirst().orElse(null);
        assertNotNull(panelRow);
        assertNotNull(glucoseRow);
        assertEquals(2, panelRow.getTestCount());
        assertEquals(1, glucoseRow.getTestCount());
        assertEquals("2026-001234-panel-panel-1", panelRow.getId());
        assertEquals("analysis-3", glucoseRow.getId());
    }

    @Test
    public void groupAnalysesByAccession_shouldReturnSeparateRowsForTwoPanelsOnSameAccession() {
        Panel cbcPanel = mock(Panel.class);
        when(cbcPanel.getId()).thenReturn("panel-1");
        when(cbcPanel.getLocalizedName()).thenReturn("Complete Blood Count");

        Panel bmpPanel = mock(Panel.class);
        when(bmpPanel.getId()).thenReturn("panel-2");
        when(bmpPanel.getLocalizedName()).thenReturn("Basic Metabolic Panel");

        List<Analysis> analyses = Arrays.asList(
                createPanelAnalysis("analysis-1", "2026-001234", "Hemoglobin", cbcPanel),
                createPanelAnalysis("analysis-2", "2026-001234", "WBC", cbcPanel),
                createPanelAnalysis("analysis-3", "2026-001234", "Sodium", bmpPanel),
                createPanelAnalysis("analysis-4", "2026-001234", "Potassium", bmpPanel));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals(2, queueItems.size());
        assertEquals(1, queueItems.stream().filter(item -> "Complete Blood Count".equals(item.getTestNames())).count());
        assertEquals(1,
                queueItems.stream().filter(item -> "Basic Metabolic Panel".equals(item.getTestNames())).count());
    }

    @Test
    public void groupAnalysesByAccession_shouldCountPanelMembersInPanelRow() {
        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn("panel-1");
        when(panel.getLocalizedName()).thenReturn("Complete Blood Count");

        List<Analysis> analyses = Arrays.asList(createPanelAnalysis("analysis-1", "2026-001234", "Hemoglobin", panel),
                createPanelAnalysis("analysis-2", "2026-001234", "WBC", panel),
                createAnalysis("analysis-3", "2026-005678", "Glucose"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals(2, queueItems.size());
        DashboardQueueItemDTO panelRow = queueItems.stream()
                .filter(item -> "2026-001234".equals(item.getAccessionNumber())).findFirst().orElse(null);
        DashboardQueueItemDTO glucoseRow = queueItems.stream()
                .filter(item -> "2026-005678".equals(item.getAccessionNumber())).findFirst().orElse(null);
        assertNotNull(panelRow);
        assertNotNull(glucoseRow);
        assertEquals(2, panelRow.getTestCount());
        assertEquals(1, glucoseRow.getTestCount());
        assertEquals(2, dashboardQueueMapper.countTestUnitsForAnalyses(analyses));
    }

    @Test
    public void filterAnalysesByTestSection_shouldKeepOnlyMatchingAnalyses() {
        TestSection chemistrySection = mock(TestSection.class);
        when(chemistrySection.getId()).thenReturn("9001");
        TestSection hematologySection = mock(TestSection.class);
        when(hematologySection.getId()).thenReturn("9002");

        Analysis chemistryAnalysis = createAnalysis("analysis-1", "2026-001234", "Glucose");
        chemistryAnalysis.setTestSection(chemistrySection);
        Analysis hematologyAnalysis = createAnalysis("analysis-2", "2026-005678", "Hematology");
        hematologyAnalysis.setTestSection(hematologySection);

        List<Analysis> filtered = dashboardQueueMapper
                .filterAnalysesByTestSection(Arrays.asList(chemistryAnalysis, hematologyAnalysis), "9002");

        assertEquals(1, filtered.size());
        assertEquals("analysis-2", filtered.get(0).getId());
    }

    @Test
    public void filterAnalysesByTestSection_beforeGrouping_shouldExcludeOtherSectionRows() {
        TestSection hematologySection = mock(TestSection.class);
        when(hematologySection.getId()).thenReturn("9002");
        TestSection chemistrySection = mock(TestSection.class);
        when(chemistrySection.getId()).thenReturn("9001");

        Analysis hematologyAnalysis = createAnalysis("analysis-1", "2026-001234", "Hemoglobin");
        hematologyAnalysis.setTestSection(hematologySection);
        Analysis chemistryAnalysis = createAnalysis("analysis-2", "2026-001234", "Glucose");
        chemistryAnalysis.setTestSection(chemistrySection);

        List<Analysis> chemistryOnly = dashboardQueueMapper
                .filterAnalysesByTestSection(Arrays.asList(hematologyAnalysis, chemistryAnalysis), "9001");
        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(chemistryOnly);

        assertEquals(1, queueItems.size());
        assertEquals("Glucose", queueItems.get(0).getTestNames());
    }

    @Test
    public void countTestUnitsForAnalyses_shouldCountPanelOncePerAccession() {
        Panel panel = mock(Panel.class);
        when(panel.getId()).thenReturn("panel-1");

        List<Analysis> analyses = Arrays.asList(createPanelAnalysis("analysis-1", "2026-001234", "Hemoglobin", panel),
                createPanelAnalysis("analysis-2", "2026-001234", "WBC", panel),
                createAnalysis("analysis-3", "2026-005678", "Glucose"));

        assertEquals(2, dashboardQueueMapper.countTestUnitsForAnalyses(analyses));
    }

    private Analysis createPanelAnalysis(String analysisId, String accessionNumber, String testName, Panel panel) {
        Analysis analysis = createAnalysis(analysisId, accessionNumber, testName);
        analysis.setPanel(panel);
        return analysis;
    }

    private Analysis createAnalysis(String analysisId, String accessionNumber, String testName) {
        Sample sample = new Sample();
        sample.setId("sample-" + accessionNumber);
        sample.setAccessionNumber(accessionNumber);
        sample.setReceivedDateForDisplay("06/13/2026");

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);

        org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
        when(test.getLocalizedName()).thenReturn(testName);

        Analysis analysis = new Analysis();
        analysis.setId(analysisId);
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        return analysis;
    }

    private DashboardQueueItemDTO createQueueItem(String accessionNumber, long sortEpochSeconds) {
        DashboardQueueItemDTO item = new DashboardQueueItemDTO();
        item.setAccessionNumber(accessionNumber);
        item.setOrderDateSort(new Timestamp(sortEpochSeconds * 1000L));
        return item;
    }

    private DashboardQueueItemDTO createSearchableQueueItem(String accessionNumber, String patientName,
            String subjectNumber, String nationalId) {
        DashboardQueueItemDTO item = new DashboardQueueItemDTO();
        item.setAccessionNumber(accessionNumber);
        item.setPatientName(patientName);
        item.setSubjectNumber(subjectNumber);
        item.setPatientNationalId(nationalId);
        return item;
    }
}
