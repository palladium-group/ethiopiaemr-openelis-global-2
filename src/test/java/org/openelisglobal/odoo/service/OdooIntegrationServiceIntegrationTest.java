package org.openelisglobal.odoo.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;

public class OdooIntegrationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private TestProductMapping testProductMapping;

    private SamplePatientUpdateData updateData;

    @Before
    public void setUp() {
        Sample sample = new Sample();
        sample.setAccessionNumber("TEST-001");
        updateData = new SamplePatientUpdateData("testUser");
        updateData.setSample(sample);
        updateData.setAccessionNumber("TEST-001");
        Mockito.reset(odooConnection, testProductMapping);
    }

    @Test
    public void createInvoice_shouldCreateInvoiceWithMappedTests() {
        mockTestMapping("CBC", "Complete Blood Count", 150.0);
        when(odooConnection.create(anyString(), any())).thenReturn(123);

        updateData.setSampleItemsTests(createSampleTestCollection("CBC"));

        odooIntegrationService.createInvoice(updateData);

        Map<String, Object> invoice = captureSingleInvoice();

        assertEquals("out_invoice", invoice.get("move_type"));
        assertEquals(1, invoice.get("partner_id"));
        assertEquals("OE-TEST-001", invoice.get("ref"));

        List<Object> lines = getInvoiceLines(invoice);
        assertEquals(1, lines.size());

        Map<String, Object> lineData = getInvoiceLineData(lines, 0);
        assertEquals("Complete Blood Count", lineData.get("name"));
        assertEquals(1.0, lineData.get("quantity"));
        assertEquals(150.0, lineData.get("price_unit"));
        assertEquals(1, lineData.get("account_id"));
    }

    @Test
    public void createInvoice_shouldCreateInvoiceWithDefaultValues_whenTestsAreNotMapped() {
        when(testProductMapping.hasValidMapping("CBC")).thenReturn(false);
        updateData.setSampleItemsTests(createSampleTestCollection("CBC"));
        when(odooConnection.create(anyString(), any())).thenReturn(123);

        odooIntegrationService.createInvoice(updateData);

        Map<String, Object> invoice = captureSingleInvoice();
        List<Object> lines = getInvoiceLines(invoice);
        assertEquals(1, lines.size());

        Map<String, Object> lineData = getInvoiceLineData(lines, 0);
        assertEquals("CBC", lineData.get("name"));
        assertEquals(100.0, lineData.get("price_unit"));
    }

    @Test
    public void createInvoice_shouldCreateMultipleInvoiceLines_forMultipleTests() {
        mockTestMapping("CBC", "Complete Blood Count", 150.0);
        mockTestMapping("Glucose", "Blood Glucose Test", 45.0);
        updateData.setSampleItemsTests(createSampleTestCollection("CBC", "Glucose"));
        when(odooConnection.create(anyString(), any())).thenReturn(123);

        odooIntegrationService.createInvoice(updateData);

        Map<String, Object> invoice = captureSingleInvoice();
        List<Object> lines = getInvoiceLines(invoice);
        assertEquals(2, lines.size());

        Map<String, Object> firstLine = getInvoiceLineData(lines, 0);
        assertEquals("Complete Blood Count", firstLine.get("name"));
        assertEquals(150.0, firstLine.get("price_unit"));

        Map<String, Object> secondLine = getInvoiceLineData(lines, 1);
        assertEquals("Blood Glucose Test", secondLine.get("name"));
        assertEquals(45.0, secondLine.get("price_unit"));
    }

    @Test
    public void createInvoice_shouldHandleEmptySampleItemsTests() {
        updateData.setSampleItemsTests(new ArrayList<>());
        when(odooConnection.create(anyString(), any())).thenReturn(123);

        odooIntegrationService.createInvoice(updateData);

        Map<String, Object> invoice = captureSingleInvoice();
        List<Object> lines = getInvoiceLines(invoice);
        assertEquals(0, lines.size());
    }

    private void mockTestMapping(String testName, String productName, Double price) {
        when(testProductMapping.hasValidMapping(testName)).thenReturn(true);
        when(testProductMapping.getProductName(testName)).thenReturn(productName);
        when(testProductMapping.getPrice(testName)).thenReturn(price);
    }

    private List<SampleTestCollection> createSampleTestCollection(String... testNames) {
        List<org.openelisglobal.test.valueholder.Test> tests = new ArrayList<>();
        for (String name : testNames) {
            org.openelisglobal.test.valueholder.Test test = mock(org.openelisglobal.test.valueholder.Test.class);
            when(test.getLocalizedName()).thenReturn(name);
            tests.add(test);
        }
        SampleTestCollection collection = new SampleTestCollection(null, new ArrayList<>(), null, null, null, null,
                null);
        collection.tests = tests;
        return List.of(collection);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> captureSingleInvoice() {
        ArgumentCaptor<List<Map<String, Object>>> captor = ArgumentCaptor.forClass(List.class);
        verify(odooConnection, Mockito.times(1)).create(eq("account.move"), captor.capture());
        return captor.getValue().get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Object> getInvoiceLines(Map<String, Object> invoice) {
        return (List<Object>) invoice.get("invoice_line_ids");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getInvoiceLineData(List<Object> lines, int index) {
        List<Object> line = (List<Object>) lines.get(index);
        return (Map<String, Object>) line.get(2);
    }
}
