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
     * Cases in the given bucket across the given categories, newest request first.
     *
     * @param categories categories to include; all of them when null or empty
     * @param bucket     stage bucket to filter on
     * @param searchTerm accession number or patient name, may be blank
     */
    List<ProgramCaseDisplayItem> search(Collection<ServiceCategory> categories, ReceptionBucket bucket,
            String searchTerm);

    /**
     * Tile counts summed across the given categories; all of them when null or empty.
     */
    ProgramReceptionDashBoardCount count(Collection<ServiceCategory> categories);
}
