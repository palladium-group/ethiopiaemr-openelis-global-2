package org.openelisglobal.sample;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.login.valueholder.UserSessionData;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;

public class SamplePatientEntryServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private PersonService personService;

    @Autowired
    private PatientService patientService;

    private PatientManagementUpdate patientManagementUpdate;

    @Before
    public void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/samplepatiententry.xml");
    }

    @Test
    public void verifyTestData() {
        Person provider = personService.get("4");
        assertNotNull("Provider person should exist in test data", provider);

        Organization org = organizationService.get("1");
        assertNotNull("Organization should exist in test data", org);
    }

    @Test
    public void persistData_shouldPersistCompleteSamplePatientData() throws Exception {

        Sample sample = sampleService.getSampleByAccessionNumber("EXISTING001");
        assertNotNull("Sample from dataset should exist", sample);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        sampleHumanService.getData(sampleHuman);

        assertNotNull("Sample should be linked to a patient", sampleHuman);
        String patientId = sampleHuman.getPatientId();
        assertNotNull("Sample should contain a patient ID", patientId);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        updateData.setSample(sample);
        updateData.setSampleHuman(sampleHuman); // Set the populated sampleHuman

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patientId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionData usd = new UserSessionData();
        request.getSession().setAttribute(IActionConstants.USER_SESSION_DATA, usd);

        PatientManagementUpdate patientUpdate = new PatientManagementUpdate();

        samplePatientEntryService.persistData(updateData, patientUpdate, patientInfo, new SamplePatientEntryForm(),
                request);

        Sample savedSample = sampleService.getSampleByAccessionNumber(sample.getAccessionNumber());
        assertNotNull("Sample should be persisted", savedSample);

        SampleHuman savedSampleHuman = new SampleHuman();
        savedSampleHuman.setSampleId(savedSample.getId());
        sampleHumanService.getData(savedSampleHuman);
        assertNotNull("Sample-human relationship should exist", savedSampleHuman);
        assertEquals("Patient ID should match", patientId, savedSampleHuman.getPatientId());

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(savedSample.getId());
        assertFalse("Sample items should be persisted", sampleItems.isEmpty());

        List<Analysis> analyses = analysisService.getAnalysesBySampleId(savedSample.getId());
        assertFalse("Analyses should be persisted", analyses.isEmpty());
    }

}
