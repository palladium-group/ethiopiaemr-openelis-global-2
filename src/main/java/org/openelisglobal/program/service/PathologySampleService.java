package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    Long getCountUnassigned();

    /** Open cases with no pathologist (Reception unassigned queue list). */
    List<PathologySample> searchUnassigned(String searchTerm);

    Long getOpenCaseloadForPathologist(String pathologistId);

    /**
     * Marks the specimen physically collected (Sample/SampleItem.collectionDate = now) and advances
     * status from RECEIVED to GROSSING. Idempotent if already collected.
     */
    void confirmReceived(Integer pathologySampleId, String curUserId);

    void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form);
}
