package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Test;

/**
 * Covers how referring diagnoses are located during order import: by following
 * the {@code ServiceRequest.reasonReference} links OpenMRS's labonfhir module
 * stamps onto each order (the FHIR-standard order-to-diagnosis link).
 */
public class FhirApiWorkFlowServiceImplTest {

    private ServiceRequest serviceRequestReferring(String... conditionIds) {
        ServiceRequest serviceRequest = new ServiceRequest();
        for (String conditionId : conditionIds) {
            serviceRequest.addReasonReference(new Reference("Condition/" + conditionId));
        }
        return serviceRequest;
    }

    @Test
    public void getReasonReferenceConditionIds_collectsConditionIdsAcrossServiceRequests() {
        List<String> ids = FhirApiWorkFlowServiceImpl.getReasonReferenceConditionIds(
                Arrays.asList(serviceRequestReferring("dx-1"), serviceRequestReferring("dx-2", "dx-3")));

        assertEquals(Arrays.asList("dx-1", "dx-2", "dx-3"), ids);
    }

    @Test
    public void getReasonReferenceConditionIds_ignoresReferencesThatAreNotConditions() {
        ServiceRequest serviceRequest = serviceRequestReferring("dx-1");
        serviceRequest.addReasonReference(new Reference("Observation/obs-1"));

        List<String> ids = FhirApiWorkFlowServiceImpl
                .getReasonReferenceConditionIds(Collections.singletonList(serviceRequest));

        assertEquals(Collections.singletonList("dx-1"), ids);
    }

    @Test
    public void getReasonReferenceConditionIds_returnsEmpty_whenNoReasonReferences() {
        List<String> ids = FhirApiWorkFlowServiceImpl
                .getReasonReferenceConditionIds(Collections.singletonList(new ServiceRequest()));

        assertTrue(ids.isEmpty());
    }
}
