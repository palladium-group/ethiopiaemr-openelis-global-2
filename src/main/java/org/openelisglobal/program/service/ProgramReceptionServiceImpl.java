package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
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
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProgramReceptionServiceImpl implements ProgramReceptionService {

    /** Kept identical to the per-program dashboards so the tiles and the lists agree. */
    private static final List<PathologyStatus> HISTOPATHOLOGY_IN_PROGRESS = Arrays.asList(PathologyStatus.GROSSING,
            PathologyStatus.CUTTING, PathologyStatus.PROCESSING, PathologyStatus.EMBEDDING, PathologyStatus.SLICING,
            PathologyStatus.STAINING);

    private static final List<CytologyStatus> CYTOPATHOLOGY_IN_PROGRESS = Arrays.asList(CytologyStatus.CELL_BLOCK,
            CytologyStatus.STAINING, CytologyStatus.PREPARING_SLIDES, CytologyStatus.SCREENING);

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
            String searchTerm, boolean assignedToMe, String currentUserId) {
        Collection<ServiceCategory> selected = selectedOrAll(categories);
        ReceptionBucket selectedBucket = bucket == null ? ReceptionBucket.UNASSIGNED : bucket;
        boolean mineOnly = assignedToMe && StringUtils.isNotBlank(currentUserId);

        List<ProgramCaseDisplayItem> rows = new ArrayList<>();
        if (selected.contains(ServiceCategory.HISTOPATHOLOGY)) {
            rows.addAll(searchHistopathology(selectedBucket, searchTerm, mineOnly, currentUserId));
        }
        if (selected.contains(ServiceCategory.CYTOPATHOLOGY)) {
            rows.addAll(searchCytopathology(selectedBucket, searchTerm, mineOnly, currentUserId));
        }

        Comparator<ProgramCaseDisplayItem> newestFirst = Comparator.comparing(ProgramCaseDisplayItem::getLastUpdated,
                Comparator.nullsLast(Comparator.<Timestamp>reverseOrder()));
        Comparator<ProgramCaseDisplayItem> thenByLabNumber = Comparator.comparing(ProgramCaseDisplayItem::getLabNumber,
                Comparator.nullsLast(Comparator.<String>reverseOrder()));
        rows.sort(newestFirst.thenComparing(thenByLabNumber));
        return rows;
    }

    @Override
    @Transactional
    public ProgramReceptionDashBoardCount count(Collection<ServiceCategory> categories, boolean assignedToMe,
            String currentUserId) {
        Collection<ServiceCategory> selected = selectedOrAll(categories);
        boolean histopathology = selected.contains(ServiceCategory.HISTOPATHOLOGY);
        boolean cytopathology = selected.contains(ServiceCategory.CYTOPATHOLOGY);
        boolean mineOnly = assignedToMe && StringUtils.isNotBlank(currentUserId);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp windowStart = Timestamp.from(Instant.now().minus(COMPLETE_TILE_WINDOW_DAYS, ChronoUnit.DAYS));

        ProgramReceptionDashBoardCount count = new ProgramReceptionDashBoardCount();
        long unassigned = 0;
        long received = 0;
        long inProgress = 0;
        long awaitingReview = 0;
        long additionalRequests = 0;
        long complete = 0;

        if (!mineOnly) {
            if (histopathology) {
                unassigned += zeroWhenNull(pathologySampleService.getCountUnassigned());
                received += zeroWhenNull(
                        pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.RECEIVED)));
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
                received += zeroWhenNull(
                        cytologySampleService.getCountWithStatus(Arrays.asList(CytologyStatus.RECEIVED)));
                inProgress += zeroWhenNull(cytologySampleService.getCountWithStatus(CYTOPATHOLOGY_IN_PROGRESS));
                awaitingReview += zeroWhenNull(cytologySampleService
                        .getCountWithStatus(Arrays.asList(CytologyStatus.READY_FOR_CYTOPATHOLOGIST)));
                complete += zeroWhenNull(cytologySampleService.getCountWithStatusBetweenDates(
                        Arrays.asList(CytologyStatus.COMPLETED), windowStart, now));
            }
        } else {
            if (histopathology) {
                unassigned += countAssignedPathology(pathologySampleService.searchUnassigned(null), currentUserId);
                received += countAssignedPathology(
                        pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.RECEIVED)), currentUserId);
                inProgress += countAssignedPathology(pathologySampleService.getWithStatus(HISTOPATHOLOGY_IN_PROGRESS),
                        currentUserId);
                awaitingReview += countAssignedPathology(
                        pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)),
                        currentUserId);
                additionalRequests += countAssignedPathology(
                        pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.ADDITIONAL_REQUEST)),
                        currentUserId);
                complete += pathologySampleService.getWithStatus(Arrays.asList(PathologyStatus.COMPLETED)).stream()
                        .filter(sample -> isAssignedToUser(sample.getPathologist(), sample.getTechnician(),
                                currentUserId))
                        .filter(sample -> inCompleteWindow(sample.getLastupdated(), windowStart, now)).count();
            }
            if (cytopathology) {
                unassigned += countAssignedCytology(cytologySampleService.searchUnassigned(null), currentUserId);
                received += countAssignedCytology(
                        cytologySampleService.getWithStatus(Arrays.asList(CytologyStatus.RECEIVED)), currentUserId);
                inProgress += countAssignedCytology(cytologySampleService.getWithStatus(CYTOPATHOLOGY_IN_PROGRESS),
                        currentUserId);
                awaitingReview += countAssignedCytology(
                        cytologySampleService.getWithStatus(Arrays.asList(CytologyStatus.READY_FOR_CYTOPATHOLOGIST)),
                        currentUserId);
                complete += cytologySampleService.getWithStatus(Arrays.asList(CytologyStatus.COMPLETED)).stream()
                        .filter(sample -> isAssignedToUser(sample.getCytoPathologist(), sample.getTechnician(),
                                currentUserId))
                        .filter(sample -> inCompleteWindow(sample.getLastupdated(), windowStart, now)).count();
            }
        }

        count.setUnassigned(unassigned);
        count.setReceived(received);
        count.setInProgress(inProgress);
        count.setAwaitingReview(awaitingReview);
        count.setAdditionalRequests(additionalRequests);
        count.setComplete(complete);
        return count;
    }

    private List<ProgramCaseDisplayItem> searchHistopathology(ReceptionBucket bucket, String searchTerm,
            boolean mineOnly, String currentUserId) {
        List<PathologySample> samples;
        if (bucket == ReceptionBucket.UNASSIGNED) {
            samples = pathologySampleService.searchUnassigned(searchTerm);
        } else {
            List<PathologyStatus> statuses = histopathologyStatusesFor(bucket);
            if (statuses.isEmpty()) {
                return List.of();
            }
            samples = pathologySampleService.searchWithStatusAndTerm(statuses, searchTerm);
        }
        if (mineOnly) {
            samples = samples.stream()
                    .filter(sample -> isAssignedToUser(sample.getPathologist(), sample.getTechnician(), currentUserId))
                    .collect(Collectors.toList());
        }
        if (bucket == ReceptionBucket.COMPLETED) {
            samples = filterCompletedWindow(samples, PathologySample::getLastupdated);
        }
        return samples.stream()
                .map(sample -> toRow(pathologyDisplayService.convertToDisplayItem(sample.getId()), sample))
                .collect(Collectors.toList());
    }

    private List<ProgramCaseDisplayItem> searchCytopathology(ReceptionBucket bucket, String searchTerm,
            boolean mineOnly, String currentUserId) {
        List<CytologySample> samples;
        if (bucket == ReceptionBucket.UNASSIGNED) {
            samples = cytologySampleService.searchUnassigned(searchTerm);
        } else {
            List<CytologyStatus> statuses = cytopathologyStatusesFor(bucket);
            if (statuses.isEmpty()) {
                return List.of();
            }
            samples = cytologySampleService.searchWithStatusAndTerm(statuses, searchTerm);
        }
        if (mineOnly) {
            samples = samples.stream().filter(
                    sample -> isAssignedToUser(sample.getCytoPathologist(), sample.getTechnician(), currentUserId))
                    .collect(Collectors.toList());
        }
        if (bucket == ReceptionBucket.COMPLETED) {
            samples = filterCompletedWindow(samples, CytologySample::getLastupdated);
        }
        return samples.stream()
                .map(sample -> toRow(cytologyDisplayService.convertToDisplayItem(sample.getId()), sample))
                .collect(Collectors.toList());
    }

    private <T> List<T> filterCompletedWindow(List<T> samples,
            java.util.function.Function<T, Timestamp> lastUpdatedGetter) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp windowStart = Timestamp.from(Instant.now().minus(COMPLETE_TILE_WINDOW_DAYS, ChronoUnit.DAYS));
        return samples.stream().filter(sample -> inCompleteWindow(lastUpdatedGetter.apply(sample), windowStart, now))
                .collect(Collectors.toList());
    }

    private long countAssignedPathology(List<PathologySample> samples, String currentUserId) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }
        return samples.stream()
                .filter(sample -> isAssignedToUser(sample.getPathologist(), sample.getTechnician(), currentUserId))
                .count();
    }

    private long countAssignedCytology(List<CytologySample> samples, String currentUserId) {
        if (samples == null || samples.isEmpty()) {
            return 0L;
        }
        return samples.stream()
                .filter(sample -> isAssignedToUser(sample.getCytoPathologist(), sample.getTechnician(), currentUserId))
                .count();
    }

    /**
     * My cases = current user is the assigned specialist or the assigned technician.
     */
    private boolean isAssignedToUser(SystemUser specialist, SystemUser technician, String currentUserId) {
        return userMatches(specialist, currentUserId) || userMatches(technician, currentUserId);
    }

    private boolean userMatches(SystemUser user, String currentUserId) {
        return user != null && currentUserId != null && Objects.equals(currentUserId, user.getId());
    }

    private boolean inCompleteWindow(Timestamp lastUpdated, Timestamp windowStart, Timestamp now) {
        if (lastUpdated == null) {
            return false;
        }
        return !lastUpdated.before(windowStart) && !lastUpdated.after(now);
    }

    private List<PathologyStatus> histopathologyStatusesFor(ReceptionBucket bucket) {
        switch (bucket) {
        case RECEIVED:
            return Arrays.asList(PathologyStatus.RECEIVED);
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
        case RECEIVED:
            return Arrays.asList(CytologyStatus.RECEIVED);
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

    private ProgramCaseDisplayItem toRow(PathologyDisplayItem item, PathologySample pathologySample) {
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
        row.setLastUpdated(resolveLastUpdated(pathologySample == null ? null : pathologySample.getSample(),
                pathologySample == null ? null : pathologySample.getLastupdated()));
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
        row.setLastUpdated(resolveLastUpdated(cytologySample == null ? null : cytologySample.getSample(),
                cytologySample == null ? null : cytologySample.getLastupdated()));
        return row;
    }

    private Timestamp resolveLastUpdated(Sample sample, Timestamp programSampleLastUpdated) {
        if (sample != null && sample.getLastupdated() != null) {
            return sample.getLastupdated();
        }
        return programSampleLastUpdated;
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
