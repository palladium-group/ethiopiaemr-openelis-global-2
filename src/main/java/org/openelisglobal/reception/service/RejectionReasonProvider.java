package org.openelisglobal.reception.service;

import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.springframework.stereotype.Component;

@Component
public class RejectionReasonProvider {

    public List<IdValuePair> getRejectionReasons() {
        return DisplayListService.getInstance().getList(ListType.REJECTION_REASONS);
    }
}
