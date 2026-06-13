package org.openelisglobal.common.rest.provider;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueItemDTO;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueResponse;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DashboardQueueMapper {

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 25;

    public static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private PatientService patientService;

    @Autowired
    private SampleHumanService sampleHumanService;

    public static boolean usesQueueView(DashBoardTile.TileType listType) {
        return listType == DashBoardTile.TileType.ORDERS_IN_PROGRESS
                || listType == DashBoardTile.TileType.ORDERS_READY_FOR_VALIDATION
                || listType == DashBoardTile.TileType.ORDERS_COMPLETED_TODAY
                || listType == DashBoardTile.TileType.PENDING_RECEPTION;
    }

    public List<DashboardQueueItemDTO> groupAnalysesByAccession(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<Analysis>> analysesByAccession = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            if (analysis == null || analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
                continue;
            }
            Sample sample = analysis.getSampleItem().getSample();
            String accessionNumber = sample.getAccessionNumber();
            if (GenericValidator.isBlankOrNull(accessionNumber)) {
                continue;
            }
            analysesByAccession.computeIfAbsent(accessionNumber, key -> new ArrayList<>()).add(analysis);
        }

        List<DashboardQueueItemDTO> queueItems = new ArrayList<>();
        for (List<Analysis> accessionAnalyses : analysesByAccession.values()) {
            queueItems.add(buildQueueItem(accessionAnalyses));
        }
        return sortByOrderDateDesc(queueItems);
    }

    public List<DashboardQueueItemDTO> sortByOrderDateDesc(List<DashboardQueueItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<DashboardQueueItemDTO> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(DashboardQueueItemDTO::getOrderDateSort,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    public DashboardQueueResponse buildQueueResponse(List<DashboardQueueItemDTO> items) {
        return buildQueueResponse(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE);
    }

    public DashboardQueueResponse buildQueueResponse(List<DashboardQueueItemDTO> items, int page, int pageSize) {
        List<DashboardQueueItemDTO> sortedItems = sortByOrderDateDesc(items != null ? items : new ArrayList<>());
        int safePage = page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int totalItems = sortedItems.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safePageSize);
        int fromIndex = Math.min((safePage - 1) * safePageSize, totalItems);
        int toIndex = Math.min(fromIndex + safePageSize, totalItems);

        DashboardQueueResponse response = new DashboardQueueResponse();
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotalItems(totalItems);
        response.setTotalPages(totalPages);
        response.setItems(new ArrayList<>(sortedItems.subList(fromIndex, toIndex)));
        return response;
    }

    private DashboardQueueItemDTO buildQueueItem(List<Analysis> analyses) {
        DashboardQueueItemDTO item = new DashboardQueueItemDTO();
        Sample sample = analyses.get(0).getSampleItem().getSample();

        item.setId(sample.getId());
        item.setAccessionNumber(sample.getAccessionNumber());
        item.setPriority(sample.getPriority() != null ? sample.getPriority().toString() : "");
        item.setOrderDate(resolveOrderDateDisplay(sample, analyses.get(0)));
        item.setOrderDateSort(resolveOrderDateSort(sample, analyses.get(0)));
        item.setTestCount(analyses.size());
        item.setTestSectionId(resolveTestSectionId(analyses));
        item.setTestNames(buildTestNames(analyses));

        Patient patient = sampleHumanService.getPatientForSample(sample);
        if (patient != null) {
            item.setPatientName(formatPatientName(patient));
            item.setSubjectNumber(StringUtils.defaultString(patientService.getSubjectNumber(patient)));
            item.setPatientNationalId(StringUtils.defaultString(patientService.getNationalId(patient)));
        } else {
            item.setPatientName("");
            item.setSubjectNumber("");
            item.setPatientNationalId("");
        }

        return item;
    }

    private String resolveOrderDateDisplay(Sample sample, Analysis analysis) {
        if (sample != null && !GenericValidator.isBlankOrNull(sample.getReceivedDateForDisplay())) {
            String receivedTime = sample.getReceivedTimeForDisplay();
            if (!GenericValidator.isBlankOrNull(receivedTime)) {
                return sample.getReceivedDateForDisplay() + " " + receivedTime;
            }
            return sample.getReceivedDateForDisplay();
        }
        return analysis != null ? analysis.getStartedDateForDisplay() : "";
    }

    private Timestamp resolveOrderDateSort(Sample sample, Analysis analysis) {
        if (sample != null && sample.getReceivedTimestamp() != null) {
            return sample.getReceivedTimestamp();
        }
        if (analysis != null && analysis.getStartedDate() != null) {
            return new Timestamp(analysis.getStartedDate().getTime());
        }
        return null;
    }

    private String resolveTestSectionId(List<Analysis> analyses) {
        for (Analysis analysis : analyses) {
            if (analysis.getTestSection() != null
                    && !GenericValidator.isBlankOrNull(analysis.getTestSection().getId())) {
                return analysis.getTestSection().getId();
            }
        }
        return "";
    }

    private String buildTestNames(List<Analysis> analyses) {
        return analyses.stream()
                .map(analysis -> analysis.getTest() != null ? analysis.getTest().getLocalizedName() : "")
                .filter(name -> !GenericValidator.isBlankOrNull(name)).distinct().collect(Collectors.joining(", "));
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
}
