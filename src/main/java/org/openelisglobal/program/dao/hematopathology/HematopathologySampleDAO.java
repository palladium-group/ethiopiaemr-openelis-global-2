package org.openelisglobal.program.dao.hematopathology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;

public interface HematopathologySampleDAO extends BaseDAO<HematopathologySample, Integer> {
    List<HematopathologySample> getWithStatus(List<HematopathologyStatus> statuses);

    List<HematopathologySample> searchWithStatusAndAccessionNumber(List<HematopathologyStatus> statuses,
            String labNumber);

    Long getCountWithStatus(List<HematopathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<HematopathologyStatus> statuses, Timestamp from, Timestamp to);

    /** Open cases with no hematopathologist assigned. */
    Long getCountUnassigned();

    List<HematopathologySample> getUnassigned();

    List<HematopathologySample> searchUnassignedWithAccessionNumber(String labNumber);

    /** Open (not completed / rejected) cases assigned to the hematopathologist. */
    Long getOpenCaseloadForHematopathologist(String hematopathologistId);
}
