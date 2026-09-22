package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.test.valueholder.Test;

/**
 * Covers the stable program-code → program-sample entity mapping used when auto-creating a
 * program case from an imported FHIR order ({@code PATH}/{@code IHC}/{@code CYTO}), and
 * cytopathology subtype resolution from ordered-test LOINC.
 */
public class ProgramSampleImportServiceImplTest {

    private final ProgramSampleImportServiceImpl service = new ProgramSampleImportServiceImpl();

    private Program programWithCode(String code) {
        Program program = new Program();
        program.setCode(code);
        program.setProgramName("test-" + code);
        return program;
    }

    private Test testWithLoinc(String loinc) {
        Test test = new Test();
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
        ProgramSample sample = service.newProgramSampleForProgram(programWithCode("CYTO"),
                testWithLoinc("97004-0"), "FNAC");
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
}
