package org.openelisglobal.program.dao.cytology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class CytologySampleDAOImpl extends BaseDAOImpl<CytologySample, Integer> implements CytologySampleDAO {
    CytologySampleDAOImpl() {
        super(CytologySample.class);
    }

    @Override
    public List<CytologySample> getWithStatus(List<CytologyStatus> statuses) {
        String sql = "from CytologySample cs where status in (:statuses)";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        List<CytologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatus(List<CytologyStatus> statuses) {
        String sql = "select count(*) from CytologySample cs where status in (:statuses)";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();

        return count;
    }

    @Override
    public List<CytologySample> searchWithStatusAndAccesionNumber(List<CytologyStatus> statuses, String labNumber) {
        String sql = "from CytologySample cs where cs.status in (:statuses) and cs.sample.accessionNumber ="
                + " :labNumber";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("labNumber", labNumber);
        List<CytologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from CytologySample cs where cs.status in (:statuses) and cs.lastupdated"
                + " between :datefrom and :dateto";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        Long count = query.uniqueResult();
        return count;
    }

    @Override
    public Long getCountUnassigned() {
        String sql = "select count(*) from CytologySample cs where cs.cytoPathologist is null"
                + " and cs.status <> :completed and cs.status <> :rejected";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameter("completed", CytologyStatus.COMPLETED.toString());
        query.setParameter("rejected", CytologyStatus.REJECTED.toString());
        return query.uniqueResult();
    }

    @Override
    public List<CytologySample> getUnassigned() {
        String sql = "from CytologySample cs where cs.cytoPathologist is null and cs.status <> :completed"
                + " and cs.status <> :rejected";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameter("completed", CytologyStatus.COMPLETED.toString());
        query.setParameter("rejected", CytologyStatus.REJECTED.toString());
        return query.list();
    }

    @Override
    public List<CytologySample> searchUnassignedWithAccessionNumber(String labNumber) {
        String sql = "from CytologySample cs where cs.cytoPathologist is null and cs.status <> :completed"
                + " and cs.status <> :rejected and cs.sample.accessionNumber = :labNumber";
        Query<CytologySample> query = entityManager.unwrap(Session.class).createQuery(sql, CytologySample.class);
        query.setParameter("completed", CytologyStatus.COMPLETED.toString());
        query.setParameter("rejected", CytologyStatus.REJECTED.toString());
        query.setParameter("labNumber", labNumber);
        return query.list();
    }

    @Override
    public Long getOpenCaseloadForCytoPathologist(String cytoPathologistId) {
        String sql = "select count(*) from CytologySample cs where cs.cytoPathologist = :cytoPathologist"
                + " and cs.status <> :completed and cs.status <> :rejected";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        SystemUser cytoPathologist = entityManager.unwrap(Session.class).get(SystemUser.class, cytoPathologistId);
        if (cytoPathologist == null) {
            return 0L;
        }
        query.setParameter("cytoPathologist", cytoPathologist);
        query.setParameter("completed", CytologyStatus.COMPLETED.toString());
        query.setParameter("rejected", CytologyStatus.REJECTED.toString());
        return query.uniqueResult();
    }
}
