package org.openelisglobal.reception.service;

import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves initial {@link Analysis} status on sample collection / order entry
 * when reception approval is enabled via site configuration.
 */
@Service
public class ReceptionApprovalSupport {

    private final ConfigurationProperties configurationProperties;
    private final IStatusService statusService;

    @Autowired
    public ReceptionApprovalSupport(ConfigurationProperties configurationProperties, IStatusService statusService) {
        this.configurationProperties = configurationProperties;
        this.statusService = statusService;
    }

    public boolean isReceptionApprovalRequired() {
        return configurationProperties.isPropertyValueEqual(Property.RECEPTION_APPROVAL_REQUIRED, "true");
    }

    /**
     * @param sampleItemRejected true when the sample item is rejected at collection
     * @return status id for a newly created analysis
     */
    public String resolveInitialAnalysisStatusId(boolean sampleItemRejected) {
        if (sampleItemRejected) {
            return statusService.getStatusID(AnalysisStatus.SampleRejected);
        }
        if (isReceptionApprovalRequired()) {
            return statusService.getStatusID(AnalysisStatus.PendingReception);
        }
        return statusService.getStatusID(AnalysisStatus.NotStarted);
    }
}
