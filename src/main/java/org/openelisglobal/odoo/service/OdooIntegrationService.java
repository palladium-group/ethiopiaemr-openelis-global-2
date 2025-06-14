package org.openelisglobal.odoo.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.client.OdooClient;
import org.openelisglobal.odoo.config.TestProductMapping;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Service class for handling Odoo integration operations. This is a simplified
 * version for the MVP that focuses on invoice creation.
 */
@Service
public class OdooIntegrationService {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private TestProductMapping testProductMapping;

    /**
     * Creates an invoice in Odoo based on the sample data.
     *
     * @param updateData The sample update data containing test information
     * @throws OdooOperationException if there is an error creating the invoice
     */
    @Transactional
    public void createInvoice(SamplePatientUpdateData updateData) {
        Assert.notNull(updateData, "SamplePatientUpdateData cannot be null");
        Assert.hasText(updateData.getAccessionNumber(), "Accession number cannot be empty");
        Assert.notNull(updateData.getSample(), "Sample cannot be null");
        Assert.notNull(updateData.getSample().getCollectionDate(), "Collection date cannot be null");

        try {
            String accessionNumber = updateData.getAccessionNumber();
            Map<String, Object> invoiceData = new HashMap<>();

            // Set basic invoice information
            invoiceData.put("name", "INV-" + accessionNumber);
            invoiceData.put("ref", accessionNumber);
            invoiceData.put("invoice_date", updateData.getSample().getCollectionDate());
            invoiceData.put("move_type", "out_invoice"); // Customer Invoice
            invoiceData.put("partner_id", 1); // Default customer ID

            // Get the list of tests from the sample
            List<String> testIds = updateData.getSampleItemsTests().stream()
                    .flatMap(collection -> collection.tests.stream()).map(Test::getId).collect(Collectors.toList());

            if (testIds.isEmpty()) {
                throw new OdooOperationException("No tests found for sample: " + accessionNumber);
            }

            // Create invoice lines
            List<Map<String, Object>> invoiceLines = new ArrayList<>();
            for (String testId : testIds) {
                Integer productId = testProductMapping.getProductId(testId);
                Double price = testProductMapping.getPrice(testId);

                if (productId != null && price != null) {
                    Map<String, Object> invoiceLine = new HashMap<>();
                    invoiceLine.put("product_id", productId);
                    invoiceLine.put("price_unit", price);
                    invoiceLine.put("quantity", 1);
                    invoiceLine.put("name", "Test: " + testId);
                    invoiceLines.add(invoiceLine);
                } else {
                    LogEvent.logWarn(this.getClass().getSimpleName(), "createInvoice",
                            "Skipping test " + testId + " - no valid product mapping found");
                }
            }

            if (invoiceLines.isEmpty()) {
                throw new OdooOperationException(
                        "No valid product mappings found for any tests in sample: " + accessionNumber);
            }

            // Add invoice lines to invoice data
            invoiceData.put("invoice_line_ids", invoiceLines);

            // Create the invoice in Odoo
            Integer invoiceId = odooClient.createInvoice(invoiceData);

            LogEvent.logInfo(this.getClass().getSimpleName(), "createInvoice",
                    "Created invoice in Odoo for sample: " + accessionNumber + " with ID: " + invoiceId);
        } catch (OdooOperationException e) {
            throw e;
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createInvoice",
                    "Error creating invoice in Odoo: " + e.getMessage());
            throw new OdooOperationException("Failed to create invoice in Odoo", e);
        }
    }
}
