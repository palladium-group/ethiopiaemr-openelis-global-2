package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;

public class OpenMrsPaymentOrderScopeTest {

    private final OpenMrsPaymentOrderScope scope = new OpenMrsPaymentOrderScope();

    @Test
    public void isSubjectToPaymentGate_matchesFhirOrdersOnly() {
        ElectronicOrder fhirOrder = new ElectronicOrder();
        fhirOrder.setType(ElectronicOrderType.FHIR);
        ElectronicOrder hl7Order = new ElectronicOrder();
        hl7Order.setType(ElectronicOrderType.HL7_V2);

        assertTrue(scope.isSubjectToPaymentGate(fhirOrder));
        assertFalse(scope.isSubjectToPaymentGate(hl7Order));
        assertFalse(scope.isSubjectToPaymentGate(null));
    }
}
