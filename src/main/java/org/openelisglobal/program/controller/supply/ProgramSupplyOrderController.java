package org.openelisglobal.program.controller.supply;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderService;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Medical supply orders from the pathology and cytology case pages. Access
 * follows the case views themselves (Results on the Pathology/Cytology unit).
 */
@RestController
public class ProgramSupplyOrderController extends BaseRestController {

    @Autowired
    private ProgramSupplyOrderService supplyOrderService;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private CytologySampleService cytologySampleService;

    @GetMapping(value = "/rest/pathology/supplyOrder/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchCatalog(@RequestParam(value = "q", required = false) String term) {
        return handle(() -> supplyOrderService.searchCatalog(term));
    }

    @GetMapping(value = "/rest/pathology/supplyOrder/units", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getQuantityUnits() {
        return handle(supplyOrderService::getQuantityUnits);
    }

    @GetMapping(value = "/rest/pathology/caseView/{id}/supplyRequests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPathologyRequests(@PathVariable("id") Integer id) {
        return handle(() -> caseRequests(pathologyCase(id).getSample()));
    }

    @PostMapping(value = "/rest/pathology/caseView/{id}/supplyRequests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPathologyRequest(@PathVariable("id") Integer id,
            @RequestBody ProgramSupplyRequestForm form, HttpServletRequest request) {
        return handle(() -> {
            PathologySample pathologySample = pathologyCase(id);
            return supplyOrderService.create(pathologySample.getSample(), isClosed(pathologySample), form,
                    getSysUserId(request));
        });
    }

    @PostMapping(value = "/rest/pathology/caseView/{id}/supplyRequests/{requestId}/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> retryPathologyRequest(@PathVariable("id") Integer id,
            @PathVariable("requestId") Integer requestId, HttpServletRequest request) {
        return handle(() -> {
            PathologySample pathologySample = pathologyCase(id);
            return supplyOrderService.retry(requestId, pathologySample.getSample().getId(), isClosed(pathologySample),
                    getSysUserId(request));
        });
    }

    @GetMapping(value = "/rest/cytology/caseView/{id}/supplyRequests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getCytologyRequests(@PathVariable("id") Integer id) {
        return handle(() -> caseRequests(cytologyCase(id).getSample()));
    }

    @PostMapping(value = "/rest/cytology/caseView/{id}/supplyRequests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCytologyRequest(@PathVariable("id") Integer id,
            @RequestBody ProgramSupplyRequestForm form, HttpServletRequest request) {
        return handle(() -> {
            CytologySample cytologySample = cytologyCase(id);
            return supplyOrderService.create(cytologySample.getSample(), isClosed(cytologySample), form,
                    getSysUserId(request));
        });
    }

    @PostMapping(value = "/rest/cytology/caseView/{id}/supplyRequests/{requestId}/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> retryCytologyRequest(@PathVariable("id") Integer id,
            @PathVariable("requestId") Integer requestId, HttpServletRequest request) {
        return handle(() -> {
            CytologySample cytologySample = cytologyCase(id);
            return supplyOrderService.retry(requestId, cytologySample.getSample().getId(), isClosed(cytologySample),
                    getSysUserId(request));
        });
    }

    /**
     * {@code fromOpenMrs} is false for cases entered in OpenELIS: they have no
     * OpenMRS patient/order to order supplies against, so the page hides the
     * action.
     */
    private Map<String, Object> caseRequests(Sample sample) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fromOpenMrs", !GenericValidator.isBlankOrNull(sample.getReferringId()));
        body.put("requests", supplyOrderService.listForSample(sample));
        return body;
    }

    private PathologySample pathologyCase(Integer id) {
        PathologySample pathologySample = pathologySampleService.get(id);
        if (pathologySample == null || pathologySample.getSample() == null) {
            throw new ProgramSupplyOrderException(ProgramSupplyOrderException.Code.NOT_FOUND, "Case not found");
        }
        return pathologySample;
    }

    private CytologySample cytologyCase(Integer id) {
        CytologySample cytologySample = cytologySampleService.get(id);
        if (cytologySample == null || cytologySample.getSample() == null) {
            throw new ProgramSupplyOrderException(ProgramSupplyOrderException.Code.NOT_FOUND, "Case not found");
        }
        return cytologySample;
    }

    private static boolean isClosed(PathologySample pathologySample) {
        return pathologySample.getStatus() == PathologyStatus.COMPLETED;
    }

    private static boolean isClosed(CytologySample cytologySample) {
        return cytologySample.getStatus() == CytologyStatus.COMPLETED
                || cytologySample.getStatus() == CytologyStatus.REJECTED;
    }

    private ResponseEntity<?> handle(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (ProgramSupplyOrderException e) {
            Map<String, String> body = new LinkedHashMap<>();
            body.put("code", e.getCode().name());
            body.put("message", e.getMessage());
            return ResponseEntity.status(statusFor(e.getCode())).body(body);
        }
    }

    private static HttpStatus statusFor(ProgramSupplyOrderException.Code code) {
        switch (code) {
        case INVALID_ITEMS:
            return HttpStatus.BAD_REQUEST;
        case NOT_FOUND:
            return HttpStatus.NOT_FOUND;
        case NOT_ENABLED:
        case NOT_CONFIGURED:
        case OPENMRS_UNAVAILABLE:
            return HttpStatus.SERVICE_UNAVAILABLE;
        default:
            return HttpStatus.CONFLICT;
        }
    }
}
