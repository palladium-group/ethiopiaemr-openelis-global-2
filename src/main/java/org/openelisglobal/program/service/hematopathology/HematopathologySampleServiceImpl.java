package org.openelisglobal.program.service.hematopathology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService.ResultFormObservation;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.controller.hematopathology.HematopathologySampleForm;
import org.openelisglobal.program.dao.hematopathology.HematopathologySampleDAO;
import org.openelisglobal.program.service.ProgramPaymentGateService;
import org.openelisglobal.program.util.ProgramSampleSearch;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;
import org.openelisglobal.result.action.util.ResultSet;
import org.openelisglobal.result.action.util.ResultsLoadUtility;
import org.openelisglobal.result.action.util.ResultsUpdateDataSet;
import org.openelisglobal.result.service.LogbookResultsPersistService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class HematopathologySampleServiceImpl extends AuditableBaseObjectServiceImpl<HematopathologySample, Integer>
        implements HematopathologySampleService {

    @Autowired
    protected HematopathologySampleDAO baseObjectDAO;
    @Autowired
    private ProgramPaymentGateService programPaymentGateService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;
    @Autowired
    private FhirTransformService fhirTransformService;

    HematopathologySampleServiceImpl() {
        super(HematopathologySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected HematopathologySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<HematopathologySample> getWithStatus(List<HematopathologyStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Override
    @Transactional
    public List<HematopathologySample> searchWithStatusAndTerm(List<HematopathologyStatus> statuses,
            String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return baseObjectDAO.getWithStatus(statuses);
        }
        if (sampleService.getSampleByAccessionNumber(searchTerm) != null) {
            return baseObjectDAO.searchWithStatusAndAccessionNumber(statuses, searchTerm);
        }
        return filterByPatientOrAccession(baseObjectDAO.getWithStatus(statuses), searchTerm);
    }

    @Override
    @Transactional
    public List<HematopathologySample> searchUnassigned(String searchTerm) {
        if (StringUtils.isBlank(searchTerm)) {
            return baseObjectDAO.getUnassigned();
        }
        if (sampleService.getSampleByAccessionNumber(searchTerm) != null) {
            return baseObjectDAO.searchUnassignedWithAccessionNumber(searchTerm);
        }
        return filterByPatientOrAccession(baseObjectDAO.getUnassigned(), searchTerm);
    }

    private List<HematopathologySample> filterByPatientOrAccession(List<HematopathologySample> samples,
            String searchTerm) {
        return samples.stream().filter(sample -> {
            Sample caseSample = sample.getSample();
            Patient patient = sampleService.getPatient(caseSample);
            return ProgramSampleSearch.matchesPatientOrAccession(caseSample, patient, searchTerm);
        }).collect(Collectors.toList());
    }

    @Override
    public Long getCountWithStatus(List<HematopathologyStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<HematopathologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }

    @Override
    public Long getCountUnassigned() {
        return baseObjectDAO.getCountUnassigned();
    }

    @Override
    public Long getOpenCaseloadForHematopathologist(String hematopathologistId) {
        return baseObjectDAO.getOpenCaseloadForHematopathologist(hematopathologistId);
    }

    @Override
    @Transactional
    public void assignTechnician(Integer hematopathologySampleId, SystemUser systemUser, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        sample.setTechnician(systemUser);
        sample.setSysUserId(curUserId);
        update(sample);
    }

    @Override
    @Transactional
    public void assignHematopathologist(Integer hematopathologySampleId, SystemUser systemUser, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        sample.setHematopathologist(systemUser);
        sample.setSysUserId(curUserId);
        update(sample);
    }

    @Override
    @Transactional
    public OpenMrsPaymentStatus confirmCollection(Integer hematopathologySampleId, HematopathologySampleForm form,
            String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        if (HematopathologyWorkflow.isClosed(sample.getStatus())) {
            throw new IllegalArgumentException("case cannot be collected in status " + sample.getStatus());
        }
        if (!HematopathologyWorkflow.isAtCollection(sample.getStatus())) {
            // Already past Collection — idempotent.
            return null;
        }
        if (form != null) {
            sample.setCollectionNotes(form.getCollectionNotes());
            sample.setSpiculesPresent(form.getSpiculesPresent());
            sample.setSampleAgeHours(form.getSampleAgeHours());
        }
        stampSampleCollected(sample.getSample(), curUserId);
        sample.setCollectionConfirmedAt(DateUtil.getNowAsTimestamp());

        OpenMrsPaymentStatus paymentStatus = programPaymentGateService.checkPayment(sample.getSample());
        sample.setStatus(ProgramPaymentGateService.allowsProceed(paymentStatus)
                ? HematopathologyWorkflow.statusAfterCollection(sample.getSubtype())
                : HematopathologyStatus.AWAITING_PAYMENT);
        sample.setSysUserId(curUserId);
        update(sample);
        return paymentStatus;
    }

    @Override
    @Transactional
    public OpenMrsPaymentStatus checkPayment(Integer hematopathologySampleId, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        if (sample.getStatus() != HematopathologyStatus.AWAITING_PAYMENT) {
            return null;
        }
        OpenMrsPaymentStatus paymentStatus = programPaymentGateService.checkPayment(sample.getSample());
        if (ProgramPaymentGateService.allowsProceed(paymentStatus)) {
            sample.setStatus(HematopathologyWorkflow.statusAfterCollection(sample.getSubtype()));
            sample.setSysUserId(curUserId);
            update(sample);
        }
        return paymentStatus;
    }

    @Override
    @Transactional
    public void rejectCollection(Integer hematopathologySampleId, String rejectionReason, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        if (!HematopathologyWorkflow.isAtCollection(sample.getStatus())) {
            throw new IllegalArgumentException("case must be at Collection to reject");
        }
        if (GenericValidator.isBlankOrNull(rejectionReason)) {
            throw new IllegalArgumentException("a rejection reason is required");
        }
        sample.setRejectionReason(rejectionReason);
        sample.setStatus(HematopathologyStatus.REJECTED);
        sample.setSysUserId(curUserId);
        update(sample);
    }

    @Override
    @Transactional
    public void markStep(Integer hematopathologySampleId, String step, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        HematopathologyStatus next = HematopathologyWorkflow.statusAfterStep(sample.getSubtype(), sample.getStatus(),
                step, sample.getSmearPreparedAt() != null);
        if (next == null) {
            return;
        }
        Timestamp now = DateUtil.getNowAsTimestamp();
        switch (step.trim()) {
        case HematopathologyWorkflow.STEP_SMEAR_PREPARED:
            if (sample.getSmearPreparedAt() == null) {
                sample.setSmearPreparedAt(now);
            }
            break;
        case HematopathologyWorkflow.STEP_SMEAR_STAINED:
            sample.setSmearStainedAt(now);
            break;
        case HematopathologyWorkflow.STEP_PROCESSING:
            sample.setProcessingCompletedAt(now);
            break;
        case HematopathologyWorkflow.STEP_ACQUISITION:
            sample.setAcquisitionCompletedAt(now);
            break;
        default:
            break;
        }
        sample.setStatus(next);
        sample.setSysUserId(curUserId);
        update(sample);
    }

    @Override
    @Transactional
    public void saveReadDraft(Integer hematopathologySampleId, HematopathologySampleForm form, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        requireReadyForRead(sample);
        applyFindings(sample, form, curUserId);
        update(sample);
    }

    @Override
    @Transactional
    public void signOut(Integer hematopathologySampleId, HematopathologySampleForm form, String curUserId) {
        HematopathologySample sample = get(hematopathologySampleId);
        if (sample.getStatus() == HematopathologyStatus.COMPLETED) {
            return;
        }
        requireReadyForRead(sample);
        applyFindings(sample, form, curUserId);
        if (GenericValidator.isBlankOrNull(sample.getConclusion())) {
            throw new IllegalArgumentException("a conclusion is required to sign out");
        }
        finalizeResults(sample, curUserId);
        sample.setStatus(HematopathologyStatus.COMPLETED);
        update(sample);
    }

    private void requireReadyForRead(HematopathologySample sample) {
        if (!HematopathologyWorkflow.isReadyForRead(sample.getStatus())) {
            throw new IllegalArgumentException("case must be ready for the hematopathologist");
        }
    }

    private void applyFindings(HematopathologySample sample, HematopathologySampleForm form, String curUserId) {
        if (form != null) {
            sample.setSmearQuality(form.getSmearQuality());
            sample.setPeripheralBloodMorphology(form.getPeripheralBloodMorphology());
            sample.setBoneMarrowMorphology(form.getBoneMarrowMorphology());
            sample.setSpecimenViability(form.getSpecimenViability());
            sample.setAntibodyPanel(form.getAntibodyPanel());
            sample.setGatingFindings(form.getGatingFindings());
            sample.setConclusion(form.getConclusion());
            sample.setComment(form.getComment());
        }
        sample.setSysUserId(curUserId);
    }

    /**
     * Writes the conclusion as the test's Remark result, finalizes the analyses,
     * finishes the sample and — after commit — pushes the DiagnosticReport with the
     * Result Form Observations so OpenMRS can import them.
     */
    private void finalizeResults(HematopathologySample hematopathologySample, String curUserId) {
        Sample sample = hematopathologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        IStatusService statusService = SpringContext.getBean(IStatusService.class);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(curUserId);
        List<Analysis> finalizedAnalyses = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        for (TestResultItem testResultItem : resultsUtility.getGroupedTestsForSample(sample)) {
            if (testResultItem.getIsGroupSeparator()) {
                continue;
            }
            if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                String conclusion = hematopathologySample.getConclusion();
                testResultItem.setResultValue(GenericValidator.isBlankOrNull(conclusion)
                        ? MessageUtil.getMessage("result.hematopathology.seereport")
                        : conclusion);
            }
            Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
            ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
            ResultSaveService resultSaveService = new ResultSaveService(analysis, curUserId);
            List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, new ArrayList<>());
            resultUpdateList.addAll(results);
            for (Result result : results) {
                analysis.setEnteredDate(DateUtil.getNowAsTimestamp());
                if (result.getId() == null) {
                    analysis.setRevision("1");
                    actionDataSet.getNewResults()
                            .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                } else {
                    analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
                    actionDataSet.getModifiedResults()
                            .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                }
            }
            analysis.setCompletedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
            analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            finalizedAnalyses.add(analysis);
        }

        logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                curUserId);
        Sample finishedSample = sampleService.get(sample.getId());
        finishedSample.setStatusId(statusService.getStatusID(OrderStatus.Finished));

        final List<Analysis> analysesForFhir = finalizedAnalyses;
        final ArrayList<Result> resultsForFhir = resultUpdateList;
        final Sample sampleForFhir = finishedSample;
        final List<ResultFormObservation> fieldsForFhir = HematopathologyResultForm
                .observationsFor(hematopathologySample);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fhirTransformService.transformPersistProgramResultFhirObjects(analysesForFhir, resultsForFhir,
                            new ArrayList<>(Arrays.asList(sampleForFhir)), fieldsForFhir);
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(HematopathologySampleServiceImpl.class.getSimpleName(), "finalizeResults",
                            "could not push hematopathology result to FHIR for sample "
                                    + sampleForFhir.getAccessionNumber() + ": " + e.getMessage());
                }
            }
        });
    }

    private void stampSampleCollected(Sample sample, String curUserId) {
        Timestamp now = DateUtil.getNowAsTimestamp();
        if (sample.getCollectionDate() == null) {
            sample.setCollectionDate(now);
            sample.setSysUserId(curUserId);
            sampleService.update(sample);
        }
        List<SampleItem> items = sampleItemService.getSampleItemsBySampleId(sample.getId());
        if (items != null) {
            for (SampleItem item : items) {
                if (item.getCollectionDate() == null) {
                    item.setCollectionDate(now);
                    item.setSysUserId(curUserId);
                    sampleItemService.update(item);
                }
            }
        }
    }
}
