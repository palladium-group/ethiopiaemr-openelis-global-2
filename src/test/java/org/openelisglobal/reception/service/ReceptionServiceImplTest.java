package org.openelisglobal.reception.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.reception.dao.ReceptionDAO;
import org.openelisglobal.reception.dto.ReceptionActionResponse;
import org.openelisglobal.reception.dto.ReceptionQueueResponse;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.reception.form.ReceptionApproveForm;
import org.openelisglobal.reception.form.ReceptionRejectForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

@RunWith(MockitoJUnitRunner.class)
public class ReceptionServiceImplTest {

    @Mock
    private ReceptionDAO receptionDAO;
    @Mock
    private IStatusService statusService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private SampleHumanService sampleHumanService;
    @Mock
    private PatientService patientService;
    @Mock
    private SampleItemService sampleItemService;
    @Mock
    private NoteService noteService;
    @Mock
    private RejectionReasonProvider rejectionReasonProvider;
    @Mock
    private ElectronicOrderRecollectionService electronicOrderRecollectionService;
    @Mock
    private SampleService sampleService;

    @InjectMocks
    private ReceptionServiceImpl receptionService;

    @Before
    public void setUp() {
        when(statusService.getStatusID(AnalysisStatus.PendingReception)).thenReturn("20");
        when(statusService.getStatusID(AnalysisStatus.NotStarted)).thenReturn("4");
        when(statusService.getStatusID(AnalysisStatus.SampleRejected)).thenReturn("10");
        when(statusService.getStatusNameFromId("20")).thenReturn("Pending Reception");
    }

    @Test
    public void getPendingQueue_groupsBySampleAndPaginates() {
        Analysis analysis1 = buildAnalysis("A1", "S1", "2025-00001", "T1");
        Analysis analysis2 = buildAnalysis("A2", "S1", "2025-00001", "T2");
        Analysis analysis3 = buildAnalysis("A3", "S2", "2025-00002", "T3");
        when(receptionDAO.findAnalysesByStatusWithSampleFilters(eq("20"), eq(null), eq(null), eq(null)))
                .thenReturn(Arrays.asList(analysis1, analysis2, analysis3));

        ReceptionQueueResponse response = receptionService.getPendingQueue(null, null, null, 1, 10);

        assertEquals(2, response.getTotalItems());
        assertEquals(2, response.getItems().size());
        assertEquals("2025-00001", response.getItems().get(0).getAccessionNumber());
        assertEquals(2, response.getItems().get(0).getPendingTestCount());
    }

    @Test
    public void approve_movesPendingAnalysesToNotTested() {
        Analysis analysis = buildAnalysis("A1", "S1", "2025-00001", "T1");
        when(receptionDAO.findAnalysesByAccessionNumberAndStatus("2025-00001", "20"))
                .thenReturn(Collections.singletonList(analysis));

        ReceptionApproveForm form = new ReceptionApproveForm();
        form.setAccessionNumber("2025-00001");

        ReceptionActionResponse response = receptionService.approve(form, "99");

        assertEquals(1, response.getAnalysesUpdated());
        ArgumentCaptor<Analysis> captor = ArgumentCaptor.forClass(Analysis.class);
        verify(analysisService).update(captor.capture());
        assertEquals("4", captor.getValue().getStatusId());
    }

    @Test
    public void reject_requiresValidRejectReason() {
        when(rejectionReasonProvider.getRejectionReasons()).thenReturn(Collections.emptyList());

        ReceptionRejectForm form = new ReceptionRejectForm();
        form.setAccessionNumber("2025-00001");
        form.setRejectReasonId("999");

        assertThrows(IllegalArgumentException.class, () -> receptionService.reject(form, "99"));
    }

    @Test
    public void reject_marksAnalysesAndSampleItemsRejected() {
        Analysis analysis = buildAnalysis("A1", "S1", "2025-00001", "T1");
        when(receptionDAO.findAnalysesByAccessionNumberAndStatus("2025-00001", "20"))
                .thenReturn(Collections.singletonList(analysis));
        when(rejectionReasonProvider.getRejectionReasons())
                .thenReturn(Collections.singletonList(new IdValuePair("1", "Hemolyzed")));
        when(noteService.createSavableNote(any(), eq(NoteType.REJECTION_REASON), anyString(), anyString(), anyString()))
                .thenReturn(new Note());
        when(electronicOrderRecollectionService.createRecollectionOrder(any(Sample.class), eq("99")))
                .thenReturn(Optional.empty());

        ReceptionRejectForm form = new ReceptionRejectForm();
        form.setAccessionNumber("2025-00001");
        form.setRejectReasonId("1");

        ReceptionActionResponse response = receptionService.reject(form, "99");

        assertEquals(1, response.getAnalysesUpdated());
        assertFalse(response.isRecollectionOrderCreated());
        verify(analysisService).update(any(Analysis.class));
        verify(sampleItemService).update(any(SampleItem.class));
    }

    @Test
    public void reject_createsRecollectionOrderWhenLinkedElectronicOrderExists() {
        Analysis analysis = buildAnalysis("A1", "S1", "2025-00001", "T1");
        analysis.getSampleItem().getSample().setClinicalOrderId("EO-1");
        when(receptionDAO.findAnalysesByAccessionNumberAndStatus("2025-00001", "20"))
                .thenReturn(Collections.singletonList(analysis));
        when(rejectionReasonProvider.getRejectionReasons())
                .thenReturn(Collections.singletonList(new IdValuePair("1", "Hemolyzed")));
        when(noteService.createSavableNote(any(), eq(NoteType.REJECTION_REASON), anyString(), anyString(), anyString()))
                .thenReturn(new Note());
        when(noteService.createSavableNote(any(), eq(NoteType.EXTERNAL), anyString(), anyString(), anyString()))
                .thenReturn(new Note());
        when(sampleService.get("S1")).thenReturn(analysis.getSampleItem().getSample());
        when(electronicOrderRecollectionService.createRecollectionOrder(any(Sample.class), eq("99")))
                .thenReturn(Optional.of(new RecollectionOrderResult("EO-2", "OPENMRS-ORDER-123-R1")));

        ReceptionRejectForm form = new ReceptionRejectForm();
        form.setAccessionNumber("2025-00001");
        form.setRejectReasonId("1");

        ReceptionActionResponse response = receptionService.reject(form, "99");

        assertTrue(response.isRecollectionOrderCreated());
        assertEquals("EO-2", response.getNewElectronicOrderId());
        assertEquals("OPENMRS-ORDER-123-R1", response.getNewElectronicOrderExternalId());
        verify(electronicOrderRecollectionService).createRecollectionOrder(any(Sample.class), eq("99"));
        verify(noteService, times(2)).insert(any(Note.class));
    }

    @Test
    public void reject_doesNotAddRecollectionNoteWhenNoLinkedOrder() {
        Analysis analysis = buildAnalysis("A1", "S1", "2025-00001", "T1");
        when(receptionDAO.findAnalysesByAccessionNumberAndStatus("2025-00001", "20"))
                .thenReturn(Collections.singletonList(analysis));
        when(rejectionReasonProvider.getRejectionReasons())
                .thenReturn(Collections.singletonList(new IdValuePair("1", "Hemolyzed")));
        when(noteService.createSavableNote(any(), eq(NoteType.REJECTION_REASON), anyString(), anyString(), anyString()))
                .thenReturn(new Note());
        when(electronicOrderRecollectionService.createRecollectionOrder(any(Sample.class), eq("99")))
                .thenReturn(Optional.empty());

        ReceptionRejectForm form = new ReceptionRejectForm();
        form.setAccessionNumber("2025-00001");
        form.setRejectReasonId("1");

        receptionService.reject(form, "99");

        verify(noteService, never()).createSavableNote(any(), eq(NoteType.EXTERNAL), anyString(), anyString(),
                anyString());
    }

    private Analysis buildAnalysis(String analysisId, String sampleId, String accessionNumber, String testId) {
        Sample sample = new Sample();
        sample.setId(sampleId);
        sample.setAccessionNumber(accessionNumber);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setId("SI-" + analysisId);
        sampleItem.setSample(sample);

        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setId(testId);
        test.setName("Test " + testId);

        Analysis analysis = new Analysis();
        analysis.setId(analysisId);
        analysis.setStatusId("20");
        analysis.setSampleItem(sampleItem);
        analysis.setTest(test);
        return analysis;
    }
}
