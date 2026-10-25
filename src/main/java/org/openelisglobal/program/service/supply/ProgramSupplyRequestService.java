package org.openelisglobal.program.service.supply;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;

public interface ProgramSupplyRequestService extends BaseObjectService<ProgramSupplyRequest, Integer> {

    List<ProgramSupplyRequest> getBySampleId(String sampleId);
}
