package org.openelisglobal.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.openelisglobal.common.services.RequesterService.personService;

import java.sql.Date;
import java.util.List;
import org.dbunit.DatabaseUnitException;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.PatientManagementUpdate;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sample.valueholder.SampleAdditionalField;
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
    private SampleRequesterService sampleRequesterService;

    @Before
    public void setUp() throws Exception {

        executeDataSetWithStateManagement("testdata/samplepatiententry.xml");
    }

    @Test
    public void persistData_shouldPersistCompleteSamplePatientData() throws DatabaseUnitException {

        SamplePatientUpdateData updateData = createSamplePatientUpdateData();
        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientId("1");

        MockHttpServletRequest request = new MockHttpServletRequest();
        SamplePatientEntryForm form = new SamplePatientEntryForm();

        samplePatientEntryService.persistData(updateData, new PatientManagementUpdate(), patientInfo, form, request);

        Sample savedSample = sampleService.getSampleByAccessionNumber("TEST123");
        assertNotNull("Sample should be saved", savedSample);

        SampleHuman sampleHuman = sampleHumanService.getDataBySample(savedSample);
        assertNotNull("SampleHuman should be created", sampleHuman);
        assertEquals("Patient ID should match", "1", sampleHuman.getPatientId());

        List<SampleItem> sampleItems = sampleItemService.getSampleItemsBySampleId(savedSample.getId());
        assertEquals("Should have one sample item", 1, sampleItems.size());

        List<Analysis> analyses = analysisService.getAnalysesBySampleId(savedSample.getId());
        assertEquals("Should have one analysis", 1, analyses.size());
        assertEquals("Analysis status should be NotStarted",
                StatusService.getInstance().getStatusID(AnalysisStatus.NotStarted), analyses.get(0).getStatusId());

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId(savedSample.getId());
        assertEquals("Should have one requester", 1, requesters.size());
    }

    @Test
    public void persistSampleData_shouldPersistSampleWithAllRelatedEntities() throws DatabaseUnitException {

        SamplePatientUpdateData updateData = createSamplePatientUpdateData();

        samplePatientEntryService.persistSampleData(updateData);

        Sample savedSample = sampleService.getSampleByAccessionNumber("TEST123");
        assertNotNull(savedSample);

        List<SampleAdditionalField> fields = sampleService.getSampleAdditionalFieldsForSample(savedSample.getId());
        assertEquals("Should have one additional field", 1, fields.size());
    }

    @Test
    public void persistOrganizationData_shouldCreateNewOrganizationWithAddress() throws DatabaseUnitException {

        SamplePatientUpdateData updateData = new SamplePatientUpdateData();
        Organization newOrg = new Organization();
        newOrg.setName("New Test Org");
        newOrg.setShortName("NTO");
        updateData.setNewOrganization(newOrg);

        samplePatientEntryService.persistOrganizationData(updateData);

        List<Organization> orgs = organizationService.getOrganizationsByName("New Test Org");
        assertEquals(1, orgs.size());
        assertTrue(orgs.get(0).getOrganizationTypes().stream()
                .anyMatch(t -> t.getId().equals(TableIdService.getInstance().REFERRING_ORG_TYPE_ID)));
    }

    @Test
    public void persistRequesterData_shouldCreateProviderAndOrganizationRequesters() throws DatabaseUnitException {

        SamplePatientUpdateData updateData = createSamplePatientUpdateData();
        updateData.getSample().setId("1");
        updateData.setProviderPerson(personService.get("4"));

        samplePatientEntryService.persistRequesterData(updateData);

        List<SampleRequester> requesters = sampleRequesterService.getRequestersForSampleId("1");
        assertEquals(2, requesters.size());
    }

    private SamplePatientUpdateData createSamplePatientUpdateData() {
        SamplePatientUpdateData updateData = new SamplePatientUpdateData();

        Sample sample = new Sample();
        sample.setAccessionNumber("TEST123");
        sample.setEnteredDate(Date.valueOf("2024-06-15"));
        sample.setStatus("Active");
        updateData.setSample(sample);

        updateData.setPatientId("1");

        SampleAdditionalField field = new SampleAdditionalField();
        field.setFieldName("TestField");
        field.setFieldValue("TestValue");
        updateData.setSampleFields(List.of(field));

        return updateData;
    }
}
