package org.openelisglobal.program.service;

import java.util.Date;
import java.util.UUID;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.valueholder.Test;

public interface ProgramSampleImportService {

    /**
     * Creates a full sample graph (Sample + SampleItem + Analysis + SampleHuman)
     * plus the program sample (e.g. PathologySample) for an order imported over
     * FHIR, so the case lands directly on the program's dashboard instead of the
     * electronic-order list. The patient is resolved/created the same way the
     * electronic-order import does.
     *
     * @param program                   the program the order routes to (resolved
     *                                  from the test's test section)
     * @param test                      the resolved OpenELIS test
     * @param messagePatient            the interpreted patient
     *                                  (guid/externalId/name/dob/gender)
     * @param priority                  the order priority (may be null)
     * @param externalOrderId           the originating order identifier, stored as
     *                                  the sample's referring id
     * @param questionnaireResponseUuid the id of the already-imported
     *                                  QuestionnaireResponse, or null
     * @param collectionDate            when the specimen was collected (the order's
     *                                  authored date), or null to use the import time
     */
    void createProgramSampleFromImport(Program program, Test test, MessagePatient messagePatient,
            OrderPriority priority, String externalOrderId, UUID questionnaireResponseUuid, Date collectionDate);
}
