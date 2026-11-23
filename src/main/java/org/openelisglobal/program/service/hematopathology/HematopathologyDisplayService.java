package org.openelisglobal.program.service.hematopathology;

import org.openelisglobal.program.valueholder.hematopathology.HematopathologyCaseViewDisplayItem;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologyDisplayItem;

public interface HematopathologyDisplayService {

    HematopathologyDisplayItem convertToDisplayItem(Integer hematopathologySampleId);

    HematopathologyCaseViewDisplayItem convertToCaseDisplayItem(Integer hematopathologySampleId);
}
