package org.openelisglobal.program.service.supply;

import java.util.List;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.program.dao.supply.ProgramSupplyRequestDAO;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramSupplyRequestServiceImpl extends AuditableBaseObjectServiceImpl<ProgramSupplyRequest, Integer>
        implements ProgramSupplyRequestService {

    @Autowired
    protected ProgramSupplyRequestDAO baseObjectDAO;

    ProgramSupplyRequestServiceImpl() {
        super(ProgramSupplyRequest.class);
    }

    @Override
    protected ProgramSupplyRequestDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramSupplyRequest> getBySampleId(String sampleId) {
        return getBaseObjectDAO().getBySampleId(sampleId);
    }
}
