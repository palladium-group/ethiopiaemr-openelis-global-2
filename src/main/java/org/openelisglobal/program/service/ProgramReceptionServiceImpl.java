package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.program.bean.ProgramReceptionDashBoardCount;
import org.openelisglobal.program.service.cytology.CytologyDisplayService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.ProgramCaseDisplayItem;
import org.openelisglobal.program.valueholder.ReceptionBucket;
import org.openelisglobal.program.valueholder.ServiceCategory;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgramReceptionServiceImpl implements ProgramReceptionService {

    /** Kept identical to the per-program dashboards so the tiles and the lists agree. */
    private static final List<PathologyStatus> HISTOPATHOLOGY_IN_PROGRESS = Arrays.asList(PathologyStatus.GROSSING,
            PathologyStatus.CUTTING, PathologyStatus.PROCESSING, PathologyStatus.EMBEDDING, PathologyStatus.SLICING,
            PathologyStatus.STAINING);

    private static final List<CytologyStatus> CYTOPATHOLOGY_IN_PROGRESS = Arrays.asList(CytologyStatus.RECEIVED,
            CytologyStatus.CELL_BLOCK, CytologyStatus.STAINING, CytologyStatus.PREPARING_SLIDES,
            CytologyStatus.SCREENING);

    private static final int COMPLETE_TILE_WINDOW_DAYS = 7;

    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private PathologyDisplayService pathologyDisplayService;
    @Autowired
    private CytologySampleService cytologySampleService;
    @Autowired
    private CytologyDisplayService cytologyDisplayService;

    @Override
    @Transactional
    public List<ProgramCaseDisplayItem> search(Collection<ServiceCategory> categories, ReceptionBucket bucket,
            String searchTerm) {
        Collection<ServiceCategory> selected = selectedOrAll(categories);
        ReceptionBucket selectedBucket = bucket == null ? ReceptionBucket.UNASSIGNED : bucket;

        List<ProgramCaseDisplayItem> rows = new ArrayList<>();
        if (selected.contains(ServiceCategory.HISTOPATHOLOGY)) {
            rows.addAll(searchHistopathology(selectedBucket, searchTerm));
        }
        if (selected.contains(ServiceCategory.CYTOPATHOLOGY)) {
            rows.addAll(searchCytopathology(selectedBucket, searchTerm));
        }

        Comparator<ProgramCaseDisplayItem> newestFirst = Comparator.comparing(
                ProgramCaseDisplayItem::getRequestDate, Comparator.nullsLast(Comparator.<Date>reverseOrder()));
        Comparator<ProgramCaseDisplayItem> thenByLabNumber = Comparator.comparing(
                ProgramCaseDisplayItem::getLabNumber, Comparator.nullsLast(Comparator.<String>naturalOrder()));
        rows.sort(newestFirst.thenComparing(thenByLabNumber));
        return rows;
    }

    @Override
    @Transactional
    public ProgramReceptionDashBoardCount count(Collection<ServiceCategory> categories) {
        Collection<ServiceCategory> selected = selectedOrAll(categories);
        boolean histopathology = selected.contains(ServiceCategory.HISTOPATHOLOGY);
        boolean cytopathology = selected.contains(ServiceCategory.CYTOPATHOLOGY);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp windowStart = Timestamp
                .from(Instant.now().minus(COMPLETE_TILE_WINDOW_DAYS, ChronoUnit.DAYS));

        ProgramReceptionDashBoardCount count = new ProgramReceptionDashBoardCount();
        long unassigned = 0;
        long inProgress = 0;
        long awaitingReview = 0;
        long additionalRequests = 0;
        long complete = 0;

        if (histopathology) {
            unassigned += zeroWhenNull(pathologySampleService.getCountUnassigned());
            inProgress += zeroWhenNull(pathologySampleService.getCountWithStatus(HISTOPATHOLOGY_IN_PROGRESS));
            awaitingReview += zeroWhenNull(
                    pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)));
            additionalRequests += zeroWhenNull(
                    pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.ADDITIONAL_REQUEST)));
            complete += zeroWhenNull(pathologySampleService.getCountWithStatusBetweenDates(
                    Arrays.asList(PathologyStatus.COMPLETED), windowStart, now));
        }
        if (cytopathology) {
            unassigned += zeroWhenNull(cytologySampleService.getCountUnassigned());
            inProgress += zeroWhenNull(cytologySampleService.getCountWithStatus(CYTOPATHOLOGY_IN_PROGRESS));
            awaitingReview += zeroWhenNull(cytologySampleService
                    .getCountWithStatus(Arrays.asList(CytologyStatus.READY_FOR_CYTOPATHOLOGIST)));
            complete += zeroWhenNull(cytologySampleService.getCountWithStatusBetweenDates(
                    Arrays.asList(CytologyStatus.COMPLETED), windowStart, now));
        }

        count.setUnassigned(unassigned);
        count.setInProgress(inProgress);
        count.setAwaitingReview(awaitingReview);
        count.setAdditionalRequests(additionalRequests);
        count.setComplete(complete);
        return count;
    }

    private List<ProgramCaseDisplayItem> searchHistopathology(ReceptionBucket bucket, String searchTerm) {
        if (bucket == ReceptionBucket.UNASSIGNED) {
            return pathologySampleService.searchUnassigned(searchTerm).stream()
                    .map(sample -> toRow(pathologyDisplayService.convertToDisplayItem(sample.getId())))
                    .collect(Collectors.toList());
        }
        List<PathologyStatus> statuses = histopathologyStatusesFor(bucket);
        if (statuses.isEmpty()) {
            return List.of();
        }
        return pathologySampleService.searchWithStatusAndTerm(statuses, searchTerm).stream()
                .map(sample -> toRow(pathologyDisplayService.convertToDisplayItem(sample.getId())))
                .collect(Collectors.toList());
    }

    private List<ProgramCaseDisplayItem> searchCytopathology(ReceptionBucket bucket, String searchTerm) {
        if (bucket == ReceptionBucket.UNASSIGNED) {
            return cytologySampleService.searchUnassigned(searchTerm).stream()
                    .map(sample -> toRow(cytologyDisplayService.convertToDisplayItem(sample.getId()), sample))
                    .collect(Collectors.toList());
        }
        List<CytologyStatus> statuses = cytopathologyStatusesFor(bucket);
        if (statuses.isEmpty()) {
            return List.of();
        }
        return cytologySampleService.searchWithStatusAndTerm(statuses, searchTerm).stream()
                .map(sample -> toRow(cytologyDisplayService.convertToDisplayItem(sample.getId()), sample))
                .collect(Collectors.toList());
    }

    private List<PathologyStatus> histopathologyStatusesFor(ReceptionBucket bucket) {
        switch (bucket) {
        case IN_PROGRESS:
            return HISTOPATHOLOGY_IN_PROGRESS;
        case AWAITING_REVIEW:
            return Arrays.asList(PathologyStatus.READY_PATHOLOGIST);
        case ADDITIONAL_REQUEST:
            return Arrays.asList(PathologyStatus.ADDITIONAL_REQUEST);
        case COMPLETED:
            return Arrays.asList(PathologyStatus.COMPLETED);
        case ALL:
            return Arrays.asList(PathologyStatus.values());
        default:
            return List.of();
        }
    }

    private List<CytologyStatus> cytopathologyStatusesFor(ReceptionBucket bucket) {
        switch (bucket) {
        case IN_PROGRESS:
            return CYTOPATHOLOGY_IN_PROGRESS;
        case AWAITING_REVIEW:
            return Arrays.asList(CytologyStatus.READY_FOR_CYTOPATHOLOGIST);
        case COMPLETED:
            return Arrays.asList(CytologyStatus.COMPLETED);
        case ALL:
            return Arrays.asList(CytologyStatus.values());
        case ADDITIONAL_REQUEST:
            // no cytopathology equivalent; the tile counts histopathology only
            return List.of();
        default:
            return List.of();
        }
    }

    private ProgramCaseDisplayItem toRow(PathologyDisplayItem item) {
        ProgramCaseDisplayItem row = newRow(ServiceCategory.HISTOPATHOLOGY, item.getPathologySampleId());
        row.setRequestDate(item.getRequestDate());
        if (item.getStatus() != null) {
            row.setStatusCode(item.getStatus().name());
            row.setStatus(item.getStatus().getDisplay());
        }
        row.setSubtype(item.getSubtype());
        row.setFirstName(item.getFirstName());
        row.setLastName(item.getLastName());
        row.setRequester(item.getRequester());
        row.setAssignedTechnician(item.getAssignedTechnician());
        row.setAssignedSpecialist(item.getAssignedPathologist());
        row.setLabNumber(item.getLabNumber());
        row.setPatientPK(item.getPatientPK());
        return row;
    }

    private ProgramCaseDisplayItem toRow(CytologyDisplayItem item, CytologySample cytologySample) {
        ProgramCaseDisplayItem row = newRow(ServiceCategory.CYTOPATHOLOGY, item.getPathologySampleId());
        row.setRequestDate(item.getRequestDate());
        if (item.getStatus() != null) {
            row.setStatusCode(item.getStatus().name());
            row.setStatus(item.getStatus().getDisplay());
        }
        if (item.getSubtype() != null) {
            row.setSubtype(item.getSubtype().getDisplay());
        }
        row.setFirstName(item.getFirstName());
        row.setLastName(item.getLastName());
        // The cytology list DTO carries no requester, so resolve it here to keep the merged
        // Requesting physician column populated for both categories.
        row.setRequester(resolveCytologyRequester(cytologySample));
        row.setAssignedTechnician(item.getAssignedTechnician());
        row.setAssignedSpecialist(item.getAssignedCytoPathologist());
        row.setLabNumber(item.getLabNumber());
        row.setPatientPK(item.getPatientPK());
        return row;
    }

    private ProgramCaseDisplayItem newRow(ServiceCategory category, Integer caseId) {
        ProgramCaseDisplayItem row = new ProgramCaseDisplayItem();
        row.setServiceCategoryCode(category.getCode());
        row.setServiceCategory(category.getDisplay());
        row.setCaseId(caseId);
        row.setRowId(category.getCode() + "-" + caseId);
        return row;
    }

    private String resolveCytologyRequester(CytologySample cytologySample) {
        if (cytologySample == null || cytologySample.getSample() == null) {
            return null;
        }
        try {
            SampleOrderItem orderItem = new SampleOrderService(cytologySample.getSample()).getSampleOrderItem();
            String requester = ((orderItem.getProviderLastName() == null ? "" : orderItem.getProviderLastName()) + " "
                    + (orderItem.getProviderFirstName() == null ? "" : orderItem.getProviderFirstName())).trim();
            return StringUtils.isBlank(requester) ? null : requester;
        } catch (RuntimeException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveCytologyRequester",
                    "could not resolve requesting physician for cytology case " + cytologySample.getId() + ": "
                            + e.getMessage());
            return null;
        }
    }

    private Collection<ServiceCategory> selectedOrAll(Collection<ServiceCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return EnumSet.allOf(ServiceCategory.class);
        }
        return EnumSet.copyOf(categories);
    }

    private long zeroWhenNull(Long value) {
        return value == null ? 0L : value;
    }
}
