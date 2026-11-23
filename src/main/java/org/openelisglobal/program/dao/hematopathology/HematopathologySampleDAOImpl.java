package org.openelisglobal.program.dao.hematopathology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class HematopathologySampleDAOImpl extends BaseDAOImpl<HematopathologySample, Integer>
        implements HematopathologySampleDAO {

    private static final String OPEN = " hs.status <> :completed and hs.status <> :rejected";

    HematopathologySampleDAOImpl() {
        super(HematopathologySample.class);
    }

    private Session session() {
        return entityManager.unwrap(Session.class);
    }

    private static List<String> names(List<HematopathologyStatus> statuses) {
        return statuses.stream().map(Enum::toString).collect(Collectors.toList());
    }

    private static void bindOpen(Query<?> query) {
        query.setParameter("completed", HematopathologyStatus.COMPLETED.toString());
        query.setParameter("rejected", HematopathologyStatus.REJECTED.toString());
    }

    @Override
    public List<HematopathologySample> getWithStatus(List<HematopathologyStatus> statuses) {
        Query<HematopathologySample> query = session().createQuery(
                "from HematopathologySample hs where hs.status in (:statuses)", HematopathologySample.class);
        query.setParameterList("statuses", names(statuses));
        return query.list();
    }

    @Override
    public List<HematopathologySample> searchWithStatusAndAccessionNumber(List<HematopathologyStatus> statuses,
            String labNumber) {
        Query<HematopathologySample> query = session().createQuery(
                "from HematopathologySample hs where hs.status in (:statuses) and hs.sample.accessionNumber = :labNumber",
                HematopathologySample.class);
        query.setParameterList("statuses", names(statuses));
        query.setParameter("labNumber", labNumber);
        return query.list();
    }

    @Override
    public Long getCountWithStatus(List<HematopathologyStatus> statuses) {
        Query<Long> query = session().createQuery(
                "select count(*) from HematopathologySample hs where hs.status in (:statuses)", Long.class);
        query.setParameterList("statuses", names(statuses));
        return query.uniqueResult();
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<HematopathologyStatus> statuses, Timestamp from, Timestamp to) {
        Query<Long> query = session().createQuery("select count(*) from HematopathologySample hs where hs.status in"
                + " (:statuses) and hs.lastupdated between :datefrom and :dateto", Long.class);
        query.setParameterList("statuses", names(statuses));
        query.setParameter("datefrom", from);
        query.setParameter("dateto", to);
        return query.uniqueResult();
    }

    @Override
    public Long getCountUnassigned() {
        Query<Long> query = session().createQuery(
                "select count(*) from HematopathologySample hs where hs.hematopathologist is null and" + OPEN,
                Long.class);
        bindOpen(query);
        return query.uniqueResult();
    }

    @Override
    public List<HematopathologySample> getUnassigned() {
        Query<HematopathologySample> query = session().createQuery(
                "from HematopathologySample hs where hs.hematopathologist is null and" + OPEN,
                HematopathologySample.class);
        bindOpen(query);
        return query.list();
    }

    @Override
    public List<HematopathologySample> searchUnassignedWithAccessionNumber(String labNumber) {
        Query<HematopathologySample> query = session().createQuery("from HematopathologySample hs where"
                + " hs.hematopathologist is null and" + OPEN + " and hs.sample.accessionNumber = :labNumber",
                HematopathologySample.class);
        bindOpen(query);
        query.setParameter("labNumber", labNumber);
        return query.list();
    }

    @Override
    public Long getOpenCaseloadForHematopathologist(String hematopathologistId) {
        SystemUser hematopathologist = session().get(SystemUser.class, hematopathologistId);
        if (hematopathologist == null) {
            return 0L;
        }
        Query<Long> query = session().createQuery(
                "select count(*) from HematopathologySample hs where hs.hematopathologist = :user and" + OPEN,
                Long.class);
        query.setParameter("user", hematopathologist);
        bindOpen(query);
        return query.uniqueResult();
    }
}
