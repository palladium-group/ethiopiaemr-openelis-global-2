package org.openelisglobal.common.provider.query;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.IntegerType;
import org.junit.Test;
import org.openelisglobal.dataexchange.fhir.FhirConfig;

/**
 * Covers how imported diagnoses are ordered (by OpenMRS rank) and rendered on
 * the order-detail screen. The order-to-diagnosis link itself is carried by
 * {@code ServiceRequest.reasonReference} (see {@link LabOrderSearchProvider}),
 * so no custom matching logic remains to test here.
 */
public class LabOrderSearchProviderDiagnosisTest {

    @Test
    public void getConditionRank_readsOpenMrsRankExtension() {
        Condition condition = new Condition();
        condition.addExtension(FhirConfig.OPENMRS_DIAGNOSIS_RANK_EXTENSION_URL, new IntegerType(1));

        assertEquals(1, LabOrderSearchProvider.getConditionRank(condition));
    }

    @Test
    public void getConditionRank_defaultsToMaxValue_whenRankExtensionAbsent() {
        Condition condition = new Condition();

        assertEquals(Integer.MAX_VALUE, LabOrderSearchProvider.getConditionRank(condition));
    }

    @Test
    public void getConditionDisplayText_prefersCodeText() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.setText("Malaria");
        code.addCoding(new Coding().setDisplay("Should not be used"));
        condition.setCode(code);

        assertEquals("Malaria", LabOrderSearchProvider.getConditionDisplayText(condition));
    }

    @Test
    public void getConditionDisplayText_fallsBackToCodingDisplay_whenNoText() {
        Condition condition = new Condition();
        CodeableConcept code = new CodeableConcept();
        code.addCoding(new Coding().setDisplay("Malaria"));
        condition.setCode(code);

        assertEquals("Malaria", LabOrderSearchProvider.getConditionDisplayText(condition));
    }

    @Test
    public void getConditionDisplayText_returnsEmpty_whenNoCode() {
        Condition condition = new Condition();

        assertEquals("", LabOrderSearchProvider.getConditionDisplayText(condition));
    }
}
