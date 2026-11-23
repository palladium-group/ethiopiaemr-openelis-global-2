package org.openelisglobal.program.service.hematopathology;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService.ResultFormObservation;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;

/**
 * OpenMRS "Hematopathology Result Form" member concept UUIDs
 * (ethiopiaemr-openmrs-content, openmrs_special_order_concepts.csv). Each
 * signed-out field travels back as an Observation coded by these UUIDs so
 * labonfhir imports it as an Obs the O3 results view can render.
 */
public final class HematopathologyResultForm {

    public static final String SMEAR_QUALITY_UUID = "c4000031-4444-4a2b-8c3d-0e1f2a3b4c31";
    public static final String PERIPHERAL_BLOOD_MORPHOLOGY_UUID = "c4000032-4444-4a2b-8c3d-0e1f2a3b4c32";
    public static final String BONE_MARROW_MORPHOLOGY_UUID = "c4000033-4444-4a2b-8c3d-0e1f2a3b4c33";
    public static final String SPECIMEN_VIABILITY_UUID = "c4000034-4444-4a2b-8c3d-0e1f2a3b4c34";
    public static final String ANTIBODY_PANEL_UUID = "c4000035-4444-4a2b-8c3d-0e1f2a3b4c35";
    public static final String GATING_FINDINGS_UUID = "c4000036-4444-4a2b-8c3d-0e1f2a3b4c36";
    public static final String CONCLUSION_UUID = "c4000037-4444-4a2b-8c3d-0e1f2a3b4c37";
    public static final String COMMENT_UUID = "c4000038-4444-4a2b-8c3d-0e1f2a3b4c38";

    private HematopathologyResultForm() {
    }

    /**
     * The result fields that apply to the case's test, in report order. Fields that
     * do not apply to the subtype are left out even when set.
     */
    public static List<ResultFormObservation> observationsFor(HematopathologySample sample) {
        List<ResultFormObservation> fields = new ArrayList<>();
        HematopathologySample.HematopathologySubtype subtype = sample.getSubtype();
        if (subtype == HematopathologySample.HematopathologySubtype.FLOW_CYTOMETRY) {
            fields.add(new ResultFormObservation(SPECIMEN_VIABILITY_UUID, "Flow cytometry specimen and viability",
                    sample.getSpecimenViability()));
            fields.add(new ResultFormObservation(ANTIBODY_PANEL_UUID, "Flow cytometry antibody panel",
                    sample.getAntibodyPanel()));
            fields.add(new ResultFormObservation(GATING_FINDINGS_UUID, "Flow cytometry gating and antigen expression",
                    sample.getGatingFindings()));
        } else {
            fields.add(new ResultFormObservation(SMEAR_QUALITY_UUID, "Hematopathology smear quality",
                    sample.getSmearQuality()));
            fields.add(new ResultFormObservation(PERIPHERAL_BLOOD_MORPHOLOGY_UUID, "Peripheral blood morphology",
                    sample.getPeripheralBloodMorphology()));
            if (subtype == HematopathologySample.HematopathologySubtype.BONE_MARROW_ASPIRATE) {
                fields.add(new ResultFormObservation(BONE_MARROW_MORPHOLOGY_UUID, "Bone marrow morphology",
                        sample.getBoneMarrowMorphology()));
            }
        }
        fields.add(new ResultFormObservation(CONCLUSION_UUID, "Hematopathology conclusion", sample.getConclusion()));
        fields.add(new ResultFormObservation(COMMENT_UUID, "Hematopathology comment", sample.getComment()));
        return fields;
    }
}
