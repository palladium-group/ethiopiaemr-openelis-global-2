/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzer.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.form.AnalyzerResultsJsonForm;
import org.openelisglobal.analyzer.form.AnalyzerResultsJsonForm.ResultRow;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.service.AnalyzerResultsService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for receiving native JSON analyzer results from device
 * interfaces. POST /rest/analyzer/results
 */
@RestController
@RequestMapping("/rest/analyzer")
public class AnalyzerResultsRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerResultsRestController.class);

    @Autowired
    private AnalyzerService analyzerService;

    @Autowired
    private AnalyzerResultsService analyzerResultsService;

    @Autowired
    private SampleService sampleService;

    /**
     * Accept native JSON analyzer results. Requires analyzer to exist and test
     * mappings (analyzer test code → OpenELIS test) to be configured.
     *
     * @param request HTTP request (for optional session user)
     * @param form    JSON body: analyzerName, results[ { accessionNumber, testCode,
     *                result, units?, completeDate?, control? } ]
     * @return 200 with count of imported results, or 4xx with error message
     */
    @PostMapping(value = "/results", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> postResults(HttpServletRequest request,
            @RequestBody AnalyzerResultsJsonForm form) {
        Map<String, Object> body = new HashMap<>();

        String sysUserId = getSysUserId(request);

        Analyzer analyzer;
        try {
            analyzer = analyzerService.getAnalyzerByName(form.getAnalyzerName());
        } catch (Exception e) {
            logger.warn("Analyzer not found: {}", form.getAnalyzerName());
            body.put("error", "Analyzer not found: " + form.getAnalyzerName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        if (analyzer == null) {
            body.put("error", "Analyzer not found: " + form.getAnalyzerName());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }

        AnalyzerTestNameCache cache = AnalyzerTestNameCache.getInstance();
        List<AnalyzerResults> toInsert = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (ResultRow row : form.getResults()) {
            // Validate accession number exists in the system
            Sample sample = sampleService.getSampleByAccessionNumber(row.getAccessionNumber());
            if (sample == null) {
                errors.add("Accession number not found: " + row.getAccessionNumber());
                continue;
            }

            MappedTestName mapped = cache.getMappedTest(analyzer.getName(), row.getTestCode());
            if (mapped == null || mapped.getTestId() == null || "-1".equals(mapped.getTestId())) {
                errors.add("No mapping for testCode: " + row.getTestCode());
                continue;
            }

            AnalyzerResults ar = new AnalyzerResults();
            ar.setAnalyzerId(analyzer.getId());
            ar.setAccessionNumber(row.getAccessionNumber());
            ar.setTestName(mapped.getOpenElisTestName());
            ar.setTestId(mapped.getTestId());
            ar.setResult(row.getResult());
            ar.setUnits(row.getUnits());
            ar.setIsControl(row.isControl());
            ar.setResultType("N");

            if (row.getCompleteDate() != null && !row.getCompleteDate().isBlank()) {
                Timestamp ts = null;
                try {
                    // Only accept yyyy-MM-dd'T'HH:mm:ss format
                    ts = DateUtil.convertStringDateToTimestampWithPatternNoLocale(row.getCompleteDate(),
                            "yyyy-MM-dd'T'HH:mm:ss");
                } catch (Exception e) {
                    // Log warning and set to default (current time)
                    logger.warn(
                            "Invalid date format for accessionNumber '{}': '{}'. Expected format: yyyy-MM-dd'T'HH:mm:ss. Using current time instead.",
                            row.getAccessionNumber(), row.getCompleteDate());
                }
                ar.setCompleteDate(ts != null ? ts : new Timestamp(System.currentTimeMillis()));
            } else {
                ar.setCompleteDate(new Timestamp(System.currentTimeMillis()));
            }

            toInsert.add(ar);
        }

        if (!errors.isEmpty() && toInsert.isEmpty()) {
            body.put("error", "No valid results; " + String.join("; ", errors));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        try {
            analyzerResultsService.insertAnalyzerResults(toInsert, sysUserId);
        } catch (LIMSRuntimeException e) {
            LogEvent.logError(e);
            body.put("error", "Failed to save results: " + (e.getMessage() != null ? e.getMessage() : "unknown"));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }

        body.put("imported", toInsert.size());
        if (!errors.isEmpty()) {
            body.put("warnings", errors);
        }
        return ResponseEntity.ok(body);
    }
}
