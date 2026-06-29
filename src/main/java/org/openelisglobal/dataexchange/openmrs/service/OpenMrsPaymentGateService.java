package org.openelisglobal.dataexchange.openmrs.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentOrderScope;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enforces OpenMRS payment requirements before sample collection is persisted.
 */
@Service
@Transactional(readOnly = true)
public class OpenMrsPaymentGateService {

    @Autowired
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Autowired
    private OpenMrsPaymentOrderScope paymentOrderScope;
    @Autowired
    private OpenMrsPaymentVerificationService paymentVerificationService;

    public void assertSampleCollectionAllowed(SamplePatientUpdateData updateData) {
        if (!paymentConfiguration.isGateEnabled() || updateData == null) {
            return;
        }

        List<ElectronicOrder> electronicOrders = collectElectronicOrders(updateData);
        if (electronicOrders.isEmpty()) {
            return;
        }

        for (ElectronicOrder electronicOrder : electronicOrders) {
            if (!paymentOrderScope.isSubjectToPaymentGate(electronicOrder)) {
                continue;
            }
            PaymentVerificationResult result = paymentVerificationService
                    .verifyAndSyncForElectronicOrder(electronicOrder, true);
            if (!result.isCollectionAllowed()) {
                String statusLabel = result.getStatus() == null ? OpenMrsPaymentStatus.UNKNOWN.name()
                        : result.getStatus().name();
                throw new LIMSRuntimeException(
                        MessageUtil.getMessage("openmrs.payment.required", new String[] { statusLabel }));
            }
        }
    }

    private List<ElectronicOrder> collectElectronicOrders(SamplePatientUpdateData updateData) {
        List<ElectronicOrder> orders = new ArrayList<>();
        if (updateData.getElectronicOrders() != null && !updateData.getElectronicOrders().isEmpty()) {
            orders.addAll(updateData.getElectronicOrders());
        } else if (updateData.getElectronicOrder() != null) {
            orders.add(updateData.getElectronicOrder());
        }
        orders.removeIf(order -> order == null || GenericValidator.isBlankOrNull(order.getId()));
        return orders;
    }
}
