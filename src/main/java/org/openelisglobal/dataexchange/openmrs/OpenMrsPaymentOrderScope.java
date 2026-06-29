package org.openelisglobal.dataexchange.openmrs;

import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;
import org.springframework.stereotype.Component;

/**
 * Determines whether an electronic order is subject to OpenMRS payment
 * verification.
 */
@Component
public class OpenMrsPaymentOrderScope {

    public boolean isSubjectToPaymentGate(ElectronicOrder electronicOrder) {
        return electronicOrder != null && ElectronicOrderType.FHIR == electronicOrder.getType();
    }
}
