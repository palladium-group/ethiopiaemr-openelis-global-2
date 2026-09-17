package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class PathologySampleDAOImpl extends BaseDAOImpl<PathologySample, Integer> implements PathologySampleDAO {
    PathologySampleDAOImpl() {
        super(PathologySample.class);
    }

    @Override
    public List<PathologySample> getWithStatus(List<PathologyStatus> statuses) {
        String sql = "from PathologySample ps where status in (:statuses)";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        List<PathologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatus(List<PathologyStatus> statuses) {
        String sql = "select count(*) from PathologySample ps where status in (:statuses)";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        Long count = query.uniqueResult();

        return count;
    }

    @Override
    public List<PathologySample> searchWithStatusAndAccesionNumber(List<PathologyStatus> statuses, String labNumber) {
        String sql = "from PathologySample ps where ps.status in (:statuses) and ps.sample.accessionNumber ="
                + " :labNumber";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameterList("statuses", statuses.stream().map(e -> e.toString()).collect(Collectors.toList()));
        query.setParameter("labNumber", labNumber);
        List<PathologySample> list = query.list();

        return list;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to) {
        String sql = "select count(*) from PathologySample ps where ps.status in (:statuses) and ps.lastupdated"
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
        String sql = "select count(*) from PathologySample ps where ps.pathologist is null"
                + " and ps.status <> :completed";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        query.setParameter("completed", PathologyStatus.COMPLETED.toString());
        return query.uniqueResult();
    }

    @Override
    public List<PathologySample> getUnassigned() {
        String sql = "from PathologySample ps where ps.pathologist is null and ps.status <> :completed";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameter("completed", PathologyStatus.COMPLETED.toString());
        return query.list();
    }

    @Override
    public List<PathologySample> searchUnassignedWithAccessionNumber(String labNumber) {
        String sql = "from PathologySample ps where ps.pathologist is null and ps.status <> :completed"
                + " and ps.sample.accessionNumber = :labNumber";
        Query<PathologySample> query = entityManager.unwrap(Session.class).createQuery(sql, PathologySample.class);
        query.setParameter("completed", PathologyStatus.COMPLETED.toString());
        query.setParameter("labNumber", labNumber);
        return query.list();
    }

    @Override
    public Long getOpenCaseloadForPathologist(String pathologistId) {
        // Bind the SystemUser entity (id is String in Java, numeric in DB) to avoid
        // Postgres "numeric = character varying" when comparing ids directly.
        String sql = "select count(*) from PathologySample ps where ps.pathologist = :pathologist"
                + " and ps.status <> :completed";
        Query<Long> query = entityManager.unwrap(Session.class).createQuery(sql, Long.class);
        SystemUser pathologist = entityManager.unwrap(Session.class).get(SystemUser.class, pathologistId);
        if (pathologist == null) {
            return 0L;
        }
        query.setParameter("pathologist", pathologist);
        query.setParameter("completed", PathologyStatus.COMPLETED.toString());
        return query.uniqueResult();
    }
}
