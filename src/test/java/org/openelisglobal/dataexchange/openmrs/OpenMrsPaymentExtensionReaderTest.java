package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Test;

public class OpenMrsPaymentExtensionReaderTest {

    private final OpenMrsPaymentExtensionReader reader = new OpenMrsPaymentExtensionReader();

    @Test
    public void readPaymentStatus_returnsStatusFromExtension() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.addExtension(
                new Extension(OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL, new CodeType("PAID")));

        OpenMrsPaymentStatus status = reader.readPaymentStatus(serviceRequest,
                OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL);

        assertEquals(OpenMrsPaymentStatus.PAID, status);
    }

    @Test
    public void readPaymentStatus_returnsUnknownWhenExtensionMissing() {
        ServiceRequest serviceRequest = new ServiceRequest();

        OpenMrsPaymentStatus status = reader.readPaymentStatus(serviceRequest,
                OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL);

        assertEquals(OpenMrsPaymentStatus.UNKNOWN, status);
    }
}
