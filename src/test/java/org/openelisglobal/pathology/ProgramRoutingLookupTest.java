package org.openelisglobal.pathology;

import org.junit.Assert;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Verifies the routing lookup that decides whether an imported FHIR order is a
 * program case (e.g. Pathology) and should be auto-created on the program
 * dashboard instead of the electronic-order list. This is the gating decision
 * used by FhirApiWorkFlowServiceImpl.processTaskImportOrder.
 */
public class ProgramRoutingLookupTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ProgramService programService;
    @Autowired
    private TestSectionService testSectionService;

    @Test
    public void getProgramByTestSectionId_returnsProgramForPathologySection() {
        TestSection pathologySection = testSectionService.getTestSectionByName("Pathology");
        Assert.assertNotNull("Pathology test section should be seeded", pathologySection);

        // The Histopathology program is created at startup from programs/pathology.json
        // in real
        // deployments; ensure one is linked to the Pathology section for this test
        // context.
        if (programService.getProgramByTestSectionId(pathologySection.getId()) == null) {
            Program program = new Program();
            program.setCode("PATHTST");
            program.setProgramName("Histopathology");
            program.setTestSection(pathologySection);
            program.setManuallyChanged(false);
            program.setSysUserId("1");
            programService.save(program);
        }

        Program found = programService.getProgramByTestSectionId(pathologySection.getId());
        Assert.assertNotNull("a program should be found for the pathology test section", found);
        Assert.assertEquals(pathologySection.getId(), found.getTestSection().getId());
    }

    @Test
    public void getProgramByTestSectionId_returnsNullForUnlinkedSection() {
        Assert.assertNull("no program should be found for a non-existent test section",
                programService.getProgramByTestSectionId("999999"));
    }
}
