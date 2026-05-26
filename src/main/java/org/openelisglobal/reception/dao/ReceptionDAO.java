package org.openelisglobal.reception.dao;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;

public interface ReceptionDAO extends BaseDAO<Analysis, String> {

    List<Analysis> findAnalysesByStatusWithSampleFilters(String statusId, String accessionNumberFilter,
            Timestamp receivedFrom, Timestamp receivedTo) throws LIMSRuntimeException;

    List<Analysis> findAnalysesByAccessionNumberAndStatus(String accessionNumber, String statusId)
            throws LIMSRuntimeException;
}
