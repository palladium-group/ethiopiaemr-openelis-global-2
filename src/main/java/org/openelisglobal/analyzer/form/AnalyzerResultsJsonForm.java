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
package org.openelisglobal.analyzer.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for POST /rest/analyzer/results (native JSON analyzer result
 * ingestion). Device interfaces send this payload to push results into
 * OpenELIS.
 */
public class AnalyzerResultsJsonForm {

    /** OpenELIS analyzer name (from analyzer table). Required. */
    @NotBlank(message = "analyzerName is required")
    private String analyzerName;

    /** One or more result rows. */
    @NotEmpty(message = "results must not be empty")
    @Valid
    private List<ResultRow> results;

    public String getAnalyzerName() {
        return analyzerName;
    }

    public void setAnalyzerName(String analyzerName) {
        this.analyzerName = analyzerName;
    }

    public List<ResultRow> getResults() {
        return results;
    }

    public void setResults(List<ResultRow> results) {
        this.results = results;
    }

    /** A single result row from the device. */
    public static class ResultRow {

        /** Sample accession number (must exist in OpenELIS). */
        @NotBlank(message = "accessionNumber is required")
        private String accessionNumber;

        /**
         * Analyzer test code (must be mapped to an OpenELIS test for this analyzer).
         */
        @NotBlank(message = "testCode is required")
        private String testCode;

        /** Result value. */
        @NotBlank(message = "result is required")
        private String result;

        /** Optional units (e.g. "10^3/uL"). */
        private String units;

        /** Optional completion date/time (ISO-8601 or yyyy-MM-dd HH:mm:ss). */
        private String completeDate;

        /** true if this is a control; default false. */
        private boolean control;

        public String getAccessionNumber() {
            return accessionNumber;
        }

        public void setAccessionNumber(String accessionNumber) {
            this.accessionNumber = accessionNumber;
        }

        public String getTestCode() {
            return testCode;
        }

        public void setTestCode(String testCode) {
            this.testCode = testCode;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        public String getCompleteDate() {
            return completeDate;
        }

        public void setCompleteDate(String completeDate) {
            this.completeDate = completeDate;
        }

        public boolean isControl() {
            return control;
        }

        public void setControl(boolean control) {
            this.control = control;
        }
    }
}
