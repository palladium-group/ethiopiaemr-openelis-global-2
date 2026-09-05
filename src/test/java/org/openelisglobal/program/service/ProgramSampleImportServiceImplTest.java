package org.openelisglobal.program.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;

/**
 * Covers the stable program-code → program-sample entity mapping used when auto-creating a
 * program case from an imported FHIR order ({@code PATH}/{@code IHC}/{@code CYTO}).
 */
public class ProgramSampleImportServiceImplTest {

    private final ProgramSampleImportServiceImpl service = new ProgramSampleImportServiceImpl();

    private Program programWithCode(String code) {
        Program program = new Program();
        program.setCode(code);
        program.setProgramName("test-" + code);
        return program;
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
}
