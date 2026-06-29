package org.openelisglobal.dataexchange.openmrs.controller;

import org.apache.commons.validator.GenericValidator;
import org.hibernate.ObjectNotFoundException;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.openmrs.form.OpenMrsPaymentStatusForm;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentVerificationService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/openmrs-payment")
public class OpenMrsPaymentRestController extends BaseRestController {

    @Autowired
    private OpenMrsPaymentVerificationService paymentVerificationService;
    @Autowired
    private ElectronicOrderService electronicOrderService;

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<OpenMrsPaymentStatusForm> getPaymentStatus(@RequestParam String orderUuid,
            @RequestParam(name = "force", defaultValue = "false") boolean forceRefresh) {
        if (GenericValidator.isBlankOrNull(orderUuid)) {
            return ResponseEntity.badRequest().build();
        }
        PaymentVerificationResult result = paymentVerificationService.verifyAndSync(orderUuid.trim(), forceRefresh);
        return ResponseEntity.ok(toForm(result));
    }

    @GetMapping(value = "/electronic-order/{electronicOrderId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<OpenMrsPaymentStatusForm> refreshElectronicOrderPaymentStatus(
            @PathVariable String electronicOrderId) {
        ElectronicOrder electronicOrder;
        try {
            electronicOrder = electronicOrderService.get(electronicOrderId);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (electronicOrder == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        PaymentVerificationResult result = paymentVerificationService.verifyAndSyncForElectronicOrder(electronicOrder,
                true);
        return ResponseEntity.ok(toForm(result));
    }

    private OpenMrsPaymentStatusForm toForm(PaymentVerificationResult result) {
        OpenMrsPaymentStatus status = result.getStatus() == null ? OpenMrsPaymentStatus.UNKNOWN : result.getStatus();
        return OpenMrsPaymentStatusForm.from(result.getOrderUuid(), status.name(), result.isCollectionAllowed(),
                result.isSyncedToLocalFhir(), result.getVerifiedAt());
    }
}
