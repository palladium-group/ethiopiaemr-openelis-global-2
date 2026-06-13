package org.openelisglobal.reception.daoimpl;

import java.sql.Timestamp;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.reception.dao.ReceptionDAO;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ReceptionDAOImpl extends BaseDAOImpl<Analysis, String> implements ReceptionDAO {

    public ReceptionDAOImpl() {
        super(Analysis.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> findAnalysesByStatusWithSampleFilters(String statusId, String accessionNumberFilter,
            Timestamp receivedFrom, Timestamp receivedTo) throws LIMSRuntimeException {
        try {
            StringBuilder hql = new StringBuilder(
                    "select distinct a from Analysis a join fetch a.sampleItem si join fetch si.sample s left join fetch a.test t left join fetch a.panel p "
                            + "where a.statusId = :statusId");
            if (!GenericValidator.isBlankOrNull(accessionNumberFilter)) {
                hql.append(" and lower(s.accessionNumber) like lower(:accessionFilter)");
            }
            if (receivedFrom != null) {
                hql.append(" and s.receivedTimestamp >= :receivedFrom");
            }
            if (receivedTo != null) {
                hql.append(" and s.receivedTimestamp <= :receivedTo");
            }
            hql.append(" order by s.receivedTimestamp desc, s.accessionNumber asc");

            Query<Analysis> query = entityManager.unwrap(Session.class).createQuery(hql.toString(), Analysis.class);
            query.setParameter("statusId", Integer.parseInt(statusId));
            if (!GenericValidator.isBlankOrNull(accessionNumberFilter)) {
                query.setParameter("accessionFilter", "%" + accessionNumberFilter.trim() + "%");
            }
            if (receivedFrom != null) {
                query.setParameter("receivedFrom", receivedFrom);
            }
            if (receivedTo != null) {
                query.setParameter("receivedTo", receivedTo);
            }
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findAnalysesByStatusWithSampleFilters", e.getMessage());
            throw new LIMSRuntimeException("Error loading reception queue analyses", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Analysis> findAnalysesByAccessionNumberAndStatus(String accessionNumber, String statusId)
            throws LIMSRuntimeException {
        try {
            String hql = "select distinct a from Analysis a join fetch a.sampleItem si join fetch si.sample s "
                    + "left join fetch a.test t left join fetch a.testSection ts "
                    + "where s.accessionNumber = :accessionNumber and a.statusId = :statusId "
                    + "order by si.sortOrder asc, t.name asc";
            Query<Analysis> query = entityManager.unwrap(Session.class).createQuery(hql, Analysis.class);
            query.setParameter("accessionNumber", accessionNumber);
            query.setParameter("statusId", Integer.parseInt(statusId));
            return query.list();
        } catch (HibernateException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "findAnalysesByAccessionNumberAndStatus",
                    e.getMessage());
            throw new LIMSRuntimeException("Error loading reception detail analyses", e);
        }
    }
}
