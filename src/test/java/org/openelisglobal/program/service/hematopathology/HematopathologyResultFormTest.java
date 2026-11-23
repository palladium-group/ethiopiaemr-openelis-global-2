package org.openelisglobal.program.service.hematopathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService.ResultFormObservation;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologySubtype;

public class HematopathologyResultFormTest {

    private static HematopathologySample sample(HematopathologySubtype subtype) {
        HematopathologySample sample = new HematopathologySample();
        sample.setSubtype(subtype);
        sample.setSmearQuality("Adequate");
        sample.setPeripheralBloodMorphology("Normocytic normochromic");
        sample.setBoneMarrowMorphology("Hypercellular marrow");
        sample.setSpecimenViability("EDTA, 90% viable");
        sample.setAntibodyPanel("Acute leukemia panel");
        sample.setGatingFindings("CD34+ blasts 40%");
        sample.setConclusion("Conclusion");
        sample.setComment("Comment");
        return sample;
    }

    private static List<String> uuids(HematopathologySample sample) {
        return HematopathologyResultForm.observationsFor(sample).stream().map(ResultFormObservation::conceptUuid)
                .collect(Collectors.toList());
    }

    @Test
    public void peripheralMorphology_sendsBloodButNotMarrowOrFlowFields() {
        List<String> uuids = uuids(sample(HematopathologySubtype.PERIPHERAL_MORPHOLOGY));
        assertTrue(uuids.contains(HematopathologyResultForm.SMEAR_QUALITY_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.PERIPHERAL_BLOOD_MORPHOLOGY_UUID));
        assertFalse(uuids.contains(HematopathologyResultForm.BONE_MARROW_MORPHOLOGY_UUID));
        assertFalse(uuids.contains(HematopathologyResultForm.GATING_FINDINGS_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.CONCLUSION_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.COMMENT_UUID));
    }

    @Test
    public void boneMarrow_sendsBothMorphologyFields() {
        List<String> uuids = uuids(sample(HematopathologySubtype.BONE_MARROW_ASPIRATE));
        assertTrue(uuids.contains(HematopathologyResultForm.PERIPHERAL_BLOOD_MORPHOLOGY_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.BONE_MARROW_MORPHOLOGY_UUID));
        assertEquals(5, uuids.size());
    }

    @Test
    public void flowCytometry_sendsFlowFieldsOnly() {
        List<String> uuids = uuids(sample(HematopathologySubtype.FLOW_CYTOMETRY));
        assertTrue(uuids.contains(HematopathologyResultForm.SPECIMEN_VIABILITY_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.ANTIBODY_PANEL_UUID));
        assertTrue(uuids.contains(HematopathologyResultForm.GATING_FINDINGS_UUID));
        assertFalse(uuids.contains(HematopathologyResultForm.SMEAR_QUALITY_UUID));
        assertEquals(5, uuids.size());
    }
}
