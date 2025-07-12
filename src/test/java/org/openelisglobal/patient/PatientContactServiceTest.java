package org.openelisglobal.patient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.patient.service.PatientContactService;
import org.openelisglobal.patient.valueholder.PatientContact;
import org.springframework.beans.factory.annotation.Autowired;

public class PatientContactServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private PatientContactService patientContactService;

    private List<PatientContact> patientContacts;
    private Map<String, Object> propertyValues;
    private List<String> orderProperties;
    private static int NUMBER_OF_PAGES = 0;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/patient_contact.xml");

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
