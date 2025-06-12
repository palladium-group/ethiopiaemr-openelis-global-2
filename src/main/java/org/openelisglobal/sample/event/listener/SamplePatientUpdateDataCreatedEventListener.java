package org.openelisglobal.sample.event.listener;

import jakarta.annotation.PostConstruct;
import org.openelisglobal.common.event.bus.EventBus;
import org.openelisglobal.common.event.bus.EventSubscriber;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.event.SamplePatientUpdateDataCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener
        implements EventSubscriber<SamplePatientUpdateDataCreatedEvent> {

    @Autowired
    private EventBus<SamplePatientUpdateDataCreatedEvent> eventBus;

    @PostConstruct
    public void init() {
        eventBus.subscribe(this);
    }

    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        onEvent(event);
    }

    @Override
    public void onEvent(SamplePatientUpdateDataCreatedEvent event) {
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();

            // TODO: Implement Odoo integration

            LogEvent.logInfo(this.getClass().getSimpleName(), "onEvent",
                    String.format("Sample created with accession number: %s at %s", updateData.getAccessionNumber(),
                            event.getCreatedAt()));

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "onEvent",
                    "Error processing sample creation event: " + e.getMessage());
        }
    }
}
