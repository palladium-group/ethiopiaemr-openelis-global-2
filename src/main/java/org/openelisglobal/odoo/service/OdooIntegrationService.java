package org.openelisglobal.odoo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service class for integrating OpenELIS with Odoo for billing functionality.
 * This service handles the creation of invoices in Odoo when orders are created in OpenELIS.
 */
@Service
public class OdooIntegrationService {

    @Autowired
    private OdooClient odooClient;

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
            if (!odooClient.isConnected()) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "createInvoice", "Odoo client is not connected. Skipping invoice creation for sample: " + updateData.getAccessionNumber());
                return;
            }

            Map<String, Object> invoiceData = createInvoiceData(updateData);
            List<Map<String, Object>> dataParams = new ArrayList<>();
            dataParams.add(invoiceData);
            Integer invoiceId = odooClient.create("account.move", dataParams);
            
            LogEvent.logInfo(this.getClass().getSimpleName(), "createInvoice", 
                "Successfully created invoice in Odoo with ID: " + invoiceId + " for sample: " + updateData.getAccessionNumber());
                
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createInvoice", 
                "Error creating invoice in Odoo for sample " + updateData.getAccessionNumber() + ": " + e.getMessage());
            throw new OdooOperationException("Failed to create invoice in Odoo", e);
        }
    }

    /**
     * Creates the invoice data structure for Odoo.
     * 
     * @param updateData The sample data
     * @return Map containing the invoice data
     */
    private Map<String, Object> createInvoiceData(SamplePatientUpdateData updateData) {
        Map<String, Object> invoiceData = new HashMap<>();
        invoiceData.put("move_type", "out_invoice");
        invoiceData.put("partner_id", 1);
        invoiceData.put("invoice_date", java.time.LocalDate.now().toString());
        invoiceData.put("ref", "OpenELIS-" + updateData.getAccessionNumber());
        List<Map<String, Object>> invoiceLines = createInvoiceLines(updateData);
        invoiceData.put("invoice_line_ids", invoiceLines);
        return invoiceData;
    }

    /**
     * Creates invoice line items based on the tests in the sample.
     * 
     * @param updateData The sample data
     * @return List of invoice line data
     */
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
                        invoiceLines.add(invoiceLine);
                        
                        LogEvent.logInfo(this.getClass().getSimpleName(), "createInvoiceLines", 
                            "Added invoice line for test: " + testName + " with product: " + productName + " and price: " + price);
                    } else {
                        LogEvent.logWarn(this.getClass().getSimpleName(), "createInvoiceLines", 
                            "No Odoo product mapping found for test: " + testName);
                    }
                }
            }
        }
        
        return invoiceLines;
    }
}
