package org.openelisglobal.program.controller.cytology;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.bean.CytologyDashBoardCount;
import org.openelisglobal.program.service.cytology.CytologyDisplayService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.cytology.CytologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologyDisplayItem;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.service.SystemUserService;
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
public class CytologyController extends BaseRestController {

    @Autowired
    private CytologySampleService cytologySampleService;

    @Autowired
    private CytologyDisplayService cytologyDisplayService;

    @Autowired
    private SystemUserService systemUserService;

    @GetMapping(value = "/rest/cytology/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<CytologyDisplayItem> getFilteredCytologyEntries(@RequestParam(required = false) String searchTerm,
            @RequestParam CytologyStatus... statuses) {

        return cytologySampleService.searchWithStatusAndTerm(Arrays.asList(statuses), searchTerm).stream()
                .map(e -> cytologyDisplayService.convertToDisplayItem(e.getId())).collect(Collectors.toList());
    }

    @GetMapping(value = "/rest/cytology/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<CytologyDashBoardCount> getCytologyDashBoardMetrics() {
        CytologyDashBoardCount count = new CytologyDashBoardCount();
        count.setInProgress(cytologySampleService.getCountWithStatus(Arrays.asList(CytologyStatus.RECEIVED,
                CytologyStatus.CELL_BLOCK, CytologyStatus.STAINING, CytologyStatus.PREPARING_SLIDES,
                CytologyStatus.SCREENING)));
        count.setAwaitingReview(
                cytologySampleService.getCountWithStatus(Arrays.asList(CytologyStatus.READY_FOR_CYTOPATHOLOGIST)));

        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        Instant weekAgoInstant = Instant.now().minus(7, ChronoUnit.DAYS);
        Timestamp weekAgoTimestamp = Timestamp.from(weekAgoInstant);
        count.setComplete(cytologySampleService.getCountWithStatusBetweenDates(Arrays.asList(CytologyStatus.COMPLETED),
                weekAgoTimestamp, currentTimestamp));
        return ResponseEntity.ok(count);
    }

    @PostMapping(value = "/rest/cytology/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Integer cytologySampleId, HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        cytologySampleService.assignTechnician(cytologySampleId, systemUserService.get(currentUserId));
        return ResponseEntity.ok("ok");
    }

    @PostMapping(value = "/rest/cytology/assignCytoPathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignPathologist(@RequestParam Integer cytologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        cytologySampleService.assignCytoPathologist(cytologySampleId, systemUserService.get(currentUserId));
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = "/rest/cytology/caseView/{cytologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CytologyCaseViewDisplayItem getCytologyEntry(@PathVariable("cytologySampleId") Integer cytologySampleId) {
        return cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId);
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CytologySampleForm createCytologyEntry(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @RequestBody CytologySampleForm form, HttpServletRequest request) {
        form.setSystemUserId(this.getSysUserId(request));
        cytologySampleService.updateWithFormValues(cytologySampleId, form);

        return form;
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/confirmCollection",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> confirmCollection(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @RequestBody(required = false) CytologySampleForm form, HttpServletRequest request) {
        try {
            cytologySampleService.confirmCollection(cytologySampleId, form == null ? new CytologySampleForm() : form,
                    getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/rejectCollection",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> rejectCollection(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @RequestBody(required = false) CytologySampleForm form, HttpServletRequest request) {
        try {
            String reason = form == null ? null : form.getRejectionReason();
            cytologySampleService.rejectCollection(cytologySampleId, reason, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/cellBlock/{step}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> markCellBlockStep(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @PathVariable("step") String step, HttpServletRequest request) {
        try {
            cytologySampleService.markCellBlockStep(cytologySampleId, step, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/slides/{slideId}/markStained",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> markSlideStained(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @PathVariable("slideId") Integer slideId, HttpServletRequest request) {
        try {
            cytologySampleService.markSlideStained(cytologySampleId, slideId, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/addSmearSlide",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> addSmearSlide(@PathVariable("cytologySampleId") Integer cytologySampleId,
            HttpServletRequest request) {
        try {
            cytologySampleService.addSmearSlide(cytologySampleId, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/saveReadDraft",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> saveReadDraft(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @RequestBody CytologySampleForm form, HttpServletRequest request) {
        try {
            cytologySampleService.saveReadDraft(cytologySampleId, form.getMicroscopyExam(), form.getConclusion(),
                    form.getConclusionText(), getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/requestRepeat",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> requestRepeat(@PathVariable("cytologySampleId") Integer cytologySampleId,
            HttpServletRequest request) {
        try {
            cytologySampleService.requestRepeat(cytologySampleId, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/requestSecondOpinion",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> requestSecondOpinion(@PathVariable("cytologySampleId") Integer cytologySampleId,
            HttpServletRequest request) {
        try {
            cytologySampleService.requestSecondOpinion(cytologySampleId, getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/orderIhc",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> orderIhc(@PathVariable("cytologySampleId") Integer cytologySampleId,
            HttpServletRequest request) {
        try {
            cytologySampleService.orderIhc(cytologySampleId, getSysUserId(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }

    @PostMapping(value = "/rest/cytology/caseView/{cytologySampleId}/signOut",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> signOut(@PathVariable("cytologySampleId") Integer cytologySampleId,
            @RequestBody CytologySampleForm form, HttpServletRequest request) {
        try {
            cytologySampleService.signOut(cytologySampleId, form.getMicroscopyExam(), form.getConclusion(),
                    form.getConclusionText(), getSysUserId(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(cytologyDisplayService.convertToCaseDisplayItem(cytologySampleId));
    }
}
