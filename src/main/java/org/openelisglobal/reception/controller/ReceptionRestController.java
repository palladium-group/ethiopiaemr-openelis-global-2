package org.openelisglobal.reception.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reception.dto.ReceptionActionResponse;
import org.openelisglobal.reception.dto.ReceptionDetailResponse;
import org.openelisglobal.reception.dto.ReceptionQueueResponse;
import org.openelisglobal.reception.form.ReceptionApproveForm;
import org.openelisglobal.reception.form.ReceptionRejectForm;
import org.openelisglobal.reception.service.ReceptionService;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/reception")
@Validated
public class ReceptionRestController extends BaseRestController {

    @Autowired
    private ReceptionService receptionService;
    @Autowired
    private UserRoleService userRoleService;

    @GetMapping(value = "/queue", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ReceptionQueueResponse> getQueue(@RequestParam(required = false) String accessionNumber,
            @RequestParam(required = false) String receivedDateFrom,
            @RequestParam(required = false) String receivedDateTo, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int pageSize, HttpServletRequest request) {
        requireReceptionRole(request);
        ReceptionQueueResponse response = receptionService.getPendingQueue(accessionNumber, receivedDateFrom,
                receivedDateTo, page, pageSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/{accessionNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ReceptionDetailResponse> getDetail(@PathVariable String accessionNumber,
            HttpServletRequest request) {
        requireReceptionRole(request);
        return ResponseEntity.ok(receptionService.getPendingDetail(accessionNumber));
    }

    @PostMapping(value = "/approve", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ReceptionActionResponse> approve(@Valid @RequestBody ReceptionApproveForm form,
            HttpServletRequest request) {
        requireReceptionRole(request);
        String sysUserId = requireSysUserId(request);
        return ResponseEntity.ok(receptionService.approve(form, sysUserId));
    }

    @PostMapping(value = "/reject", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ReceptionActionResponse> reject(@Valid @RequestBody ReceptionRejectForm form,
            HttpServletRequest request) {
        requireReceptionRole(request);
        String sysUserId = requireSysUserId(request);
        return ResponseEntity.ok(receptionService.reject(form, sysUserId));
    }

    private String requireSysUserId(HttpServletRequest request) {
        String sysUserId = getSysUserId(request);
        if (sysUserId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return sysUserId;
    }

    private void requireReceptionRole(HttpServletRequest request) {
        String sysUserId = requireSysUserId(request);
        if (!userRoleService.userInRole(sysUserId, Constants.ROLE_SAMPLE_RECEPTION_APPROVAL)) {
            throw new SecurityException("User does not have Sample Reception Approval role");
        }
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(jakarta.validation.ConstraintViolationException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleValidationException", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation Error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleIllegalArgumentException", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Invalid Request", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleIllegalStateException", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Invalid State", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        LogEvent.logWarn(this.getClass().getName(), "handleSecurityException", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Forbidden", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        LogEvent.logError(this.getClass().getName(), "handleGeneralException", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("Internal Server Error",
                "An unexpected error occurred. Please contact support if the problem persists."));
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
