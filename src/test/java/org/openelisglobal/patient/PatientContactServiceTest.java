package org.openelisglobal.patient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class PatientContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientContactService patientContactService;

    private JdbcTemplate jdbcTemplate;
    private List<PatientContact> patientContacts;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Autowired
    public void setDataSource(@NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Before
    public void setUp() {

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS person ( id numeric(10,0) NOT NULL,"
                + "last_name character varying(255),    first_name character varying(255),"
                + "middle_name character varying(255),    multiple_unit character varying(255),"
                + "street_address character varying(255),    city character varying(255),"
                + "state character(2),    zip_code character(10),    country character varying(255),"
                + "work_phone character varying(255),    home_phone character varying(255),"
                + "cell_phone character varying(255),    fax character varying(255),"
                + "email character varying(255),    lastupdated timestamp(6) without time zone,"
                + "primary_phone character varying(255));");

        jdbcTemplate.update(
                "INSERT INTO person (id, last_name, first_name, email) VALUES (701, 'Cobby', 'Peters', 'cobpeters@peters.com')");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS clinlims.patient ( id numeric(10,0) NOT NULL,"
                + "person_id numeric(10,0) NOT NULL,    race character varying(5),"
                + "gender character varying(1),    birth_date timestamp without time zone,"
                + "epi_first_name character varying(25),    epi_middle_name character varying(25),"
                + "epi_last_name character varying(240),    birth_time timestamp without time zone,"
                + "death_date timestamp without time zone,    national_id character varying(255),"
                + "ethnicity character varying(1),    school_attend character varying(240),"
                + "medicare_id character varying(240),    medicaid_id character varying(240),"
                + "birth_place character varying(255),    lastupdated timestamp(6) without time zone,"
                + "external_id character varying(255),    chart_number character varying(20),"
                + "entered_birth_date character varying(10));");

        jdbcTemplate.update("INSERT INTO patient (id, person_id, race, gender) VALUES (3001, 701, 'black', 'F')");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS patient_contact ( id VARCHAR(20) PRIMARY KEY, "
                + "LASTUPDATED TIMESTAMP, patient_id VARCHAR(20), person_id NUMERIC(10, 0),"
                + "CONSTRAINT fk_patient_contact_person FOREIGN KEY (person_id) REFERENCES person(id) );");

        jdbcTemplate.update("INSERT INTO patient_contact (    id, lastupdated, patient_id, person_id) "
                + "VALUES ('8001', '2025-06-22 11:30:00', '3001', 701);");
        jdbcTemplate.execute("INSERT INTO PATIENT_CONTACT ( id, lastupdated, patient_id, person_id) "
                + "VALUES ( '8002', '2024-06-10 11:45:00', '3001', 701);");

        propertyValues = new HashMap<>();
        propertyValues.put("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));

        orderProperties = new ArrayList<>();
        orderProperties.add("patientId");
    }

    @Test
    public void getAll_ShouldReturnAllPatientContacts() {
        patientContacts = patientContactService.getAll();
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("8002", patientContacts.get(1).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingPatientContacts_UsingPropertyName() {
        patientContacts = patientContactService.getAllMatching("lastupdated", Timestamp.valueOf("2025-06-22 11:30:00"));
        assertNotNull(patientContacts);
        assertEquals(1, patientContacts.size());
        assertEquals("8001", patientContacts.get(0).getId());
    }

    @Test
    public void getAllMatching_ShouldReturnMatchingPatientContacts_UsingMap() {
        patientContacts = patientContactService.getAllMatching(propertyValues);
        assertNotNull(patientContacts);
        assertEquals(1, patientContacts.size());
        assertEquals("8001", patientContacts.get(0).getId());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedPatientContacts_UsingPropertyName() {
        patientContacts = patientContactService.getAllOrdered("patientId", true);
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("Cobby", patientContacts.get(1).getPerson().getLastName());
    }

    @Test
    public void getAllOrdered_ShouldReturnAllOrderedPatientContacts_UsingList() {
        orderProperties.add("patientId");
        patientContacts = patientContactService.getAllOrdered(orderProperties, true);
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("cobpeters@peters.com", patientContacts.get(0).getPerson().getEmail());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedPatientContacts_Using() {
        patientContacts = patientContactService.getAllMatchingOrdered("patientId", 3001, "patientId", false);
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("701", patientContacts.get(0).getPerson().getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedPatientContacts_UsingList() {
        patientContacts = patientContactService.getAllMatchingOrdered("patientId", 3001, orderProperties, true);
        assertNotNull(patientContacts);
        assertEquals(2, patientContacts.size());
        assertEquals("8001", patientContacts.get(0).getId());
    }

    @Test
    public void getAllMatchingOrdered_ShouldReturnMatchingOrderedPatientContacts_UsingMap() {
        patientContacts = patientContactService.getAllMatchingOrdered(propertyValues, "lastupdated", true);
        assertNotNull(patientContacts);
        assertEquals(1, patientContacts.size());
        assertEquals("8001", patientContacts.get(0).getId());
    }

    @Test
    public void getPage_ShouldReturnAPageOfResults_UsingPageNumber() {
        patientContacts = patientContactService.getPage(1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValue() {
        patientContacts = patientContactService.getMatchingPage("person", 701, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingPage_ShouldReturnAPageOfResults_UsingMap() {
        patientContacts = patientContactService.getMatchingPage(propertyValues, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingOrderProperty() {
        patientContacts = patientContactService.getOrderedPage("patientId", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getOrderedPage_ShouldReturnAPageOfResults_UsingList() {
        patientContacts = patientContactService.getOrderedPage(orderProperties, false, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingPropertyNameAndValue() {
        patientContacts = patientContactService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2025-06-22 11:30:00"), "person", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingList() {
        patientContacts = patientContactService.getMatchingOrderedPage("lastupdated",
                Timestamp.valueOf("2024-06-10 11:45:00"), orderProperties, false, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingMap() {
        patientContacts = patientContactService.getMatchingOrderedPage(propertyValues, "patientId", true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void getMatchingOrderedPage_ShouldReturnAPageOfResults_UsingMapAndList() {
        patientContacts = patientContactService.getMatchingOrderedPage(propertyValues, orderProperties, true, 1);
        NUMBER_OF_PAGES = Integer
                .parseInt(ConfigurationProperties.getInstance().getPropertyValue("page.defaultPageSize"));
        assertTrue(NUMBER_OF_PAGES >= patientContacts.size());
    }

    @Test
    public void deletePatientContact_ShouldDeleteAPatientContactPassedAsParameter() {
        PatientContact patientContact = patientContactService.getAll().get(0);
        patientContactService.delete(patientContact);
        List<PatientContact> deletedPatientContact = patientContactService.getAll();
        assertEquals(1, deletedPatientContact.size());
    }

    @Test
    public void deleteAllPatientContacts_ShouldDeleteAllPatientContacts() {
        patientContactService.deleteAll(patientContactService.getAll());
        List<PatientContact> delectedPatientContact = patientContactService.getAll();
        assertNotNull(delectedPatientContact);
        assertEquals(0, delectedPatientContact.size());
    }
}
