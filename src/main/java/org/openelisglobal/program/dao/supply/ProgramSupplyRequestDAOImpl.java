package org.openelisglobal.program.dao.supply;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProgramSupplyRequestDAOImpl extends BaseDAOImpl<ProgramSupplyRequest, Integer>
        implements ProgramSupplyRequestDAO {

    ProgramSupplyRequestDAOImpl() {
        super(ProgramSupplyRequest.class);
    }

    @Override
    public List<ProgramSupplyRequest> getBySampleId(String sampleId) {
        String hql = "from ProgramSupplyRequest r where r.sample.id = :sampleId order by r.requestedAt desc, r.id desc";
        Query<ProgramSupplyRequest> query = entityManager.unwrap(Session.class).createQuery(hql,
                ProgramSupplyRequest.class);
        query.setParameter("sampleId", Integer.parseInt(sampleId));
        return query.list();
    }

    public String getTableName() {
        return "program_supply_request";
    }
}
