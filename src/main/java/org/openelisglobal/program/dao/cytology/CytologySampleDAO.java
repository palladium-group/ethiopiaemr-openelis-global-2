package org.openelisglobal.program.dao.cytology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;

public interface CytologySampleDAO extends BaseDAO<CytologySample, Integer> {
    List<CytologySample> getWithStatus(List<CytologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to);

    List<CytologySample> searchWithStatusAndAccesionNumber(List<CytologyStatus> statuses, String labNumber);

    Long getCountWithStatus(List<CytologyStatus> statuses);

    /** Cases with no cytopathologist assigned (Reception unassigned queue). */
    Long getCountUnassigned();

    /** Open cases with no cytopathologist assigned. */
    List<CytologySample> getUnassigned();

    /** Unassigned cases matching accession number. */
    List<CytologySample> searchUnassignedWithAccessionNumber(String labNumber);

    /**
     * Open (non-COMPLETED/REJECTED) cases currently assigned to the given
     * cytopathologist.
     */
    Long getOpenCaseloadForCytoPathologist(String cytoPathologistId);
}
