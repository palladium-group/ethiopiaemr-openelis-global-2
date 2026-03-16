package org.openelisglobal.resultvalidation.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notification.service.TestNotificationService;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultValidationServiceImpl implements ResultValidationService {

    private AnalysisService analysisService;
    private ResultService resultService;
    private NoteService noteService;
    private SampleService sampleService;
    private TestNotificationService testNotificationService;
    private AnalyzerService analyzerService;
    private AnalyzerTestMappingService analyzerTestMappingService;
    private PanelService panelService;
    private String cbcPanelName;

    public ResultValidationServiceImpl(AnalysisService analysisService, ResultService resultService,
            NoteService noteService, SampleService sampleService, TestNotificationService testNotificationService,
            AnalyzerService analyzerService, AnalyzerTestMappingService analyzerTestMappingService,
            PanelService panelService,
            @Value("${org.openelisglobal.cbc.panelname:Complete Blood Count}") String cbcPanelName) {
        this.analysisService = analysisService;
        this.resultService = resultService;
        this.noteService = noteService;
        this.sampleService = sampleService;
        this.testNotificationService = testNotificationService;
        this.analyzerService = analyzerService;
        this.analyzerTestMappingService = analyzerTestMappingService;
        this.panelService = panelService;
        this.cbcPanelName = cbcPanelName;
    }

    @Override
    @Transactional
    public void persistdata(List<Result> deletableList, List<Analysis> analysisUpdateList,
            ArrayList<Result> resultUpdateList, List<AnalysisItem> resultItemList, ArrayList<Sample> sampleUpdateList,
            ArrayList<Note> noteUpdateList, IResultSaveService resultSaveService, List<IResultUpdate> updaters,
            String sysUserId) {
        ResultSaveService.removeDeletedResultsInTransaction(deletableList, sysUserId);

        // update analysis
        for (Analysis analysis : analysisUpdateList) {
            analysisService.update(analysis);
        }

        for (Result resultUpdate : resultUpdateList) {
            if (resultUpdate.getId() != null) {
                resultService.update(resultUpdate);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "validating a result that doesn't exist yet. Creating result.");
                String id = resultService.insert(resultUpdate);
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "Result with id: " + id + " created while validating");
            }
            if (isResultAnalysisFinalized(resultUpdate, analysisUpdateList)) {
                try {
                    testNotificationService.createAndSendNotificationsToConfiguredSources(
                            NotificationNature.RESULT_VALIDATION, resultUpdate);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }

        autoCancelCbcTests(resultItemList, sysUserId);

        checkIfSamplesFinished(resultItemList, sampleUpdateList);

        // update finished samples
        for (Sample sample : sampleUpdateList) {
            sampleService.update(sample);
        }

        // create or update notes
        for (Note note : noteUpdateList) {
            if (note != null) {
                if (note.getId() == null) {
                    if (!noteService.duplicateNoteExists(note)) {
                        noteService.insert(note);
                    }
                } else {
                    noteService.update(note);
                }
            }
        }

        for (IResultUpdate updater : updaters) {
            updater.transactionalUpdate(resultSaveService);
        }
    }

    private void autoCancelCbcTests(List<AnalysisItem> resultItemList, String sysUserId) {
        String cbcPanelId = panelService.getIdForPanelName(cbcPanelName);
        if (cbcPanelId == null) {
            return;
        }

        // Group validated items by accession
        java.util.Map<String, java.util.Set<String>> validatedTestsByAccession = new java.util.HashMap<>();
        for (AnalysisItem item : resultItemList) {
            if (item.getAccessionNumber() == null)
                continue;

            Analysis analysis = analysisService.getAnalysisById(item.getAnalysisId());
            if (analysis != null && analysis.getPanel() != null && cbcPanelId.equals(analysis.getPanel().getId())) {
                validatedTestsByAccession.computeIfAbsent(item.getAccessionNumber(), k -> new java.util.HashSet<>())
                        .add(item.getTestId());
            }
        }

        if (validatedTestsByAccession.isEmpty()) {
            return;
        }

        // Cache analyzer test mappings for this request
        java.util.Map<String, java.util.Set<String>> analyzerSupportedTestsMap = getAnalyzerSupportedTestsMap();

        for (java.util.Map.Entry<String, java.util.Set<String>> entry : validatedTestsByAccession.entrySet()) {
            String accessionNumber = entry.getKey();
            java.util.Set<String> validatedTestIds = entry.getValue();

            String matchingAnalyzerId = findMatchingAnalyzerId(validatedTestIds, analyzerSupportedTestsMap);

            if (matchingAnalyzerId != null) {
                cancelExtraCbcAnalyses(accessionNumber, cbcPanelId, validatedTestIds, sysUserId);
            }
        }
    }

    private java.util.Map<String, java.util.Set<String>> getAnalyzerSupportedTestsMap() {
        java.util.Map<String, java.util.Set<String>> map = new java.util.HashMap<>();
        List<Analyzer> activeAnalyzers = analyzerService.getAllWithTypes();

        for (Analyzer analyzer : activeAnalyzers) {
            Analyzer.AnalyzerStatus status = analyzer.getStatus();
            if (status == Analyzer.AnalyzerStatus.ACTIVE || status == Analyzer.AnalyzerStatus.SETUP) {
                List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAllForAnalyzer(analyzer.getId());
                java.util.Set<String> testIds = new java.util.HashSet<>();
                for (AnalyzerTestMapping m : mappings) {
                    testIds.add(m.getTestId());
                }
                map.put(analyzer.getId(), testIds);
            }
        }
        return map;
    }

    private String findMatchingAnalyzerId(java.util.Set<String> validatedTestIds,
            java.util.Map<String, java.util.Set<String>> analyzerSupportedTestsMap) {
        for (java.util.Map.Entry<String, java.util.Set<String>> entry : analyzerSupportedTestsMap.entrySet()) {
            java.util.Set<String> analyzerTestIds = entry.getValue();
            // Reversed Subset Matching: Match if ALL tests the analyzer supports were
            // validated
            if (!analyzerTestIds.isEmpty() && validatedTestIds.containsAll(analyzerTestIds)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void cancelExtraCbcAnalyses(String accessionNumber, String cbcPanelId,
            java.util.Set<String> validatedTestIds, String sysUserId) {
        Sample sample = sampleService.getSampleByAccessionNumber(accessionNumber);
        if (sample == null)
            return;

        List<Analysis> allAnalyses = analysisService.getAnalysesBySampleId(sample.getId());
        List<Analysis> toCancel = new java.util.ArrayList<>();

        String notStartedStatus = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted);
        String technicalAcceptanceStatus = SpringContext.getBean(IStatusService.class)
                .getStatusID(AnalysisStatus.TechnicalAcceptance);
        String canceledStatus = SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled);

        for (Analysis analysis : allAnalyses) {
            if (analysis.getPanel() != null && cbcPanelId.equals(analysis.getPanel().getId())) {
                // Cancel ANY CBC test that was NOT part of the current validation batch
                if (!validatedTestIds.contains(analysis.getTest().getId())) {
                    String statusId = analysis.getStatusId();
                    // Cancel it if it's in a non-terminal, pre-validation state
                    if (statusId.equals(notStartedStatus) || statusId.equals(technicalAcceptanceStatus)) {
                        analysis.setStatusId(canceledStatus);
                        toCancel.add(analysis);
                    }
                }
            }
        }

        if (!toCancel.isEmpty()) {
            analysisService.updateAnalysises(toCancel, new ArrayList<>(), sysUserId);
        }
    }

    private boolean isResultAnalysisFinalized(Result result, List<Analysis> analysisUpdateList) {
        String analysisId = result.getAnalysis().getId();
        for (Analysis analysis : analysisUpdateList) {
            if (analysis.getId().equals(analysisId)) {
                return analysis.getStatusId()
                        .equals(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
            }
        }
        return false;
    }

    private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Sample> sampleUpdateList) {
        String currentSampleId = "";
        boolean sampleFinished = true;
        List<Integer> sampleFinishedStatus = getSampleFinishedStatuses();

        for (AnalysisItem analysisItem : resultItemList) {
            String analysisSampleId = sampleService.getSampleByAccessionNumber(analysisItem.getAccessionNumber())
                    .getId();
            if (!analysisSampleId.equals(currentSampleId)) {

                currentSampleId = analysisSampleId;

                List<Analysis> analysisList = analysisService.getAnalysesBySampleId(currentSampleId);

                for (Analysis analysis : analysisList) {
                    if (!sampleFinishedStatus.contains(Integer.parseInt(analysis.getStatusId()))) {
                        sampleFinished = false;
                        break;
                    }
                }

                if (sampleFinished) {
                    Sample sample = sampleService.get(currentSampleId);
                    sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
                    sampleUpdateList.add(sample);
                }

                sampleFinished = true;
            }
        }
    }

    private List<Integer> getSampleFinishedStatuses() {
        ArrayList<Integer> sampleFinishedStatus = new ArrayList<>();
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        sampleFinishedStatus.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        return sampleFinishedStatus;
    }
}
