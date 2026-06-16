package org.openelisglobal.reception.service;

import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.reception.dto.ReceptionActionResponse;
import org.openelisglobal.reception.dto.ReceptionDetailResponse;
import org.openelisglobal.reception.dto.ReceptionQueueResponse;
import org.openelisglobal.reception.form.ReceptionApproveForm;
import org.openelisglobal.reception.form.ReceptionRejectForm;

public interface ReceptionService {

    ReceptionQueueResponse getPendingQueue(String accessionNumberFilter, String receivedDateFrom, String receivedDateTo,
            int page, int pageSize);

    ReceptionDetailResponse getPendingDetail(String accessionNumber);

    ReceptionActionResponse approve(ReceptionApproveForm form, String sysUserId);

    ReceptionActionResponse reject(ReceptionRejectForm form, String sysUserId);

    /**
     * Loads analyses for a single status with sample/test joins used by the home
     * dashboard queue.
     */
    List<Analysis> findAnalysesByStatusForDashboard(String statusId);
}
