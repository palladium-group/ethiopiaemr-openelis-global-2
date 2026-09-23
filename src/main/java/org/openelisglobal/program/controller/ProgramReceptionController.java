package org.openelisglobal.program.controller;

import java.util.Arrays;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.program.bean.ProgramReceptionDashBoardCount;
import org.openelisglobal.program.service.ProgramReceptionService;
import org.openelisglobal.program.valueholder.ProgramCaseDisplayItem;
import org.openelisglobal.program.valueholder.ReceptionBucket;
import org.openelisglobal.program.valueholder.ServiceCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single reception queue for the pathology service categories. The per-category endpoints under
 * {@code /rest/pathology} and {@code /rest/cytology} are untouched — case views, assignment and
 * workflow still go through them; this controller only serves the merged list and tiles.
 */
@RestController
public class ProgramReceptionController extends BaseRestController {

    @Autowired
    private ProgramReceptionService programReceptionService;

    @GetMapping(value = "/rest/pathology/reception/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<ProgramCaseDisplayItem> getReceptionEntries(
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "bucket", required = false, defaultValue = "UNASSIGNED") ReceptionBucket bucket,
            @RequestParam(value = "serviceCategories", required = false) ServiceCategory... serviceCategories) {
        return programReceptionService.search(asList(serviceCategories), bucket, searchTerm);
    }

    @GetMapping(value = "/rest/pathology/reception/dashboard/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<ProgramReceptionDashBoardCount> getReceptionCounts(
            @RequestParam(value = "serviceCategories", required = false) ServiceCategory... serviceCategories) {
        return ResponseEntity.ok(programReceptionService.count(asList(serviceCategories)));
    }

    private List<ServiceCategory> asList(ServiceCategory... serviceCategories) {
        return serviceCategories == null ? List.of() : Arrays.asList(serviceCategories);
    }
}
