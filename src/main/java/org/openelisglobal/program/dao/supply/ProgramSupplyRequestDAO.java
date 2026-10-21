package org.openelisglobal.program.dao.supply;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;

public interface ProgramSupplyRequestDAO extends BaseDAO<ProgramSupplyRequest, Integer> {

    /** Newest first. */
    List<ProgramSupplyRequest> getBySampleId(String sampleId);
}
