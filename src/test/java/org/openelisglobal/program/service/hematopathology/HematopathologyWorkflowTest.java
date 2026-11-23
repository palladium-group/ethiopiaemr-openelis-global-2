package org.openelisglobal.program.service.hematopathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologySubtype;

public class HematopathologyWorkflowTest {

    @Test
    public void statusAfterCollection_morphologyGoesToSmearPrep() {
        assertEquals(HematopathologyStatus.SMEAR_PREP,
                HematopathologyWorkflow.statusAfterCollection(HematopathologySubtype.PERIPHERAL_MORPHOLOGY));
        assertEquals(HematopathologyStatus.SMEAR_PREP,
                HematopathologyWorkflow.statusAfterCollection(HematopathologySubtype.BONE_MARROW_ASPIRATE));
    }

    @Test
    public void statusAfterCollection_flowGoesToProcessing() {
        assertEquals(HematopathologyStatus.PROCESSING,
                HematopathologyWorkflow.statusAfterCollection(HematopathologySubtype.FLOW_CYTOMETRY));
    }

    @Test
    public void morphology_smearPreparedThenStained_reachesReview() {
        assertEquals(HematopathologyStatus.SMEAR_PREP,
                HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                        HematopathologyStatus.SMEAR_PREP, HematopathologyWorkflow.STEP_SMEAR_PREPARED, false));
        assertEquals(HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST,
                HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.BONE_MARROW_ASPIRATE,
                        HematopathologyStatus.SMEAR_PREP, HematopathologyWorkflow.STEP_SMEAR_STAINED, true));
    }

    @Test
    public void morphology_stainBeforePrepare_isRejected() {
        try {
            HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                    HematopathologyStatus.SMEAR_PREP, HematopathologyWorkflow.STEP_SMEAR_STAINED, false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void flow_processingThenAcquisition_reachesReview() {
        assertEquals(HematopathologyStatus.ACQUISITION,
                HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                        HematopathologyStatus.PROCESSING, HematopathologyWorkflow.STEP_PROCESSING, false));
        assertEquals(HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST,
                HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                        HematopathologyStatus.ACQUISITION, HematopathologyWorkflow.STEP_ACQUISITION, false));
    }

    @Test
    public void flowStepOnMorphologyCase_isRejected() {
        try {
            HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                    HematopathologyStatus.SMEAR_PREP, HematopathologyWorkflow.STEP_PROCESSING, false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void morphologyStepOnFlowCase_isRejected() {
        try {
            HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                    HematopathologyStatus.PROCESSING, HematopathologyWorkflow.STEP_SMEAR_PREPARED, false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void stepWhileAwaitingPayment_isRejected() {
        try {
            HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                    HematopathologyStatus.AWAITING_PAYMENT, HematopathologyWorkflow.STEP_PROCESSING, false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void repeatedStepAfterMovingOn_isIdempotent() {
        assertNull(HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                HematopathologyStatus.READY_FOR_HEMATOPATHOLOGIST, HematopathologyWorkflow.STEP_PROCESSING, false));
        assertNull(HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                HematopathologyStatus.COMPLETED, HematopathologyWorkflow.STEP_SMEAR_STAINED, true));
    }

    @Test
    public void unknownStep_isRejected() {
        try {
            HematopathologyWorkflow.statusAfterStep(HematopathologySubtype.FLOW_CYTOMETRY,
                    HematopathologyStatus.PROCESSING, "centrifuge", false);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }
}
