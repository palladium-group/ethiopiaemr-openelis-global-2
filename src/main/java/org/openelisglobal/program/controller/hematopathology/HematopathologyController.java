package org.openelisglobal.program.controller.hematopathology;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.program.bean.PathologyPathologistOption;
import org.openelisglobal.program.service.hematopathology.HematopathologyDisplayService;
import org.openelisglobal.program.service.hematopathology.HematopathologySampleService;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologyCaseViewDisplayItem;
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

/**
 * Hematopathology case page and reception assignment. The list itself is served
 * by the shared pathology reception endpoint.
 */
@RestController
public class HematopathologyController extends BaseRestController {

    private static final String ROLE_HEMATOPATHOLOGIST = "Hematopathologist";
    private static final String CASE_VIEW = "/rest/hematopathology/caseView/{id}";

    @Autowired
    private HematopathologySampleService hematopathologySampleService;
    @Autowired
    private HematopathologyDisplayService hematopathologyDisplayService;
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private UserRoleService userRoleService;

    /** Hematopathologists for reception assignment, with open caseload. */
    @GetMapping(value = "/rest/hematopathology/hematopathologists", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<PathologyPathologistOption> getHematopathologistsWithCaseload() {
        return systemUserService.getAllSystemUsers().stream()
                .filter(u -> userRoleService.userInRole(u.getId(), ROLE_HEMATOPATHOLOGIST)).map(u -> {
                    Long caseload = hematopathologySampleService.getOpenCaseloadForHematopathologist(u.getId());
                    return new PathologyPathologistOption(u.getId(), u.getDisplayName(),
                            caseload == null ? 0L : caseload);
                }).collect(Collectors.toList());
    }

    /**
     * Assign a hematopathologist; without {@code pathologistId} the current user
     * claims the case.
     */
    @PostMapping(value = "/rest/hematopathology/assignHematopathologist", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignHematopathologist(@RequestParam Integer hematopathologySampleId,
            @RequestParam(value = "pathologistId", required = false) String pathologistId, HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        String assigneeId = (pathologistId != null && !pathologistId.isBlank()) ? pathologistId : currentUserId;
        SystemUser hematopathologist = systemUserService.get(assigneeId);
        if (hematopathologist == null) {
            return ResponseEntity.badRequest().body("unknown hematopathologist");
        }
        hematopathologySampleService.assignHematopathologist(hematopathologySampleId, hematopathologist, currentUserId);
        return ResponseEntity.ok("ok");
    }

    @PostMapping(value = "/rest/hematopathology/assignTechnician", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> assignTechnician(@RequestParam Integer hematopathologySampleId,
            HttpServletRequest request) {
        String currentUserId = getSysUserId(request);
        hematopathologySampleService.assignTechnician(hematopathologySampleId, systemUserService.get(currentUserId),
                currentUserId);
        return ResponseEntity.ok("ok");
    }

    @GetMapping(value = CASE_VIEW, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HematopathologyCaseViewDisplayItem getCase(@PathVariable("id") Integer id) {
        return hematopathologyDisplayService.convertToCaseDisplayItem(id);
    }

    @PostMapping(value = CASE_VIEW + "/confirmCollection", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> confirmCollection(@PathVariable("id") Integer id,
            @RequestBody(required = false) HematopathologySampleForm form, HttpServletRequest request) {
        try {
            OpenMrsPaymentStatus paymentStatus = hematopathologySampleService.confirmCollection(id,
                    form == null ? new HematopathologySampleForm() : form, getSysUserId(request));
            return ResponseEntity.ok(caseWithPaymentStatus(id, paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping(value = CASE_VIEW + "/checkPayment", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public HematopathologyCaseViewDisplayItem checkPayment(@PathVariable("id") Integer id, HttpServletRequest request) {
        return caseWithPaymentStatus(id, hematopathologySampleService.checkPayment(id, getSysUserId(request)));
    }

    @PostMapping(value = CASE_VIEW + "/rejectCollection", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> rejectCollection(@PathVariable("id") Integer id,
            @RequestBody(required = false) HematopathologySampleForm form, HttpServletRequest request) {
        return caseAfter(id, () -> hematopathologySampleService.rejectCollection(id,
                form == null ? null : form.getRejectionReason(), getSysUserId(request)));
    }

    @PostMapping(value = CASE_VIEW + "/step/{step}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> markStep(@PathVariable("id") Integer id, @PathVariable("step") String step,
            HttpServletRequest request) {
        return caseAfter(id, () -> hematopathologySampleService.markStep(id, step, getSysUserId(request)));
    }

    @PostMapping(value = CASE_VIEW + "/saveReadDraft", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> saveReadDraft(@PathVariable("id") Integer id, @RequestBody HematopathologySampleForm form,
            HttpServletRequest request) {
        return caseAfter(id, () -> hematopathologySampleService.saveReadDraft(id, form, getSysUserId(request)));
    }

    @PostMapping(value = CASE_VIEW + "/signOut", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> signOut(@PathVariable("id") Integer id, @RequestBody HematopathologySampleForm form,
            HttpServletRequest request) {
        return caseAfter(id, () -> hematopathologySampleService.signOut(id, form, getSysUserId(request)));
    }

    private ResponseEntity<?> caseAfter(Integer id, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok(hematopathologyDisplayService.convertToCaseDisplayItem(id));
    }

    private HematopathologyCaseViewDisplayItem caseWithPaymentStatus(Integer id, OpenMrsPaymentStatus status) {
        HematopathologyCaseViewDisplayItem item = hematopathologyDisplayService.convertToCaseDisplayItem(id);
        item.setPaymentStatus(status == null ? null : status.name());
        return item;
    }
}
