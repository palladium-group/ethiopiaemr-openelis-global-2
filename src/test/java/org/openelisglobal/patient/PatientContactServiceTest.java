package org.openelisglobal.patient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import javax.sql.DataSource;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class PatientContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientContactService patientContactService;

    private JdbcTemplate jdbcTemplate;
    private List<PatientContact> patientContacts;

    @Autowired
    public void setDataSource(@NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {

        jdbcTemplate.execute("" + "CREATE TABLE IF NOT EXISTS person (" + "    id numeric(10,0) NOT NULL,"
                + "    last_name character varying(255)," + "    first_name character varying(255),"
                + "    middle_name character varying(255)," + "    multiple_unit character varying(255),"
                + "    street_address character varying(255)," + "    city character varying(255),"
                + "    state character(2)," + "    zip_code character(10)," + "    country character varying(255),"
                + "    work_phone character varying(255)," + "    home_phone character varying(255),"
                + "    cell_phone character varying(255)," + "    fax character varying(255),"
                + "    email character varying(255)," + "    lastupdated timestamp(6) without time zone,"
                + "    primary_phone character varying(255)" + ");");

        jdbcTemplate.update(
                "INSERT INTO person (id, last_name, first_name, email) VALUES (701, 'Cobby', 'Peters', 'cobpeters@peters.com')");

        jdbcTemplate.execute("" + "CREATE TABLE IF NOT EXISTS clinlims.patient (" + "    id numeric(10,0) NOT NULL,"
                + "    person_id numeric(10,0) NOT NULL," + "    race character varying(5),"
                + "    gender character varying(1)," + "    birth_date timestamp without time zone,"
                + "    epi_first_name character varying(25)," + "    epi_middle_name character varying(25),"
                + "    epi_last_name character varying(240)," + "    birth_time timestamp without time zone,"
                + "    death_date timestamp without time zone," + "    national_id character varying(255),"
                + "    ethnicity character varying(1)," + "    school_attend character varying(240),"
                + "    medicare_id character varying(240)," + "    medicaid_id character varying(240),"
                + "    birth_place character varying(255)," + "    lastupdated timestamp(6) without time zone,"
                + "    external_id character varying(255)," + "    chart_number character varying(20),"
                + "    entered_birth_date character varying(10)" + ");");

        jdbcTemplate.update("INSERT INTO patient (id, person_id, race, gender) VALUES (3001, 701, 'black', 'F')");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS patient_contact (" + "    id VARCHAR(20) PRIMARY KEY, "
                + "    LASTUPDATED TIMESTAMP,      " + "    patient_id VARCHAR(20),     "
                + "    person_id NUMERIC(10, 0),   " + ""
                + "    CONSTRAINT fk_patient_contact_person FOREIGN KEY (person_id)" + "        REFERENCES person(id) "
                + ");");

        jdbcTemplate.update("INSERT INTO patient_contact (" + "    id, LASTUPDATED, patient_id, person_id"
                + ") VALUES (" + "    '8001', CURRENT_TIMESTAMP, '3001', 701" + ");");
        jdbcTemplate.execute("INSERT INTO PATIENT_CONTACT (" + "    id, LASTUPDATED, patient_id, person_id"
                + ") VALUES (" + "    '8002', CURRENT_TIMESTAMP, '3001', 701" + ");");
    }

    @Test
    public void getAll_ShouldReturnAllPatientContacts() {
        patientContacts = patientContactService.getAll();
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("8002", patientContacts.get(1).getId());
    }

}
