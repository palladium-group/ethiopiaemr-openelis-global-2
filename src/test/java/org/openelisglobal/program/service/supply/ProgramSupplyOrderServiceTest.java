package org.openelisglobal.program.service.supply;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestClient;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestException;
import org.openelisglobal.program.bean.SupplyCatalogOption;
import org.openelisglobal.program.controller.supply.ProgramSupplyRequestForm;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException.Code;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest.DaguStatus;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequest.Status;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestDisplayItem;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestItem;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProgramSupplyOrderServiceTest {

    private static final String ORDER_UUID = "0773d273-e32c-4fc2-85e5-f82bdbedb598";
    private static final String PATIENT_UUID = "5b4b8a2c-1111-4c3e-9a55-2f0d1e6c7a10";
    private static final String VISIT_UUID = "a1c6d0f2-2222-4b7e-8e1f-3c9a0d4b5e21";
    private static final String LOCATION_UUID = "44c3efb0-2583-4c80-a79e-1f756a03c0a1";
    private static final String PROVIDER_UUID = "f9badd80-ab76-11e2-9e96-0800200c9a66";
    private static final String GLOVES_UUID = "c1d2e3f4-3333-4a2b-8c3d-0e1f2a3b4c5d";
    private static final String BOX_UUID = "1513AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private OpenMrsRestClient openMrsRestClient;
    @Mock
    private ProgramSupplyRequestService supplyRequestService;
    @Mock
    private SystemUserService systemUserService;
    @Mock
    private ConfigurationProperties configurationProperties;

    @InjectMocks
    private ProgramSupplyOrderService service;

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "ordererProviderUuid", PROVIDER_UUID);
        ReflectionTestUtils.setField(service, "encounterTypeUuid", "39da3525-afe4-45ff-8977-c53b7b359158");
        ReflectionTestUtils.setField(service, "careSettingUuid", "6f0c9a92-6f24-11e3-af88-005056821db0");
        ReflectionTestUtils.setField(service, "conceptClassUuid", "0dcf23d4-3008-4d8e-b12c-4ec95d1cfd97");
        ReflectionTestUtils.setField(service, "quantityUnitsConceptSetUuid", "162402AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        ReflectionTestUtils.setField(service, "fallbackLocationUuid", "");

        when(configurationProperties.isPropertyValueEqual(Property.PATHOLOGY_SUPPLY_ORDER_ENABLED, "true"))
                .thenReturn(true);
        when(openMrsRestClient.isConfigured()).thenReturn(true);
        when(systemUserService.get("1001")).thenReturn(user());
        when(supplyRequestService.save(any())).thenAnswer(i -> i.getArgument(0));
        when(supplyRequestService.update(any())).thenAnswer(i -> i.getArgument(0));

        when(openMrsRestClient.get(startsWith("order/" + ORDER_UUID)))
                .thenReturn(json("{\"uuid\":\"" + ORDER_UUID + "\",\"patient\":{\"uuid\":\"" + PATIENT_UUID + "\"}}"));
        when(openMrsRestClient.get(startsWith("visit?patient=" + PATIENT_UUID))).thenReturn(json("{\"results\":[{"
                + "\"uuid\":\"" + VISIT_UUID + "\",\"location\":{\"uuid\":\"" + LOCATION_UUID + "\"}}]}"));
    }

    @Test
    public void create_postsOneOrderEncounterLikeTheO3Basket() {
        ProgramSupplyRequestDisplayItem result = service.create(sample(ORDER_UUID), false, form(2, "STAT", "for FNA"),
                "1001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(openMrsRestClient).post(eq("encounter"), payload.capture());
        Map<String, Object> encounter = payload.getValue();
        assertEquals(PATIENT_UUID, encounter.get("patient"));
        assertEquals("39da3525-afe4-45ff-8977-c53b7b359158", encounter.get("encounterType"));
        assertEquals(VISIT_UUID, encounter.get("visit"));
        assertEquals(LOCATION_UUID, encounter.get("location"));
        assertTrue(((String) encounter.get("uuid")).matches("[0-9a-f-]{36}"));

        @SuppressWarnings("unchecked")
        Map<String, Object> order = ((List<Map<String, Object>>) encounter.get("orders")).get(0);
        assertEquals("medicalsupplyorder", order.get("type"));
        assertEquals("NEW", order.get("action"));
        assertEquals(PROVIDER_UUID, order.get("orderer"));
        assertEquals(GLOVES_UUID, order.get("concept"));
        assertEquals(2, order.get("quantity"));
        assertEquals(BOX_UUID, order.get("quantityUnits"));
        assertEquals("STAT", order.get("urgency"));
        assertEquals("for FNA (Requested via OpenELIS by Abebe Kebede, lab no. DEV0126000000000123)",
                order.get("instructions"));

        assertEquals("SENT", result.getStatus());
        assertEquals("Gloves", result.getItems().get(0).getConceptName());
    }

    @Test
    public void create_savesRequestBeforeCallingOpenMrs_andKeepsItFailedWhenRejected() {
        when(openMrsRestClient.post(eq("encounter"), any()))
                .thenThrow(new OpenMrsRestException(400, "Invalid concept"));

        ProgramSupplyRequestDisplayItem result = service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null),
                "1001");

        verify(supplyRequestService).save(any(ProgramSupplyRequest.class));
        assertEquals("FAILED", result.getStatus());
        assertEquals("Invalid concept", result.getFailureReason());
    }

    @Test
    public void create_rejectsCaseEnteredInOpenElis() {
        assertRejected(() -> service.create(sample(" "), false, form(1, "ROUTINE", null), "1001"),
                Code.NO_OPENMRS_ORDER);
    }

    @Test
    public void create_blocksWhenPatientHasNoActiveVisit() {
        when(openMrsRestClient.get(startsWith("visit?patient="))).thenReturn(json("{\"results\":[]}"));

        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001"),
                Code.NO_ACTIVE_VISIT);
        verify(supplyRequestService, never()).save(any());
        verify(openMrsRestClient, never()).post(anyString(), any());
    }

    @Test
    public void create_usesConfiguredLocationWhenVisitHasNone() {
        ReflectionTestUtils.setField(service, "fallbackLocationUuid", "lab-location");
        when(openMrsRestClient.get(startsWith("visit?patient=")))
                .thenReturn(json("{\"results\":[{\"uuid\":\"" + VISIT_UUID + "\"}]}"));

        service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(openMrsRestClient).post(eq("encounter"), payload.capture());
        assertEquals("lab-location", payload.getValue().get("location"));
    }

    @Test
    public void create_reportsOpenMrsUnavailableDuringLookup() {
        when(openMrsRestClient.get(startsWith("order/"))).thenThrow(new OpenMrsRestException("timeout", null));

        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001"),
                Code.OPENMRS_UNAVAILABLE);
        verify(supplyRequestService, never()).save(any());
    }

    @Test
    public void create_reportsOrderMissingInOpenMrs() {
        when(openMrsRestClient.get(startsWith("order/"))).thenReturn(null);

        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001"),
                Code.ORDER_NOT_FOUND);
    }

    @Test
    public void create_rejectsInvalidLines() {
        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(0, "ROUTINE", null), "1001"),
                Code.INVALID_ITEMS);
        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ON_SCHEDULED_DATE", null), "1001"),
                Code.INVALID_ITEMS);
        assertRejected(() -> service.create(sample(ORDER_UUID), false, new ProgramSupplyRequestForm(), "1001"),
                Code.INVALID_ITEMS);
        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", "x".repeat(501)), "1001"),
                Code.INVALID_ITEMS);
    }

    @Test
    public void create_rejectsCompletedCase() {
        assertRejected(() -> service.create(sample(ORDER_UUID), true, form(1, "ROUTINE", null), "1001"),
                Code.CASE_CLOSED);
    }

    @Test
    public void create_refusesWhenDisabledOrNotConfigured() {
        when(configurationProperties.isPropertyValueEqual(Property.PATHOLOGY_SUPPLY_ORDER_ENABLED, "true"))
                .thenReturn(false);
        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001"),
                Code.NOT_ENABLED);

        when(configurationProperties.isPropertyValueEqual(Property.PATHOLOGY_SUPPLY_ORDER_ENABLED, "true"))
                .thenReturn(true);
        ReflectionTestUtils.setField(service, "ordererProviderUuid", "");
        assertRejected(() -> service.create(sample(ORDER_UUID), false, form(1, "ROUTINE", null), "1001"),
                Code.NOT_CONFIGURED);
    }

    @Test
    public void retry_marksSentWithoutReposting_whenOpenMrsAlreadyHasTheEncounter() {
        ProgramSupplyRequest failed = failedRequest();
        when(supplyRequestService.get(7)).thenReturn(failed);
        when(openMrsRestClient.get(startsWith("encounter/" + failed.getEncounterUuid())))
                .thenReturn(json("{\"uuid\":\"" + failed.getEncounterUuid() + "\"}"));

        ProgramSupplyRequestDisplayItem result = service.retry(7, "123", false, "1001");

        assertEquals("SENT", result.getStatus());
        assertNull(result.getFailureReason());
        verify(openMrsRestClient, never()).post(anyString(), any());
    }

    @Test
    public void retry_resendsWithTheSameEncounterUuid() {
        ProgramSupplyRequest failed = failedRequest();
        when(supplyRequestService.get(7)).thenReturn(failed);
        when(openMrsRestClient.get(startsWith("encounter/"))).thenReturn(null);

        ProgramSupplyRequestDisplayItem result = service.retry(7, "123", false, "1001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(openMrsRestClient).post(eq("encounter"), payload.capture());
        assertEquals(failed.getEncounterUuid(), payload.getValue().get("uuid"));
        assertEquals("SENT", result.getStatus());
    }

    @Test
    public void retry_rejectsRequestOfAnotherCase() {
        when(supplyRequestService.get(7)).thenReturn(failedRequest());

        assertRejected(() -> service.retry(7, "999", false, "1001"), Code.NOT_FOUND);
    }

    @Test
    public void listForSample_recordsDaguOutcomeReportedToOpenMrs() {
        ProgramSupplyRequest sent = failedRequest();
        sent.setStatus(Status.SENT);
        when(supplyRequestService.getBySampleId("123")).thenReturn(List.of(sent));
        when(openMrsRestClient.get(startsWith("ethiopiaemrcustommodule/prescriptionOutbox/status?encounters=")))
                .thenReturn(json("{\"results\":[{\"encounterUuid\":\"" + sent.getEncounterUuid()
                        + "\",\"outcomeStatus\":\"FAILED\",\"reason\":\"Item not in Dagu catalog\"}]}"));

        ProgramSupplyRequestDisplayItem item = service.listForSample(sample(ORDER_UUID)).get(0);

        assertEquals("FAILED", item.getDaguStatus());
        assertEquals("Item not in Dagu catalog", item.getDaguReason());
        verify(supplyRequestService).update(sent);
        assertEquals(DaguStatus.FAILED, sent.getDaguStatus());
    }

    @Test
    public void listForSample_stillListsWhenOpenMrsIsDown() {
        ProgramSupplyRequest sent = failedRequest();
        sent.setStatus(Status.SENT);
        when(supplyRequestService.getBySampleId("123")).thenReturn(List.of(sent));
        when(openMrsRestClient.get(anyString())).thenThrow(new OpenMrsRestException("down", null));

        List<ProgramSupplyRequestDisplayItem> items = service.listForSample(sample(ORDER_UUID));

        assertEquals(1, items.size());
        assertNull(items.get(0).getDaguStatus());
    }

    @Test
    public void searchCatalog_returnsDistinctSupplyConcepts() {
        when(openMrsRestClient.get(startsWith("conceptsearch?conceptClasses=0dcf23d4"))).thenReturn(json(
                "{\"results\":[{\"concept\":{\"uuid\":\"" + GLOVES_UUID + "\",\"display\":\"Gloves\"},\"display\":\"Gloves\"},"
                        + "{\"concept\":{\"uuid\":\"" + GLOVES_UUID + "\",\"display\":\"Gloves\"},\"display\":\"Glove\"}]}"));

        List<SupplyCatalogOption> options = service.searchCatalog("glo");

        assertEquals(1, options.size());
        assertEquals(GLOVES_UUID, options.get(0).getUuid());
        assertEquals("Gloves", options.get(0).getDisplay());
        assertTrue(service.searchCatalog("g").isEmpty());
    }

    private void assertRejected(Runnable call, Code expected) {
        try {
            call.run();
            fail("expected " + expected);
        } catch (ProgramSupplyOrderException e) {
            assertEquals(expected, e.getCode());
        }
    }

    private ProgramSupplyRequestForm form(int quantity, String urgency, String instructions) {
        ProgramSupplyRequestForm.Item line = new ProgramSupplyRequestForm.Item();
        line.setConceptUuid(GLOVES_UUID);
        line.setConceptName("Gloves");
        line.setQuantity(quantity);
        line.setQuantityUnitsUuid(BOX_UUID);
        line.setQuantityUnitsName("Box");
        line.setUrgency(urgency);
        line.setInstructions(instructions);
        ProgramSupplyRequestForm form = new ProgramSupplyRequestForm();
        form.getItems().add(line);
        return form;
    }

    private ProgramSupplyRequest failedRequest() {
        ProgramSupplyRequest request = new ProgramSupplyRequest();
        request.setId(7);
        request.setSample(sample(ORDER_UUID));
        request.setEncounterUuid("3f1b2c4d-5555-4e6f-8a9b-0c1d2e3f4a5b");
        request.setStatus(Status.FAILED);
        request.setFailureReason("OpenMRS is unreachable");
        request.setRequestedBy(user());
        ProgramSupplyRequestItem item = new ProgramSupplyRequestItem();
        item.setConceptUuid(GLOVES_UUID);
        item.setConceptName("Gloves");
        item.setQuantity(1);
        item.setUrgency("ROUTINE");
        request.addItem(item);
        return request;
    }

    private Sample sample(String referringId) {
        Sample sample = new Sample();
        sample.setId("123");
        sample.setAccessionNumber("DEV0126000000000123");
        sample.setReferringId(referringId);
        return sample;
    }

    private SystemUser user() {
        SystemUser user = new SystemUser();
        user.setId("1001");
        user.setFirstName("Abebe");
        user.setLastName("Kebede");
        return user;
    }

    private JsonNode json(String value) {
        try {
            return mapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
