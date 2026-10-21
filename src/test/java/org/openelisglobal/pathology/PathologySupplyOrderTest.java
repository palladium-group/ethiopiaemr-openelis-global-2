package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestClient;
import org.openelisglobal.dataexchange.openmrs.rest.OpenMrsRestException;
import org.openelisglobal.program.controller.supply.ProgramSupplyRequestForm;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderException;
import org.openelisglobal.program.service.supply.ProgramSupplyOrderService;
import org.openelisglobal.program.valueholder.supply.ProgramSupplyRequestDisplayItem;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Supply orders from a pathology case through the service, DAO and Liquibase
 * tables; only the OpenMRS REST client is mocked.
 */
public class PathologySupplyOrderTest extends BaseWebContextSensitiveTest {

    private static final String ORDER_UUID = "0773d273-e32c-4fc2-85e5-f82bdbedb598";
    private static final String PATIENT_UUID = "5b4b8a2c-1111-4c3e-9a55-2f0d1e6c7a10";
    private static final String VISIT_UUID = "a1c6d0f2-2222-4b7e-8e1f-3c9a0d4b5e21";

    @Autowired
    private ProgramSupplyOrderService supplyOrderService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private DataSource dataSource;

    private final ObjectMapper mapper = new ObjectMapper();
    private JdbcTemplate jdbcTemplate;
    private Object serviceTarget;
    private Object originalClient;
    private Object originalConfig;
    private OpenMrsRestClient client;
    private ConfigurationProperties config;

    @Before
    public void init() throws Exception {
        executeDataSetWithStateManagement("testdata/pathology-sample.xml");
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("UPDATE pathology_sample SET status = 'GROSSING' WHERE id = 2");
        jdbcTemplate.update("UPDATE sample SET referring_id = ? WHERE id = 2", ORDER_UUID);

        serviceTarget = AopTestUtils.getUltimateTargetObject(supplyOrderService);
        originalClient = ReflectionTestUtils.getField(serviceTarget, "openMrsRestClient");
        originalConfig = ReflectionTestUtils.getField(serviceTarget, "configurationProperties");
        client = mock(OpenMrsRestClient.class);
        config = mock(ConfigurationProperties.class);
        ReflectionTestUtils.setField(serviceTarget, "openMrsRestClient", client);
        ReflectionTestUtils.setField(serviceTarget, "configurationProperties", config);
        ReflectionTestUtils.setField(serviceTarget, "ordererProviderUuid", "f9badd80-ab76-11e2-9e96-0800200c9a66");

        when(config.isPropertyValueEqual(Property.PATHOLOGY_SUPPLY_ORDER_ENABLED, "true")).thenReturn(true);
        when(client.isConfigured()).thenReturn(true);
        when(client.get(startsWith("order/"))).thenReturn(json("{\"patient\":{\"uuid\":\"" + PATIENT_UUID + "\"}}"));
        when(client.get(startsWith("visit?patient="))).thenReturn(
                json("{\"results\":[{\"uuid\":\"" + VISIT_UUID + "\",\"location\":{\"uuid\":\"lab-location\"}}]}"));

    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(serviceTarget, "openMrsRestClient", originalClient);
        ReflectionTestUtils.setField(serviceTarget, "configurationProperties", originalConfig);
        ReflectionTestUtils.setField(serviceTarget, "ordererProviderUuid", "");
    }

    @Test
    public void order_isCreatedInOpenMrsAndListedOnTheCase() throws Exception {
        ProgramSupplyRequestDisplayItem created = supplyOrderService.create(sampleService.get("2"), false, form(),
                "1001");

        assertEquals("SENT", created.getStatus());
        verify(client).post(eq("encounter"), any());
        assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM program_supply_request WHERE sample_id = 2 AND status = 'SENT'", Integer.class));
        assertEquals("Gloves", jdbcTemplate.queryForObject(
                "SELECT concept_name FROM program_supply_request_item WHERE quantity = 2", String.class));

        List<ProgramSupplyRequestDisplayItem> list = supplyOrderService.listForSample(sampleService.get("2"));
        assertEquals(1, list.size());
        assertEquals("Gloves", list.get(0).getItems().get(0).getConceptName());
        assertEquals("Box", list.get(0).getItems().get(0).getQuantityUnitsName());
        assertNotNull(list.get(0).getRequestedBy());
        assertNotNull(list.get(0).getRequestedAt());
    }

    @Test
    public void failedOrder_isKeptAndCanBeRetried() throws Exception {
        when(client.post(eq("encounter"), any())).thenThrow(new OpenMrsRestException("OpenMRS is unreachable", null));
        ProgramSupplyRequestDisplayItem failed = supplyOrderService.create(sampleService.get("2"), false, form(),
                "1001");
        assertEquals("FAILED", failed.getStatus());

        when(client.post(eq("encounter"), any())).thenReturn(json("{}"));
        ProgramSupplyRequestDisplayItem retried = supplyOrderService.retry(failed.getId(), "2", false, "1001");

        assertEquals("SENT", retried.getStatus());
        assertEquals(Integer.valueOf(1),
                jdbcTemplate.queryForObject("SELECT count(*) FROM program_supply_request", Integer.class));
    }

    @Test
    public void daguOutcome_isStoredOnTheRequest() throws Exception {
        ProgramSupplyRequestDisplayItem created = supplyOrderService.create(sampleService.get("2"), false, form(),
                "1001");
        String encounterUuid = jdbcTemplate.queryForObject(
                "SELECT encounter_uuid FROM program_supply_request WHERE id = ?", String.class, created.getId());
        when(client.get(startsWith("ethiopiaemrcustommodule/prescriptionOutbox/status"))).thenReturn(
                json("{\"results\":[{\"encounterUuid\":\"" + encounterUuid + "\",\"outcomeStatus\":\"SUCCESS\"}]}"));

        supplyOrderService.listForSample(sampleService.get("2"));

        assertEquals("SUCCESS", jdbcTemplate.queryForObject(
                "SELECT dagu_status FROM program_supply_request WHERE id = ?", String.class, created.getId()));
    }

    @Test
    public void order_isRefusedWithoutSavingWhenPatientHasNoActiveVisit() throws Exception {
        when(client.get(startsWith("visit?patient="))).thenReturn(json("{\"results\":[]}"));

        try {
            supplyOrderService.create(sampleService.get("2"), false, form(), "1001");
            fail("expected NO_ACTIVE_VISIT");
        } catch (ProgramSupplyOrderException e) {
            assertEquals(ProgramSupplyOrderException.Code.NO_ACTIVE_VISIT, e.getCode());
        }
        verify(client, never()).post(anyString(), any());
        assertEquals(Integer.valueOf(0),
                jdbcTemplate.queryForObject("SELECT count(*) FROM program_supply_request", Integer.class));
    }

    private ProgramSupplyRequestForm form() {
        ProgramSupplyRequestForm.Item line = new ProgramSupplyRequestForm.Item();
        line.setConceptUuid("c1d2e3f4-3333-4a2b-8c3d-0e1f2a3b4c5d");
        line.setConceptName("Gloves");
        line.setQuantity(2);
        line.setQuantityUnitsUuid("1513AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        line.setQuantityUnitsName("Box");
        line.setUrgency("ROUTINE");
        line.setInstructions("size M");
        ProgramSupplyRequestForm form = new ProgramSupplyRequestForm();
        form.getItems().add(line);
        return form;
    }

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }
}
