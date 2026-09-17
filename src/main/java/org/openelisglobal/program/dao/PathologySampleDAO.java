package org.openelisglobal.program.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;

public interface PathologySampleDAO extends BaseDAO<PathologySample, Integer> {

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    List<PathologySample> searchWithStatusAndAccesionNumber(List<PathologyStatus> statuses, String labNumber);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    /** Cases with no pathologist assigned (Reception unassigned queue). */
    Long getCountUnassigned();

    /** Open cases with no pathologist assigned (same criteria as {@link #getCountUnassigned()}). */
    List<PathologySample> getUnassigned();

    /** Unassigned cases matching accession number. */
    List<PathologySample> searchUnassignedWithAccessionNumber(String labNumber);

    /** Open (non-COMPLETED) cases currently assigned to the given pathologist. */
    Long getOpenCaseloadForPathologist(String pathologistId);
}
