package org.openelisglobal.program.service;

import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.Program;

public interface ProgramService extends BaseObjectService<Program, String> {

    /**
     * Finds the Program associated with the given test section, if any. Used to
     * route incoming FHIR orders to their program workflow (e.g. Pathology) instead
     * of the generic electronic-order path.
     *
     * @param testSectionId the test section id
     * @return the matching Program, or null if the test section is not linked to a
     *         program
     */
    Program getProgramByTestSectionId(String testSectionId);
}
