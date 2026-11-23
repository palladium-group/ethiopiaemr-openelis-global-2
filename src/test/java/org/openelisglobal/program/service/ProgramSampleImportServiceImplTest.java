package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologySubtype;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * Covers the stable program-code → program-sample entity mapping used when
 * auto-creating a program case from an imported FHIR order
 * ({@code PATH}/{@code IHC}/{@code CYTO}), and cytopathology subtype resolution
 * from ordered-test LOINC.
 */
public class ProgramSampleImportServiceImplTest {

    private final ProgramSampleImportServiceImpl service = new ProgramSampleImportServiceImpl();

    private Program programWithCode(String code) {
        Program program = new Program();
        program.setCode(code);
        program.setProgramName("test-" + code);
        return program;
    }

    private org.openelisglobal.test.valueholder.Test testWithLoinc(String loinc) {
        org.openelisglobal.test.valueholder.Test test = new org.openelisglobal.test.valueholder.Test();
        test.setLoinc(loinc);
        return test;
    }

    @Test
    public void newProgramSampleForProgram_createsPathologySampleForPathCode() {
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("PATH"));
        assertTrue(sample instanceof PathologySample);
    }

    @Test
    public void newProgramSampleForProgram_createsIhcSampleForIhcCode() {
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("IHC"));
        assertTrue(sample instanceof ImmunohistochemistrySample);
    }

    @Test
    public void newProgramSampleForProgram_createsCytologySampleForCytoCode() {
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("CYTO"));
        assertTrue(sample instanceof CytologySample);
    }

    @Test
    public void newProgramSampleForProgram_rejectsUnknownCode() {
        try {
            service.newProgramSampleForProgram(programWithCode("UNKNOWN"));
            fail("expected IllegalStateException for unsupported program code");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("UNKNOWN"));
        }
    }

    @Test
    public void resolveCytologySubtype_prefersOrderedTestLoinc() {
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("CYTO"), testWithLoinc("97004-0"),
                "FNAC");
        assertTrue(sample instanceof CytologySample);
        assertEquals(CytologySample.CytologySubtype.FLUID, ((CytologySample) sample).getSubtype());
    }

    @Test
    public void resolveCytologySubtype_fallsBackToSampleTypeText() {
        assertEquals(CytologySample.CytologySubtype.PAP_SMEAR,
                ProgramSampleImportServiceImpl.resolveCytologySubtype(null, "Pap smear"));
        assertEquals(CytologySample.CytologySubtype.IMAGE_GUIDED_FNAC,
                ProgramSampleImportServiceImpl.resolveCytologySubtypeFromLoinc("97003-2"));
        assertEquals(CytologySample.CytologySubtype.FNAC,
                ProgramSampleImportServiceImpl.resolveCytologySubtypeFromLoinc("33716-2"));
        assertEquals(CytologySample.CytologySubtype.PAP_SMEAR,
                ProgramSampleImportServiceImpl.resolveCytologySubtypeFromLoinc("10524-7"));
    }

    @Test
    public void newProgramSampleForProgram_createsHematopathologySampleForHemeCode() {
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("HEME"), testWithLoinc("97007-3"),
                null);
        assertTrue(sample instanceof HematopathologySample);
        HematopathologySample heme = (HematopathologySample) sample;
        assertEquals(HematopathologySample.HematopathologyStatus.RECEIVED, heme.getStatus());
        assertEquals(HematopathologySubtype.FLOW_CYTOMETRY, heme.getSubtype());
    }

    @Test
    public void resolveHematopathologySubtype_byLoinc() {
        assertEquals(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtypeFromLoinc("22637-3"));
        assertEquals(HematopathologySubtype.BONE_MARROW_ASPIRATE,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtypeFromLoinc("97006-5"));
        assertEquals(HematopathologySubtype.FLOW_CYTOMETRY,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtypeFromLoinc("97007-3"));
    }

    @Test
    public void resolveHematopathologySubtype_fallsBackToSampleTypeText() {
        assertEquals(HematopathologySubtype.BONE_MARROW_ASPIRATE,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtype(null, "Bone marrow aspirate"));
        assertEquals(HematopathologySubtype.FLOW_CYTOMETRY,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtype(null, "Flow cytometry"));
        assertEquals(HematopathologySubtype.PERIPHERAL_MORPHOLOGY,
                ProgramSampleImportServiceImpl.resolveHematopathologySubtype(null, null));
    }

    @Test
    public void legacyMorphologyLoinc_noLongerRoutesToHistopathologyBiopsy() {
        assertEquals(null, ProgramSampleImportServiceImpl.resolvePathologySubtypeFromLoinc("22637-3"));
    }
}
