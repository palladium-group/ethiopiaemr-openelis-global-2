package org.openelisglobal.common.rest.provider;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashBoardTile;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueItemDTO;
import org.openelisglobal.common.rest.provider.bean.homedashboard.DashboardQueueResponse;
import org.openelisglobal.panel.valueholder.Panel;
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
                || listType == DashBoardTile.TileType.PENDING_RECEPTION
                || listType == DashBoardTile.TileType.ORDERS_PATIALLY_COMPLETED_TODAY
                || listType == DashBoardTile.TileType.ORDERS_REJECTED_TODAY
                || listType == DashBoardTile.TileType.UN_PRINTED_RESULTS
                || listType == DashBoardTile.TileType.DELAYED_TURN_AROUND
                || listType == DashBoardTile.TileType.ORDERS_FOR_USER;
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

    /**
     * Counts order test units across all analyses, grouping panel members as one
     * unit per panel per accession and standalone tests individually.
     */
    public int countTestUnitsForAnalyses(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return 0;
        }
        Map<String, List<Analysis>> analysesByAccession = new LinkedHashMap<>();
        for (Analysis analysis : analyses) {
            if (analysis == null || analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
                continue;
            }
            String accessionNumber = analysis.getSampleItem().getSample().getAccessionNumber();
            if (GenericValidator.isBlankOrNull(accessionNumber)) {
                continue;
            }
            analysesByAccession.computeIfAbsent(accessionNumber, key -> new ArrayList<>()).add(analysis);
        }
        int totalUnits = 0;
        for (List<Analysis> accessionAnalyses : analysesByAccession.values()) {
            totalUnits += countTestUnits(accessionAnalyses);
        }
        return totalUnits;
    }

    private int countTestUnits(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return 0;
        }
        int unitCount = 0;
        Set<String> includedPanelIds = new HashSet<>();
        for (Analysis analysis : analyses) {
            if (analysis == null) {
                continue;
            }
            Panel panel = analysis.getPanel();
            if (panel != null && !GenericValidator.isBlankOrNull(panel.getId())) {
                if (includedPanelIds.add(panel.getId())) {
                    unitCount++;
                }
            } else {
                unitCount++;
            }
        }
        return unitCount;
    }

    /**
     * Counts every analysis on an accession for queue row display (panel members
     * counted individually).
     */
    private int countRawTests(List<Analysis> analyses) {
        if (analyses == null || analyses.isEmpty()) {
            return 0;
        }
        int testCount = 0;
        for (Analysis analysis : analyses) {
            if (analysis != null) {
                testCount++;
            }
        }
        return testCount;
    }

    public List<DashboardQueueItemDTO> filterQueueItems(List<DashboardQueueItemDTO> items, String patientQuery,
            String labNumber) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        String normalizedPatientQuery = normalizeSearchTerm(patientQuery);
        String normalizedLabNumber = normalizeSearchTerm(labNumber);
        if (normalizedPatientQuery == null && normalizedLabNumber == null) {
            return new ArrayList<>(items);
        }
        return items.stream().filter(item -> matchesLabNumber(item, normalizedLabNumber)
                && matchesPatientQuery(item, normalizedPatientQuery)).collect(Collectors.toList());
    }

    private String normalizeSearchTerm(String value) {
        if (GenericValidator.isBlankOrNull(value)) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private boolean matchesLabNumber(DashboardQueueItemDTO item, String normalizedLabNumber) {
        if (normalizedLabNumber == null) {
            return true;
        }
        return item.getAccessionNumber() != null
                && item.getAccessionNumber().toLowerCase().contains(normalizedLabNumber);
    }

    private boolean matchesPatientQuery(DashboardQueueItemDTO item, String normalizedPatientQuery) {
        if (normalizedPatientQuery == null) {
            return true;
        }
        return containsNormalized(item.getPatientName(), normalizedPatientQuery)
                || containsNormalized(item.getPatientNationalId(), normalizedPatientQuery)
                || containsNormalized(item.getSubjectNumber(), normalizedPatientQuery);
    }

    private boolean containsNormalized(String value, String normalizedQuery) {
        return value != null && value.toLowerCase().contains(normalizedQuery);
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
        item.setTestCount(countRawTests(analyses));
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
        LinkedHashSet<String> displayNames = new LinkedHashSet<>();
        Set<String> includedPanelIds = new HashSet<>();

        for (Analysis analysis : analyses) {
            if (analysis == null) {
                continue;
            }
            Panel panel = analysis.getPanel();
            if (panel != null && !GenericValidator.isBlankOrNull(panel.getId())) {
                if (includedPanelIds.add(panel.getId())) {
                    String panelName = panel.getLocalizedName();
                    if (!GenericValidator.isBlankOrNull(panelName)) {
                        displayNames.add(panelName);
                    }
                }
                continue;
            }
            if (analysis.getTest() != null) {
                String testName = analysis.getTest().getLocalizedName();
                if (!GenericValidator.isBlankOrNull(testName)) {
                    displayNames.add(testName);
                }
            }
        }
        return String.join(", ", displayNames);
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
