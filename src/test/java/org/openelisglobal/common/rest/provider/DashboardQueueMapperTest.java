package org.openelisglobal.common.rest.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

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
    public void groupAnalysesByAccession_shouldReturnOneRowForMultipleTestsOnSameAccession() {
        List<Analysis> analyses = Arrays.asList(createAnalysis("analysis-1", "2026-001234", "Chemistry"),
                createAnalysis("analysis-2", "2026-001234", "Hematology"),
                createAnalysis("analysis-3", "2026-001234", "Serology"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals("Should group to one accession row", 1, queueItems.size());
        DashboardQueueItemDTO item = queueItems.get(0);
        assertEquals("2026-001234", item.getAccessionNumber());
        assertEquals(3, item.getTestCount());
        assertEquals("Jane Doe", item.getPatientName());
        assertEquals("334-422-A", item.getSubjectNumber());
        assertEquals("ET-123", item.getPatientNationalId());
        assertNotNull(item.getTestNames());
    }

    @Test
    public void groupAnalysesByAccession_shouldReturnSeparateRowsForDifferentAccessions() {
        List<Analysis> analyses = Arrays.asList(createAnalysis("analysis-1", "2026-001234", "Chemistry"),
                createAnalysis("analysis-2", "2026-005678", "Chemistry"));

        List<DashboardQueueItemDTO> queueItems = dashboardQueueMapper.groupAnalysesByAccession(analyses);

        assertEquals(2, queueItems.size());
    }

    @Test
    public void buildQueueResponse_shouldExposePlaceholderPaginationMetadata() {
        DashboardQueueItemDTO item = new DashboardQueueItemDTO();
        item.setAccessionNumber("2026-001234");

        DashboardQueueResponse response = dashboardQueueMapper.buildQueueResponse(Arrays.asList(item));

        assertEquals(1, response.getPage());
        assertEquals(1, response.getPageSize());
        assertEquals(1, response.getTotalItems());
        assertEquals(1, response.getTotalPages());
        assertEquals(1, response.getItems().size());
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
        assertEquals(false, DashboardQueueMapper.usesQueueView(
                org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile.TileType.INCOMING_ORDERS));
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
}
