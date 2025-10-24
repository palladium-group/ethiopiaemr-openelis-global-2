package org.openelisglobal.sample.event.listener;

import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.odoo.service.OdooIntegrationService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener {

    @Autowired
    private OdooIntegrationService odooIntegrationService;

    @Async
    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        // Check if Odoo integration is enabled globally
        boolean odooEnabled = ConfigurationProperties.getInstance()
                .isPropertyValueEqual(Property.ENABLE_OPENELIS_TO_ODOO_CONNECTION, "true");
        
        if (!odooEnabled) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Odoo integration is disabled. Skipping invoice creation for sample: " 
                    + (event.getUpdateData() != null ? event.getUpdateData().getAccessionNumber() : "unknown"));
            return;
        }
        
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();

            LogEvent.logInfo(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Starting Odoo integration for sample: " + updateData.getAccessionNumber());

            if (updateData.getSample() != null) {
                LogEvent.logInfo(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                        "Sample details - Collection Date: " + updateData.getSample().getCollectionDate()
                                + ", Received Date: " + updateData.getSample().getReceivedTimestamp()
                                + ", Tests count: "
                                + (updateData.getSampleItemsTests() != null ? updateData.getSampleItemsTests().size()
                                        : 0));
            }

            odooIntegrationService.createInvoice(updateData);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Error processing sample creation event for sample "
                            + (event.getUpdateData() != null ? event.getUpdateData().getAccessionNumber() : "unknown")
                            + ": " + e.getMessage());
            // Log the full stack trace for debugging
            LogEvent.logError(this.getClass().getSimpleName(), "handleSamplePatientUpdateDataCreatedEvent",
                    "Full stack trace: " + e.toString());
        }
    }
}
