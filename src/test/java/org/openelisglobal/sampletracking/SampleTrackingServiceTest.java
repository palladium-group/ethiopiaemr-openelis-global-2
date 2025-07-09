package org.openelisglobal.sampletracking;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.sampletracking.service.SampleTrackingService;
import org.openelisglobal.sampletracking.valueholder.SampleTracking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class SampleTrackingServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleTrackingService sampleTrackingService;

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS sampletracking CASCADE;");
        jdbcTemplate.execute("CREATE TABLE sampletracking (" + "    accNum VARCHAR(10) PRIMARY KEY,"
                + "    PATIENTID VARCHAR(20)," + "    CLIREF VARCHAR(20)," + "    PATIENTLASTNAME VARCHAR(30),"
                + "    PATIENTFIRSTNAME VARCHAR(20)," + "    ORG_LOCAL_ABBREV VARCHAR(10)," + "    ORGNAME VARCHAR(40),"
                + "    RECDDATE VARCHAR(7)," + "    TOSID VARCHAR(10)," + "    TOSDESC VARCHAR(20),"
                + "    SOSID VARCHAR(10)," + "    SOSDESC VARCHAR(20)," + "    COLLDATE NUMERIC(7,0),"
                + "    DATEOFBIRTH NUMERIC(7,0)," + "    SORI VARCHAR(1)" + ");");

        jdbcTemplate.update(
                "INSERT INTO SAMPLETRACKING (" + "    accNum, PATIENTID, CLIREF, PATIENTLASTNAME, PATIENTFIRSTNAME,"
                        + "    ORG_LOCAL_ABBREV, ORGNAME, RECDDATE, TOSID, TOSDESC,"
                        + "    SOSID, SOSDESC, COLLDATE, DATEOFBIRTH, SORI" + ") VALUES ("
                        + "    '00123456', 'PAT-001', 'CL-REF-22', 'Doe', 'John',"
                        + "    '001', 'General Hospital', '240705', '1001', 'Routine',"
                        + "    '2001', 'Emergency', 240705, 940101, 'S'" + ");");

        jdbcTemplate.update("INSERT INTO SAMPLETRACKING (" + "    accNum," + "    PATIENTID," + "    CLIREF,"
                + "    PATIENTLASTNAME," + "    PATIENTFIRSTNAME," + "    ORG_LOCAL_ABBREV," + "    ORGNAME,"
                + "    RECDDATE," + "    TOSID," + "    TOSDESC," + "    SOSID," + "    SOSDESC," + "    COLLDATE,"
                + "    DATEOFBIRTH," + "    SORI" + ") VALUES (" + "    '00123457'," + "    'PAT-002',"
                + "    'CL-REF-23'," + "    'Smith'," + "    'Jane'," + "    '002'," + "    'City Medical Center',"
                + "    '240708'," + "    '1002'," + "    'Urgent'," + "    '2002'," + "    'Scheduled'," + "    240708,"
                + "    950315," + "    'I'" + ");");
    }

    @After
    public void cleanUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS sampletracking CASCADE;");
    }

    @Test
    public void getAllSamples() {
        List<SampleTracking> sampleTrackingList = sampleTrackingService.getAll();
        assertNotNull(sampleTrackingList);
        System.out.println("Sample Tracking List: " + sampleTrackingList.size());

    }

}
