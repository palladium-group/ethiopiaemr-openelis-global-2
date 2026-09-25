package org.openelisglobal.program.service;

import java.util.Collection;
import java.util.List;
import org.openelisglobal.program.bean.ProgramReceptionDashBoardCount;
import org.openelisglobal.program.valueholder.ProgramCaseDisplayItem;
import org.openelisglobal.program.valueholder.ReceptionBucket;
import org.openelisglobal.program.valueholder.ServiceCategory;

/**
 * Backs the single reception queue shared by every pathology service category. Histopathology and
 * cytopathology keep their own entities, statuses and case views; this service only fans out to
 * them and normalizes the results into one list the dashboard can render.
 */
public interface ProgramReceptionService {

    /**
     * Cases in the given bucket across the given categories, newest first.
     *
     * @param categories   categories to include; all of them when null or empty
     * @param bucket       stage bucket to filter on
     * @param searchTerm   accession number or patient name, may be blank
     * @param assignedToMe when true, only cases where {@code currentUserId} is assigned as
     *                     specialist or technician
     * @param currentUserId system user id used when {@code assignedToMe} is true
     */
    List<ProgramCaseDisplayItem> search(Collection<ServiceCategory> categories, ReceptionBucket bucket,
            String searchTerm, boolean assignedToMe, String currentUserId);

    /**
     * Tile counts summed across the given categories; all of them when null or empty. When
     * {@code assignedToMe} is true, counts only cases assigned to {@code currentUserId} as
     * specialist or technician.
     */
    ProgramReceptionDashBoardCount count(Collection<ServiceCategory> categories, boolean assignedToMe,
            String currentUserId);
}
