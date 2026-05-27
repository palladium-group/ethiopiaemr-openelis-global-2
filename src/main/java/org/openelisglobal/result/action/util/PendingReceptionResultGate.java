package org.openelisglobal.result.action.util;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.action.beanitems.AnalyzerResultItem;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

@Service
public class PendingReceptionResultGate {

    private static final String PENDING_RECEPTION_RESULTS_ERROR = "errors.reception.pendingResultsEntry";

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private IStatusService statusService;

    public boolean isPendingReception(Analysis analysis) {
        return analysis != null && statusService.matches(analysis.getStatusId(), AnalysisStatus.PendingReception);
    }

    public boolean sampleHasPendingReception(Sample sample) {
        if (sample == null || GenericValidator.isBlankOrNull(sample.getId())) {
            return false;
        }
        return analysisService.getAnalysesBySampleId(sample.getId()).stream().anyMatch(this::isPendingReception);
    }

    public void rejectIfSamplePendingReception(Errors errors, Sample sample, String accessionNumber) {
        if (sampleHasPendingReception(sample)) {
            rejectPendingReception(errors, accessionNumber);
        }
    }

    public void rejectIfResultItemsPendingReception(Errors errors, List<TestResultItem> items) {
        if (items == null) {
            return;
        }
        for (TestResultItem item : items) {
            if (item == null || GenericValidator.isBlankOrNull(item.getAnalysisId())) {
                continue;
            }
            Analysis analysis = analysisService.get(item.getAnalysisId());
            if (isPendingReception(analysis)) {
                item.setFailedValidation(true);
                rejectPendingReception(errors, item.getAccessionNumber());
            }
        }
    }

    public void rejectIfAnalyzerItemsPendingReception(Errors errors, List<AnalyzerResultItem> items) {
        if (items == null) {
            return;
        }
        for (AnalyzerResultItem item : items) {
            if (item == null || GenericValidator.isBlankOrNull(item.getAccessionNumber())) {
                continue;
            }
            Sample sample = sampleService.getSampleByAccessionNumber(item.getAccessionNumber());
            if (sampleHasPendingReception(sample)) {
                item.setErrorMessage("Sample is pending reception approval and cannot receive results.");
                rejectPendingReception(errors, item.getAccessionNumber());
            }
        }
    }

    private void rejectPendingReception(Errors errors, String accessionNumber) {
        errors.reject(PENDING_RECEPTION_RESULTS_ERROR, new String[] { accessionNumber },
                "Sample is pending reception approval and cannot receive results.");
    }
}
