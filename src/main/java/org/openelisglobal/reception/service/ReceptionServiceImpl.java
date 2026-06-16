package org.openelisglobal.reception.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.reception.dao.ReceptionDAO;
import org.openelisglobal.reception.dto.ReceptionActionResponse;
import org.openelisglobal.reception.dto.ReceptionDetailResponse;
import org.openelisglobal.reception.dto.ReceptionQueueItemDTO;
import org.openelisglobal.reception.dto.ReceptionQueueResponse;
import org.openelisglobal.reception.dto.ReceptionSampleItemDTO;
import org.openelisglobal.reception.dto.ReceptionTestDTO;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.reception.form.ReceptionApproveForm;
import org.openelisglobal.reception.form.ReceptionRejectForm;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionServiceImpl implements ReceptionService {

    private static final String SAMPLE_NOTE_SUBJECT = "Sample Note";

    @Autowired
    private ReceptionDAO receptionDAO;

    @Autowired
    private IStatusService statusService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private RejectionReasonProvider rejectionReasonProvider;

    @Autowired
    private ElectronicOrderRecollectionService electronicOrderRecollectionService;

    @Override
    @Transactional(readOnly = true)
    public ReceptionQueueResponse getPendingQueue(String accessionNumberFilter, String receivedDateFrom,
            String receivedDateTo, int page, int pageSize) {
        String pendingStatusId = statusService.getStatusID(AnalysisStatus.PendingReception);
        List<Analysis> analyses = receptionDAO.findAnalysesByStatusWithSampleFilters(pendingStatusId,
                accessionNumberFilter, parseDateStart(receivedDateFrom), parseDateEnd(receivedDateTo));

        List<ReceptionQueueItemDTO> grouped = groupAnalysesBySample(analyses);
        return paginate(grouped, page, pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceptionDetailResponse getPendingDetail(String accessionNumber) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalArgumentException("Accession number is required");
        }
        String pendingStatusId = statusService.getStatusID(AnalysisStatus.PendingReception);
        List<Analysis> analyses = receptionDAO.findAnalysesByAccessionNumberAndStatus(accessionNumber.trim(),
                pendingStatusId);
        if (analyses.isEmpty()) {
            throw new IllegalArgumentException(
                    "No samples pending reception were found for accession number: " + accessionNumber);
        }
        ReceptionDetailResponse detail = new ReceptionDetailResponse();
        populateQueueItemFromAnalyses(detail, analyses);
        detail.setSampleItems(buildSampleItemBreakdown(analyses));
        return detail;
    }

    @Override
    @Transactional
    public ReceptionActionResponse approve(ReceptionApproveForm form, String sysUserId) {
        List<Analysis> pendingAnalyses = loadPendingAnalysesForAccession(form.getAccessionNumber());
        String notTestedStatusId = statusService.getStatusID(AnalysisStatus.NotStarted);
        for (Analysis analysis : pendingAnalyses) {
            analysis.setStatusId(notTestedStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);
        }
        if (!GenericValidator.isBlankOrNull(form.getNotes())) {
            addSampleNote(pendingAnalyses.get(0).getSampleItem().getSample(), form.getNotes(), sysUserId);
        }
        return new ReceptionActionResponse(form.getAccessionNumber().trim(), pendingAnalyses.size(),
                "Sample approved for testing");
    }

    @Override
    @Transactional
    public ReceptionActionResponse reject(ReceptionRejectForm form, String sysUserId) {
        validateRejectReason(form.getRejectReasonId());
        List<Analysis> pendingAnalyses = loadPendingAnalysesForAccession(form.getAccessionNumber());
        String rejectedStatusId = statusService.getStatusID(AnalysisStatus.SampleRejected);
        String rejectReasonLabel = resolveRejectReasonLabel(form.getRejectReasonId());

        Map<String, SampleItem> updatedItems = new LinkedHashMap<>();
        for (Analysis analysis : pendingAnalyses) {
            analysis.setStatusId(rejectedStatusId);
            analysis.setSysUserId(sysUserId);
            analysisService.update(analysis);

            SampleItem item = analysis.getSampleItem();
            if (item != null && item.getId() != null && !updatedItems.containsKey(item.getId())) {
                item.setRejected(true);
                item.setRejectReasonId(form.getRejectReasonId());
                item.setSysUserId(sysUserId);
                sampleItemService.update(item);
                updatedItems.put(item.getId(), item);

                Note note = noteService.createSavableNote(item, NoteType.REJECTION_REASON, rejectReasonLabel,
                        SAMPLE_NOTE_SUBJECT, sysUserId);
                noteService.insert(note);
            }
        }

        Sample sample = pendingAnalyses.get(0).getSampleItem().getSample();
        if (!GenericValidator.isBlankOrNull(form.getNotes())) {
            addSampleNote(sample, form.getNotes(), sysUserId);
        }

        ReceptionActionResponse response = new ReceptionActionResponse(form.getAccessionNumber().trim(),
                pendingAnalyses.size(), "Sample rejected at reception");
        Optional<RecollectionOrderResult> recollectionOrder = electronicOrderRecollectionService
                .createRecollectionOrder(sample, sysUserId);
        if (recollectionOrder.isPresent()) {
            RecollectionOrderResult createdOrder = recollectionOrder.get();
            response.setRecollectionOrderCreated(true);
            response.setNewElectronicOrderId(createdOrder.getElectronicOrderId());
            response.setNewElectronicOrderExternalId(createdOrder.getExternalOrderId());
            response.setMessage("Sample rejected at reception; re-collection order created");
            addSampleNote(sample,
                    "Re-collection order created: " + createdOrder.getExternalOrderId()
                            + " (rejected at reception, accession " + form.getAccessionNumber().trim() + ")",
                    sysUserId);
        } else {
            response.setRecollectionOrderCreated(false);
            response.setMessage("Sample rejected at reception; no electronic order to re-create (manual order)");
        }

        return response;
    }

    private List<Analysis> loadPendingAnalysesForAccession(String accessionNumber) {
        if (GenericValidator.isBlankOrNull(accessionNumber)) {
            throw new IllegalArgumentException("Accession number is required");
        }
        String pendingStatusId = statusService.getStatusID(AnalysisStatus.PendingReception);
        List<Analysis> analyses = receptionDAO.findAnalysesByAccessionNumberAndStatus(accessionNumber.trim(),
                pendingStatusId);
        if (analyses.isEmpty()) {
            throw new IllegalArgumentException(
                    "No analyses pending reception were found for accession number: " + accessionNumber);
        }
        return analyses;
    }

    private List<ReceptionQueueItemDTO> groupAnalysesBySample(List<Analysis> analyses) {
        Map<String, List<Analysis>> bySampleId = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            if (analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
                continue;
            }
            String sampleId = analysis.getSampleItem().getSample().getId();
            bySampleId.computeIfAbsent(sampleId, key -> new ArrayList<>()).add(analysis);
        }

        List<ReceptionQueueItemDTO> items = new ArrayList<>();
        for (List<Analysis> sampleAnalyses : bySampleId.values()) {
            ReceptionQueueItemDTO item = new ReceptionQueueItemDTO();
            populateQueueItemFromAnalyses(item, sampleAnalyses);
            items.add(item);
        }
        return items;
    }

    private void populateQueueItemFromAnalyses(ReceptionQueueItemDTO item, List<Analysis> analyses) {
        Sample sample = analyses.get(0).getSampleItem().getSample();
        item.setSampleId(sample.getId());
        item.setAccessionNumber(sample.getAccessionNumber());
        item.setReferringId(sample.getReferringId());
        item.setPriority(sample.getPriority() != null ? sample.getPriority().toString() : null);
        item.setCollectionDate(sample.getCollectionDateForDisplay());
        item.setReceivedDate(sample.getReceivedDateForDisplay());
        item.setReceivedTime(sample.getReceivedTimeForDisplay());

        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient != null) {
            item.setPatientId(patient.getId());
            item.setPatientNationalId(patientService.getNationalId(patient));
            item.setPatientName(formatPatientName(patient));
        }

        List<ReceptionTestDTO> tests = analyses.stream().map(this::toTestDto).collect(Collectors.toList());
        item.setTests(tests);
        item.setPendingTestCount(tests.size());
    }

    private List<ReceptionSampleItemDTO> buildSampleItemBreakdown(List<Analysis> analyses) {
        Map<String, ReceptionSampleItemDTO> byItemId = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            SampleItem sampleItem = analysis.getSampleItem();
            ReceptionSampleItemDTO dto = byItemId.computeIfAbsent(sampleItem.getId(), id -> {
                ReceptionSampleItemDTO itemDto = new ReceptionSampleItemDTO();
                itemDto.setSampleItemId(sampleItem.getId());
                itemDto.setExternalId(sampleItem.getExternalId());
                itemDto.setSampleType(analysis.getSampleTypeName());
                itemDto.setCollectionDate(DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()));
                return itemDto;
            });
            dto.getTests().add(toTestDto(analysis));
        }
        return new ArrayList<>(byItemId.values());
    }

    private ReceptionTestDTO toTestDto(Analysis analysis) {
        ReceptionTestDTO dto = new ReceptionTestDTO();
        dto.setAnalysisId(analysis.getId());
        dto.setSampleItemId(analysis.getSampleItem() != null ? analysis.getSampleItem().getId() : null);
        dto.setStatusId(analysis.getStatusId());
        dto.setStatusName(statusService.getStatusNameFromId(analysis.getStatusId()));
        if (analysis.getTest() != null) {
            dto.setTestId(analysis.getTest().getId());
            dto.setTestName(analysisService.getTestDisplayName(analysis));
        }
        if (GenericValidator.isBlankOrNull(dto.getSampleType())) {
            dto.setSampleType(analysis.getSampleTypeName());
        }
        return dto;
    }

    private ReceptionQueueResponse paginate(List<ReceptionQueueItemDTO> allItems, int page, int pageSize) {
        int safePage = page < 1 ? 1 : page;
        int safePageSize = pageSize < 1 ? 25 : Math.min(pageSize, 200);
        int totalItems = allItems.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safePageSize);
        int fromIndex = Math.min((safePage - 1) * safePageSize, totalItems);
        int toIndex = Math.min(fromIndex + safePageSize, totalItems);

        ReceptionQueueResponse response = new ReceptionQueueResponse();
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        response.setItems(allItems.subList(fromIndex, toIndex));
        return response;
    }

    private Timestamp parseDateStart(String dateText) {
        if (GenericValidator.isBlankOrNull(dateText)) {
            return null;
        }
        return DateUtil.convertStringDateStringTimeToTimestamp(dateText.trim(), "00:00:00.0");
    }

    private Timestamp parseDateEnd(String dateText) {
        if (GenericValidator.isBlankOrNull(dateText)) {
            return null;
        }
        return DateUtil.convertStringDateStringTimeToTimestamp(dateText.trim(), "23:59:59.999");
    }

    private String formatPatientName(Patient patient) {
        Person person = patient.getPerson();
        if (person == null) {
            return "";
        }
        String first = person.getFirstName() != null ? person.getFirstName().trim() : "";
        String last = person.getLastName() != null ? person.getLastName().trim() : "";
        return (first + " " + last).trim();
    }

    private void validateRejectReason(String rejectReasonId) {
        boolean found = false;
        for (IdValuePair reason : rejectionReasonProvider.getRejectionReasons()) {
            if (rejectReasonId.equals(reason.getId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Invalid rejection reason id: " + rejectReasonId);
        }
    }

    private String resolveRejectReasonLabel(String rejectReasonId) {
        for (IdValuePair reason : rejectionReasonProvider.getRejectionReasons()) {
            if (rejectReasonId.equals(reason.getId())) {
                return reason.getValue();
            }
        }
        return rejectReasonId;
    }

    private void addSampleNote(Sample sample, String text, String sysUserId) {
        Sample loaded = sampleService.get(sample.getId());
        Note note = noteService.createSavableNote(loaded, NoteType.EXTERNAL, text, SAMPLE_NOTE_SUBJECT, sysUserId);
        noteService.insert(note);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> findAnalysesByStatusForDashboard(String statusId) {
        return receptionDAO.findAnalysesByStatusWithSampleFilters(statusId, null, null, null);
    }
}
