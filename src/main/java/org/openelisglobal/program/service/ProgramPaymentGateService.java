package org.openelisglobal.program.service;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentVerificationService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Decides whether a pathology/cytology case may move past collection. Cases
 * imported from OpenMRS carry the OpenMRS order UUID in
 * {@code sample.referringId}; its ServiceRequest payment-status is read from
 * the remote FHIR store (always fresh, never cached). Cases entered directly in
 * OpenELIS have no referring order and are not gated.
 */
@Service
public class ProgramPaymentGateService {

    @Autowired
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Autowired
    private OpenMrsPaymentVerificationService paymentVerificationService;

    public OpenMrsPaymentStatus checkPayment(Sample sample) {
        if (!paymentConfiguration.isPathologyGateEnabled() || sample == null
                || GenericValidator.isBlankOrNull(sample.getReferringId())) {
            return OpenMrsPaymentStatus.NOT_APPLICABLE;
        }
        PaymentVerificationResult result = paymentVerificationService.verifyOrderPayment(sample.getReferringId(), true);
        return result.getStatus() == null ? OpenMrsPaymentStatus.UNKNOWN : result.getStatus();
    }

    public static boolean allowsProceed(OpenMrsPaymentStatus status) {
        return status == OpenMrsPaymentStatus.NOT_APPLICABLE || status.allowsSampleCollection();
    }
}
