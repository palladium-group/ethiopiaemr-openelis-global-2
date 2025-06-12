package org.openelisglobal.sample.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;

@Getter
public class SamplePatientUpdateDataCreatedEvent {
    private final SamplePatientUpdateData updateData;
    private final PatientManagementInfo patientInfo;
    private final OffsetDateTime createdAt;

    public SamplePatientUpdateDataCreatedEvent(SamplePatientUpdateData updateData, PatientManagementInfo patientInfo) {
        this.updateData = updateData;
        this.patientInfo = patientInfo;
        this.createdAt = OffsetDateTime.now();
    }
}
