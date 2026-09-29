package org.openelisglobal.program.dao;

import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ProgramDAOImpl extends BaseDAOImpl<Program, String> implements ProgramDAO {
    ProgramDAOImpl() {
        super(Program.class);
    }

    @Override
    public Program getProgramByTestSectionId(String testSectionId) {
        if (testSectionId == null) {
            return null;
        }
        // Compare against the TestSection entity (not the id) so Hibernate binds the
        // association's
        // foreign key with the correct column type; the test_section id is a
        // numeric-backed String.
        TestSection testSection = entityManager.find(TestSection.class, testSectionId);
        if (testSection == null) {
            return null;
        }
        String hql = "from Program p where p.testSection = :testSection";
        Query<Program> query = entityManager.unwrap(Session.class).createQuery(hql, Program.class);
        query.setParameter("testSection", testSection);
        query.setMaxResults(1);
        List<Program> programs = query.list();
        return programs.isEmpty() ? null : programs.get(0);
    }
}
