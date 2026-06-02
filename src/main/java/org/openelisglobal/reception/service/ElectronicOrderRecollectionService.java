package org.openelisglobal.reception.service;

import java.util.Optional;
import org.openelisglobal.reception.dto.RecollectionOrderResult;
import org.openelisglobal.sample.valueholder.Sample;

public interface ElectronicOrderRecollectionService {

    /**
     * Creates a new {@code Entered} electronic order for re-collection after
     * reception rejection, cloned from the sample's linked electronic order.
     *
     * @return empty when the sample has no linked electronic order (e.g. manual
     *         entry)
     */
    Optional<RecollectionOrderResult> createRecollectionOrder(Sample sample, String sysUserId);
}
