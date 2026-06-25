package org.openelisglobal.dataexchange.openmrs;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Determines whether an electronic order is subject to OpenMRS payment
 * verification.
 */
@Component
public class OpenMrsPaymentOrderScope {

    @Autowired
    private OpenMrsOrderUuidResolver orderUuidResolver;

    public boolean isSubjectToPaymentGate(ElectronicOrder electronicOrder) {
        return electronicOrder != null && ElectronicOrderType.FHIR == electronicOrder.getType();
    }

    public boolean hasResolvableOrderUuid(ElectronicOrder electronicOrder) {
        return !GenericValidator.isBlankOrNull(orderUuidResolver.resolve(electronicOrder));
    }
}
