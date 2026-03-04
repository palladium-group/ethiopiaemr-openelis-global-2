/*
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzer.controller;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Integration tests for AnalyzerResultsRestController. Tests POST
 * /rest/analyzer/results endpoint.
 */
@RunWith(SpringRunner.class)
public class AnalyzerResultsRestControllerTest extends BaseWebContextSensitiveTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/analyzer-results-rest-controller.xml");
        AnalyzerTestNameCache.getInstance().reloadCache();
    }

    @Test
    public void testPostResults_HappyPath_Returns200WithImportedCount() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "JSON Sample Analyzer");

        Map<String, Object> resultRow = new HashMap<>();
        resultRow.put("accessionNumber", "JSON-TEST-001");
        resultRow.put("testCode", "CBC");
        resultRow.put("result", "10.5");
        body.put("results", Arrays.asList(resultRow));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(1));
    }

    @Test
    public void testPostResults_HappyPath_WithUnitsAndCompleteDate_Returns200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "JSON Sample Analyzer");

        Map<String, Object> resultRow = new HashMap<>();
        resultRow.put("accessionNumber", "JSON-TEST-001");
        resultRow.put("testCode", "CBC");
        resultRow.put("result", "12.3");
        resultRow.put("units", "10^3/uL");
        resultRow.put("completeDate", "2025-01-15T14:30:00");
        resultRow.put("control", false);
        body.put("results", Arrays.asList(resultRow));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(1));
    }

    @Test
    public void testPostResults_WhenAnalyzerNotFound_Returns404() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "NonExistentAnalyzer");

        Map<String, Object> resultRow = new HashMap<>();
        resultRow.put("accessionNumber", "JSON-TEST-001");
        resultRow.put("testCode", "CBC");
        resultRow.put("result", "10.5");
        body.put("results", Arrays.asList(resultRow));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Analyzer not found: NonExistentAnalyzer"));
    }

    @Test
    public void testPostResults_WhenAccessionNotFound_Returns400WithWarnings() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "JSON Sample Analyzer");

        Map<String, Object> resultRow = new HashMap<>();
        resultRow.put("accessionNumber", "NONEXISTENT-999");
        resultRow.put("testCode", "CBC");
        resultRow.put("result", "10.5");
        body.put("results", Arrays.asList(resultRow));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("Accession number not found")));
    }

    @Test
    public void testPostResults_WhenNoMappingForTestCode_Returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "JSON Sample Analyzer");

        Map<String, Object> resultRow = new HashMap<>();
        resultRow.put("accessionNumber", "JSON-TEST-001");
        resultRow.put("testCode", "UnknownTestCode");
        resultRow.put("result", "10.5");
        body.put("results", Arrays.asList(resultRow));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("No mapping for testCode")));
    }

    @Test
    public void testPostResults_WhenBodyInvalid_EmptyAnalyzerName_Returns404() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "");
        body.put("results", Arrays.asList(new HashMap<>()));

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testPostResults_WhenResultsEmpty_Returns200WithZeroImported() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("analyzerName", "JSON Sample Analyzer");
        body.put("results", Arrays.asList());

        String json = objectMapper.writeValueAsString(body);

        mockMvc.perform(post("/rest/analyzer/results").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(0));
    }
}
