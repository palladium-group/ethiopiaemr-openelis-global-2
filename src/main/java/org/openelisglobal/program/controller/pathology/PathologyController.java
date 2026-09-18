package org.openelisglobal.program.controller.pathology;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.bean.PathologyDashBoardCount;
import org.openelisglobal.program.bean.PathologyPathologistOption;
import org.openelisglobal.program.service.PathologyDisplayService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.PathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologyDisplayItem;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PathologyController extends BaseRestController {

    private static final String ROLE_PATHOLOGIST = "Pathologist";

    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private PathologyDisplayService pathologyDisplayService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private UserRoleService userRoleService;

    @GetMapping(value = "/rest/pathology/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<PathologyDisplayItem> getFilteredPathologyEntries(
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "unassigned", required = false, defaultValue = "false") boolean unassigned,
            @RequestParam(value = "statuses", required = false) PathologyStatus... statuses) {
        // Unassigned = no pathologist (not merely RECEIVED status). Assignment does not change status.
        if (unassigned) {
            return pathologySampleService.searchUnassigned(searchTerm).stream()
                    .map(e -> pathologyDisplayService.convertToDisplayItem(e.getId())).collect(Collectors.toList());
        }
        if (statuses == null || statuses.length == 0) {
            return List.of();
        }
        return pathologySampleService.searchWithStatusAndTerm(Arrays.asList(statuses), searchTerm).stream()
                .map(e -> pathologyDisplayService.convertToDisplayItem(e.getId())).collect(Collectors.toList());
    }

    @GetMapping(value = "/rest/pathology/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<PathologyDashBoardCount> getFilteredPathologyEntries() {
        PathologyDashBoardCount count = new PathologyDashBoardCount();
        count.setUnassigned(pathologySampleService.getCountUnassigned());
        count.setInProgress(pathologySampleService.getCountWithStatus(
                Arrays.asList(PathologyStatus.GROSSING, PathologyStatus.CUTTING, PathologyStatus.PROCESSING,
                        PathologyStatus.SLICING, PathologyStatus.STAINING)));
        count.setAwaitingReview(
                pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.READY_PATHOLOGIST)));
        count.setAdditionalRequests(
                pathologySampleService.getCountWithStatus(Arrays.asList(PathologyStatus.ADDITIONAL_REQUEST)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);

        count.setComplete(pathologySampleService.getCountWithStatusBetweenDates(
                Arrays.asList(PathologyStatus.COMPLETED), weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(count);
    }

    /**
     * Pathologists available for Reception assignment, each with open caseload count.
     */
    @GetMapping(value = "/rest/pathology/pathologists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<PathologyPathologistOption> getPathologistsWithCaseload() {
        List<SystemUser> users = systemUserService.getAllSystemUsers();
        return users.stream().filter(u -> userRoleService.userInRole(u.getId(), ROLE_PATHOLOGIST)).map(u -> {
            Long caseload = pathologySampleService.getOpenCaseloadForPathologist(u.getId());
            return new PathologyPathologistOption(u.getId(), u.getDisplayName(), caseload == null ? 0L : caseload);
        }).collect(Collectors.toList());
    }

    @PostMapping(value = "/rest/pathology/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(
            @RequestParam(value = "pathologySampleId") Integer pathologySampleId, HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        pathologySampleService.assignTechnician(pathologySampleId, systemUserService.get(currentUserId), currentUserId);
        return ResponseEntity.ok("ok");
    }

    /**
     * Assign a pathologist to a case. When {@code pathologistId} is omitted, assigns the current user
     * (legacy self-claim). Reception passes an explicit pathologistId from the dashboard dropdown.
     */
    @PostMapping(value = "/rest/pathology/assignPathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignPathologist(
            @RequestParam(value = "pathologySampleId") Integer pathologySampleId,
            @RequestParam(value = "pathologistId", required = false) String pathologistId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        String assigneeId = (pathologistId != null && !pathologistId.isBlank()) ? pathologistId : currentUserId;
        SystemUser pathologist = systemUserService.get(assigneeId);
        if (pathologist == null) {
            return ResponseEntity.badRequest().body("unknown pathologist");
        }
        pathologySampleService.assignPathologist(pathologySampleId, pathologist, currentUserId);
        return ResponseEntity.ok("ok");
    }

    /**
     * Collection Step 3: mark specimen physically received (collectionDate) and move RECEIVED → GROSSING.
     */
    @PostMapping(value = "/rest/pathology/caseView/{pathologySampleId}/confirmReceived",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologyCaseViewDisplayItem confirmReceived(
            @PathVariable("pathologySampleId") Integer pathologySampleId, HttpServletRequest request) {
        pathologySampleService.confirmReceived(pathologySampleId, getSysUserId(request));
        return pathologyDisplayService.convertToCaseDisplayItem(pathologySampleId);
    }

    @GetMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologyCaseViewDisplayItem getFilteredPathologyEntries(
            @PathVariable("pathologySampleId") Integer pathologySampleId) {
        return pathologyDisplayService.convertToCaseDisplayItem(pathologySampleId);
    }

    @PostMapping(value = "/rest/pathology/caseView/{pathologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public PathologySampleForm getFilteredPathologyEntries(@PathVariable("pathologySampleId") Integer pathologySampleId,
            @RequestBody PathologySampleForm form, HttpServletRequest request) {
        form.setSystemUserId(this.getSysUserId(request));
        pathologySampleService.updateWithFormValues(pathologySampleId, form);

        return form;
    }
}
