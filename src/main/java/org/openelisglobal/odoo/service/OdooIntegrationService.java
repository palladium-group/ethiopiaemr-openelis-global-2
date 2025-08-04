package org.openelisglobal.odoo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.odoo.client.OdooConnection;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for integrating OpenELIS with Odoo for billing functionality.
 * This service handles the creation of invoices in Odoo when orders are created
 * in OpenELIS.
 */
@Slf4j
@Service
public class OdooIntegrationService {

    @Autowired
    private OdooConnection odooConnection;

    @Autowired
    private TestProductMapping testProductMapping;

    /**
     * Creates an invoice in Odoo for the given sample data.
     * 
     * @param updateData The sample data containing order information
     * @throws OdooOperationException if there's an error creating the invoice
     */
    public void createInvoice(SamplePatientUpdateData updateData) {
        try {
            Map<String, Object> invoiceData = createInvoiceData(updateData);
            Integer invoiceId = odooConnection.create("account.move", List.of(invoiceData));
            if (invoiceId == null) {
                throw new OdooOperationException(
                        "Odoo returned null invoice ID for sample: " + updateData.getAccessionNumber());
            }
            log.info("Successfully created invoice in Odoo with ID: {} for sample: {}", invoiceId,
                    updateData.getAccessionNumber());
        } catch (Exception e) {
            log.error("Error creating invoice in Odoo for sample {}: {}", updateData.getAccessionNumber(),
                    e.getMessage(), e);
            throw new OdooOperationException("Failed to create invoice in Odoo", e);
        }
    }

    private Map<String, Object> createInvoiceData(SamplePatientUpdateData updateData) {
        Map<String, Object> invoiceData = new HashMap<>();
        invoiceData.put("move_type", "out_invoice");
        invoiceData.put("partner_id", 1);
        invoiceData.put("invoice_date", java.time.LocalDate.now().toString());
        invoiceData.put("ref", "OpenELIS-" + updateData.getAccessionNumber());
        List<Object> formattedInvoiceLines = new ArrayList<>();
        List<Map<String, Object>> invoiceLines = createInvoiceLines(updateData);
        for (Map<String, Object> line : invoiceLines) {
            formattedInvoiceLines.add(List.of(0, 0, line));
        }
        invoiceData.put("invoice_line_ids", formattedInvoiceLines);
        return invoiceData;
    }

    private List<Map<String, Object>> createInvoiceLines(SamplePatientUpdateData updateData) {
        List<Map<String, Object>> invoiceLines = new ArrayList<>();
        if (updateData.getSampleItemsTests() != null) {
            for (SampleTestCollection sampleTest : updateData.getSampleItemsTests()) {
                for (Test test : sampleTest.tests) {
                    String testName = test.getLocalizedName();
                    if (testProductMapping.hasValidMapping(testName)) {
                        String productName = testProductMapping.getProductName(testName);
                        Double price = testProductMapping.getPrice(testName);

                        Map<String, Object> invoiceLine = new HashMap<>();
                        invoiceLine.put("name", productName);
                        invoiceLine.put("quantity", 1.0);
                        invoiceLine.put("price_unit", price != null ? price : 100.0);
                        invoiceLine.put("account_id", 1);
                        invoiceLines.add(invoiceLine);
                        log.info("Added invoice line for test: {} with product: {} and price: {}", testName,
                                productName, price);
                    } else {
                        log.warn("No Odoo product mapping found for test: {}", testName);
                        Map<String, Object> invoiceLine = new HashMap<>();
                        invoiceLine.put("name", testName);
                        invoiceLine.put("quantity", 1.0);
                        invoiceLine.put("price_unit", 100.0);
                        invoiceLine.put("account_id", 1);
                        invoiceLines.add(invoiceLine);
                        log.info("Added default invoice line for unmapped test: {} with price: {}", testName, 100.0);
                    }
                }
            }
        }
        return invoiceLines;
    }
}
