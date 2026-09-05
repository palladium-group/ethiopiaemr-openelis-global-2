package org.openelisglobal.dataexchange.fhir.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

/**
 * Covers how order-related resources are located during order import by following the FHIR
 * links OpenMRS's labonfhir module stamps onto each order: referring diagnoses via
 * {@code ServiceRequest.reasonReference}, and order-time context observations (specimen site,
 * clinical history, ...) via {@code ServiceRequest.supportingInfo}. Also covers synthesizing a
 * QuestionnaireResponse from those observations for the program case view.
 */
public class FhirApiWorkFlowServiceImplTest {

    private ServiceRequest serviceRequestReferring(String... conditionIds) {
        ServiceRequest serviceRequest = new ServiceRequest();
        for (String conditionId : conditionIds) {
            serviceRequest.addReasonReference(new Reference("Condition/" + conditionId));
        }
        return serviceRequest;
    }

    private ServiceRequest serviceRequestSupporting(String... observationIds) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId("sr-1");
        for (String observationId : observationIds) {
            serviceRequest.addSupportingInfo(new Reference("Observation/" + observationId));
        }
        return serviceRequest;
    }

    private Observation contextObservation(String id, String code, String label, String value) {
        Observation observation = new Observation();
        observation.setId(id);
        observation.setCode(new CodeableConcept().setText(label).addCoding(new Coding().setCode(code).setDisplay(label)));
        observation.setValue(new StringType(value));
        return observation;
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

    @Test
    public void getSupportingInfoObservationIds_collectsObservationIdsAcrossServiceRequests() {
        List<String> ids = FhirApiWorkFlowServiceImpl.getSupportingInfoObservationIds(
                Arrays.asList(serviceRequestSupporting("obs-1"), serviceRequestSupporting("obs-2", "obs-3")));

        assertEquals(Arrays.asList("obs-1", "obs-2", "obs-3"), ids);
    }

    @Test
    public void getSupportingInfoObservationIds_ignoresReferencesThatAreNotObservations() {
        ServiceRequest serviceRequest = serviceRequestSupporting("obs-1");
        serviceRequest.addSupportingInfo(new Reference("Condition/dx-1"));

        List<String> ids = FhirApiWorkFlowServiceImpl
                .getSupportingInfoObservationIds(Collections.singletonList(serviceRequest));

        assertEquals(Collections.singletonList("obs-1"), ids);
    }

    @Test
    public void getSupportingInfoObservationIds_returnsEmpty_whenNoSupportingInfo() {
        List<String> ids = FhirApiWorkFlowServiceImpl
                .getSupportingInfoObservationIds(Collections.singletonList(new ServiceRequest()));

        assertTrue(ids.isEmpty());
    }

    @Test
    public void buildQuestionnaireResponseFromObservations_buildsItemsFromSupportingInfo() {
        ServiceRequest serviceRequest = serviceRequestSupporting("obs-site", "obs-history");
        List<Observation> observations = Arrays.asList(contextObservation("obs-site", "SITE", "Specimen site", "Liver"),
                contextObservation("obs-history", "HX", "Clinical history", "Mass noted"));

        QuestionnaireResponse qr = FhirApiWorkFlowServiceImpl.buildQuestionnaireResponseFromObservations(serviceRequest,
                observations);

        assertNotNull(qr);
        assertEquals(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED, qr.getStatus());
        assertEquals("ServiceRequest/sr-1", qr.getBasedOnFirstRep().getReference());
        assertEquals(2, qr.getItem().size());
        assertEquals("SITE", qr.getItem().get(0).getLinkId());
        assertEquals("Specimen site", qr.getItem().get(0).getText());
        assertEquals("Liver", qr.getItem().get(0).getAnswerFirstRep().getValueStringType().getValue());
        assertEquals("Clinical history", qr.getItem().get(1).getText());
        assertEquals("Mass noted", qr.getItem().get(1).getAnswerFirstRep().getValueStringType().getValue());
    }

    @Test
    public void buildQuestionnaireResponseFromObservations_returnsNull_whenNoObservations() {
        assertNull(FhirApiWorkFlowServiceImpl
                .buildQuestionnaireResponseFromObservations(serviceRequestSupporting("obs-1"), Collections.emptyList()));
        assertNull(FhirApiWorkFlowServiceImpl.buildQuestionnaireResponseFromObservations(serviceRequestSupporting("obs-1"),
                null));
    }

    @Test
    public void buildQuestionnaireResponseFromObservations_ignoresUnlinkedObservations() {
        ServiceRequest serviceRequest = serviceRequestSupporting("obs-site");
        List<Observation> observations = Arrays.asList(contextObservation("obs-site", "SITE", "Specimen site", "Liver"),
                contextObservation("obs-other", "OTHER", "Other", "Should be ignored"));

        QuestionnaireResponse qr = FhirApiWorkFlowServiceImpl.buildQuestionnaireResponseFromObservations(serviceRequest,
                observations);

        assertNotNull(qr);
        assertEquals(1, qr.getItem().size());
        assertEquals("Liver", qr.getItem().get(0).getAnswerFirstRep().getValueStringType().getValue());
    }
}
