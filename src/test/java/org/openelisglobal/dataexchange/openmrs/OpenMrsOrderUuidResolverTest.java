package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

@RunWith(MockitoJUnitRunner.class)
public class OpenMrsOrderUuidResolverTest {

    @Mock
    private FhirUtil fhirUtil;

    @InjectMocks
    private OpenMrsOrderUuidResolver resolver;

    @Test
    public void resolve_usesBasedOnServiceRequestIdFromTask() {
        Task task = new Task();
        task.addBasedOn(new Reference("ServiceRequest/order-uuid-123"));
        when(fhirUtil.getFhirParser()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4Cached().newJsonParser());

        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder
                .setData(ca.uhn.fhir.context.FhirContext.forR4Cached().newJsonParser().encodeResourceToString(task));
        electronicOrder.setExternalId("fallback-id");

        assertEquals("order-uuid-123", resolver.resolve(electronicOrder));
    }

    @Test
    public void resolve_fallsBackToExternalId() {
        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder.setExternalId("external-order-id");

        assertEquals("external-order-id", resolver.resolve(electronicOrder));
    }

    @Test
    public void resolveFromTaskData_returnsServiceRequestIdPart() {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId("order-uuid-456");
        Task task = new Task();
        task.addBasedOn(new Reference(serviceRequest));

        when(fhirUtil.getFhirParser()).thenReturn(ca.uhn.fhir.context.FhirContext.forR4Cached().newJsonParser());
        String taskJson = ca.uhn.fhir.context.FhirContext.forR4Cached().newJsonParser().encodeResourceToString(task);

        assertEquals("order-uuid-456", resolver.resolveFromTaskData(taskJson));
    }
}
