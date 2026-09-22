package org.openelisglobal.program.service.cytology;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.beanAdapters.ResultSaveBeanAdapter;
import org.openelisglobal.common.services.registration.ResultUpdateRegister;
import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirTransformService;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.dao.cytology.CytologySampleDAO;
import org.openelisglobal.program.service.ImmunohistochemistrySampleService;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologySubtype;
import org.openelisglobal.program.valueholder.cytology.CytologySlide;
import org.openelisglobal.program.valueholder.cytology.CytologySlide.CytologySlideType;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
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
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CytologySampleServiceImpl extends AuditableBaseObjectServiceImpl<CytologySample, Integer>
        implements CytologySampleService {

    @Autowired
    protected CytologySampleDAO baseObjectDAO;

    @Autowired
    protected SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    @Autowired
    private FhirTransformService fhirTransformService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;

    @Autowired
    private ProgramService programService;

    CytologySampleServiceImpl() {
        super(CytologySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected CytologySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<CytologySample> getWithStatus(List<CytologyStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Transactional
    @Override
    public void assignTechnician(Integer cytologySampleId, SystemUser systemUser, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        cytologySample.setTechnician(systemUser);
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String searchTerm) {
        List<CytologySample> cytologySamples = baseObjectDAO.getWithStatus(statuses);
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                cytologySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses, searchTerm);
            } else {
                List<CytologySample> filteredCytologySamples = new ArrayList<>();
                cytologySamples.forEach(cytologySample -> {
                    Patient patient = sampleService.getPatient(cytologySample.getSample());
                    if (patient.getPerson().getFirstName().equals(searchTerm)
                            || patient.getPerson().getLastName().equals(searchTerm)) {
                        filteredCytologySamples.add(cytologySample);
                    }
                });
                cytologySamples = filteredCytologySamples;
            }
        }

        return cytologySamples;
    }

    @Transactional
    @Override
    public void assignCytoPathologist(Integer cytologySampleId, SystemUser systemUser, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        cytologySample.setCytoPathologist(systemUser);
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Override
    public Long getCountUnassigned() {
        return baseObjectDAO.getCountUnassigned();
    }

    @Override
    public List<CytologySample> searchUnassigned(String searchTerm) {
        List<CytologySample> cytologySamples = baseObjectDAO.getUnassigned();
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                cytologySamples = baseObjectDAO.searchUnassignedWithAccessionNumber(searchTerm);
            } else {
                List<CytologySample> filtered = new ArrayList<>();
                cytologySamples.forEach(cytologySample -> {
                    Patient patient = sampleService.getPatient(cytologySample.getSample());
                    if (patient.getPerson().getFirstName().equals(searchTerm)
                            || patient.getPerson().getLastName().equals(searchTerm)) {
                        filtered.add(cytologySample);
                    }
                });
                cytologySamples = filtered;
            }
        }
        return cytologySamples;
    }

    @Override
    public Long getOpenCaseloadForCytoPathologist(String cytoPathologistId) {
        return baseObjectDAO.getOpenCaseloadForCytoPathologist(cytoPathologistId);
    }

    @Override
    public Long getCountWithStatus(List<CytologyStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer cytologySampleId, CytologySampleForm form) {
        CytologySample cytologySample = get(cytologySampleId);
        if (!GenericValidator.isBlankOrNull(form.getAssignedCytoPathologistId())) {
            cytologySample.setCytoPathologist(systemUserService.get(form.getAssignedCytoPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            cytologySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        cytologySample.setStatus(form.getStatus());

        cytologySample.getSlides().removeAll(cytologySample.getSlides());
        if (form.getSlides() != null)
            form.getSlides().stream().forEach(e -> e.setId(null));
        cytologySample.getSlides().addAll(form.getSlides());
        if (form.getSpecimenAdequacy() != null) {
            cytologySample.setSpecimenAdequacy(form.getSpecimenAdequacy());
        }

        cytologySample.getReports().removeAll(cytologySample.getReports());
        if (form.getReports() != null) {
            form.getReports().stream().forEach(e -> e.setId(null));
            cytologySample.getReports().addAll(form.getReports());
        }

        if (form.getDiagnosis() != null) {
            cytologySample.setDiagnosis(form.getDiagnosis());
        }

        if (form.getRelease()) {
            validateCytologySample(cytologySample, form);
        }
    }

    private void validateCytologySample(CytologySample cytologySample, CytologySampleForm form) {
        cytologySample.setStatus(CytologyStatus.COMPLETED);
        Sample sample = cytologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());
        List<Analysis> finalizedAnalyses = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    String conclusionText = form.getConclusionText();
                    if (GenericValidator.isBlankOrNull(conclusionText)) {
                        conclusionText = cytologySample.getConclusionText();
                    }
                    testResultItem.setResultValue(GenericValidator.isBlankOrNull(conclusionText)
                            ? MessageUtil.getMessage("result.cytoology.seereport")
                            : conclusionText);
                }
                Analysis analysis = analysisService.get(testResultItem.getAnalysisId());
                ResultSaveBean bean = ResultSaveBeanAdapter.fromTestResultItem(testResultItem);
                ResultSaveService resultSaveService = new ResultSaveService(analysis, form.getSystemUserId());
                List<Result> results = resultSaveService.createResultsFromTestResultItem(bean, new ArrayList<>());
                resultUpdateList.addAll(results);
                for (Result result : results) {
                    boolean newResult = result.getId() == null;
                    analysis.setEnteredDate(DateUtil.getNowAsTimestamp());

                    if (newResult) {
                        analysis.setRevision("1");
                        actionDataSet.getNewResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    } else {
                        analysis.setRevision(String.valueOf(Integer.parseInt(analysis.getRevision()) + 1));
                        actionDataSet.getModifiedResults()
                                .add(new ResultSet(result, null, null, patient, sample, new HashMap<>(), false));
                    }

                    if (ConfigurationProperties.getInstance().getPropertyValueUpperCase(Property.StatusRules)
                            .equals(IActionConstants.STATUS_RULES_RETROCI)) {
                        if (!SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)
                                .equals(analysis.getStatusId())) {
                            analysis.setCompletedDate(
                                    DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                            analysis.setStatusId(SpringContext.getBean(IStatusService.class)
                                    .getStatusID(AnalysisStatus.TechnicalAcceptance));
                        }
                    } else if (SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                            AnalysisStatus.Finalized)
                            || SpringContext.getBean(IStatusService.class).matches(analysis.getStatusId(),
                                    AnalysisStatus.TechnicalAcceptance)
                            || (analysis.isReferredOut()
                                    && !GenericValidator.isBlankOrNull(testResultItem.getShadowResultValue()))) {
                        analysis.setCompletedDate(DateUtil.convertStringDateToSqlDate(testResultItem.getTestDate()));
                        analysis.setStatusId(
                                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                    }
                }
                analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                finalizedAnalyses.add(analysis);
            }
        }

        logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                form.getSystemUserId());
        Sample finishedSample = sampleService.get(sample.getId());
        finishedSample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));

        final List<Analysis> analysesForFhir = finalizedAnalyses;
        final ArrayList<Result> resultsForFhir = resultUpdateList;
        final Sample sampleForFhir = finishedSample;
        final String microscopicFindingForFhir = cytologySample.getMicroscopyExam();
        final String conclusionTextForFhir = !GenericValidator.isBlankOrNull(form.getConclusionText())
                ? form.getConclusionText()
                : cytologySample.getConclusionText();
        final String conclusionForFhir = !GenericValidator.isBlankOrNull(form.getConclusion()) ? form.getConclusion()
                : cytologySample.getConclusion();
        final List<String> conclusionDictionaryIdsForFhir = GenericValidator.isBlankOrNull(conclusionForFhir)
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(conclusionForFhir));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fhirTransformService.transformPersistResultValidationFhirObjects(new ArrayList<>(),
                            analysesForFhir, resultsForFhir, new ArrayList<>(),
                            new ArrayList<>(Arrays.asList(sampleForFhir)), new ArrayList<>(),
                            microscopicFindingForFhir, conclusionTextForFhir, conclusionDictionaryIdsForFhir);
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(CytologySampleServiceImpl.class.getSimpleName(), "validateCytologySample",
                            "could not push cytology result to FHIR for sample " + sampleForFhir.getAccessionNumber()
                                    + ": " + e.getMessage());
                }
            }
        });
    }

    @Transactional
    @Override
    public void confirmCollection(Integer cytologySampleId, CytologySampleForm form, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getStatus() == CytologyStatus.REJECTED
                || cytologySample.getStatus() == CytologyStatus.COMPLETED) {
            throw new IllegalArgumentException("case cannot be collected in status " + cytologySample.getStatus());
        }
        if (cytologySample.getStatus() != CytologyStatus.RECEIVED
                && cytologySample.getStatus() != CytologyStatus.PREPARING_SLIDES) {
            // Already past Collection — leave as-is (idempotent).
            return;
        }

        applyCollectionFields(cytologySample, form);
        stampSampleCollected(cytologySample.getSample(), curUserId);
        cytologySample.setCollectionConfirmedAt(DateUtil.getNowAsTimestamp());

        if (cytologySample.getSlides() == null) {
            cytologySample.setSlides(new ArrayList<>());
        }
        if (cytologySample.getSlides().isEmpty() && cytologySample.getSubtype() != CytologySubtype.FLUID) {
            cytologySample.getSlides().add(newSmearSlide(1, curUserId));
        }

        if (cytologySample.getSubtype() == CytologySubtype.FLUID) {
            cytologySample.setStatus(CytologyStatus.CELL_BLOCK);
        } else {
            cytologySample.setStatus(CytologyStatus.STAINING);
        }
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void rejectCollection(Integer cytologySampleId, String rejectionReason, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getSubtype() != CytologySubtype.FLUID) {
            throw new IllegalArgumentException("rejection is only supported for Fluid cytology");
        }
        if (cytologySample.getStatus() != CytologyStatus.RECEIVED
                && cytologySample.getStatus() != CytologyStatus.PREPARING_SLIDES) {
            throw new IllegalArgumentException("case must be at Collection to reject");
        }
        cytologySample.setRejectionReason(rejectionReason);
        cytologySample.setStatus(CytologyStatus.REJECTED);
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void markCellBlockStep(Integer cytologySampleId, String step, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getSubtype() != CytologySubtype.FLUID) {
            throw new IllegalArgumentException("cell block is only for Fluid cytology");
        }
        if (cytologySample.getStatus() != CytologyStatus.CELL_BLOCK) {
            if (cytologySample.getStatus() == CytologyStatus.STAINING
                    || cytologySample.getStatus() == CytologyStatus.READY_FOR_CYTOPATHOLOGIST
                    || cytologySample.getStatus() == CytologyStatus.COMPLETED) {
                return;
            }
            throw new IllegalArgumentException("case must be in CELL_BLOCK");
        }
        if (GenericValidator.isBlankOrNull(step)) {
            throw new IllegalArgumentException("cell block step is required");
        }
        Timestamp now = DateUtil.getNowAsTimestamp();
        String normalized = step.trim().toLowerCase();
        switch (normalized) {
        case "centrifuge":
            if (cytologySample.getCellBlockCentrifugedAt() == null) {
                cytologySample.setCellBlockCentrifugedAt(now);
            }
            break;
        case "prepare":
            if (cytologySample.getCellBlockCentrifugedAt() == null) {
                throw new IllegalArgumentException("centrifuge cell block before prepare");
            }
            if (cytologySample.getCellBlockPreparedAt() == null) {
                cytologySample.setCellBlockPreparedAt(now);
            }
            break;
        case "slide":
            if (cytologySample.getCellBlockPreparedAt() == null) {
                throw new IllegalArgumentException("prepare cell block before cutting slide");
            }
            if (cytologySample.getCellBlockSlideAt() == null) {
                cytologySample.setCellBlockSlideAt(now);
            }
            if (cytologySample.getSlides() == null) {
                cytologySample.setSlides(new ArrayList<>());
            }
            boolean hasCellBlockSlide = cytologySample.getSlides().stream()
                    .anyMatch(s -> s.getSlideType() == CytologySlideType.CELL_BLOCK_HE);
            if (!hasCellBlockSlide) {
                CytologySlide slide = new CytologySlide();
                slide.setSlideNumber(nextSlideNumber(cytologySample));
                slide.setSlideType(CytologySlideType.CELL_BLOCK_HE);
                slide.setLocation("CB.1");
                slide.setSysUserId(curUserId);
                cytologySample.getSlides().add(slide);
            }
            // Also ensure at least one smear if none exist from collection
            if (cytologySample.getSlides().stream().noneMatch(s -> s.getSlideType() == CytologySlideType.SMEAR)) {
                cytologySample.getSlides().add(newSmearSlide(nextSlideNumber(cytologySample), curUserId));
            }
            cytologySample.setStatus(CytologyStatus.STAINING);
            break;
        default:
            throw new IllegalArgumentException("unknown cell block step: " + step);
        }
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void markSlideStained(Integer cytologySampleId, Integer slideId, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getStatus() != CytologyStatus.STAINING
                && cytologySample.getStatus() != CytologyStatus.SCREENING) {
            if (cytologySample.getStatus() == CytologyStatus.READY_FOR_CYTOPATHOLOGIST
                    || cytologySample.getStatus() == CytologyStatus.COMPLETED) {
                return;
            }
            throw new IllegalArgumentException("case must be in STAINING before marking a slide stained");
        }
        if (slideId == null) {
            throw new IllegalArgumentException("slide id is required");
        }
        CytologySlide target = findSlide(cytologySample, slideId);
        if (target.getStainedAt() == null) {
            target.setStainedAt(DateUtil.getNowAsTimestamp());
            target.setSysUserId(curUserId);
        }
        if (isStainingComplete(cytologySample)) {
            cytologySample.setStatus(CytologyStatus.READY_FOR_CYTOPATHOLOGIST);
        }
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void addSmearSlide(Integer cytologySampleId, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getStatus() != CytologyStatus.STAINING
                && cytologySample.getStatus() != CytologyStatus.CELL_BLOCK
                && cytologySample.getStatus() != CytologyStatus.SCREENING) {
            throw new IllegalArgumentException("case must be in STAINING or CELL_BLOCK to add a smear");
        }
        if (cytologySample.getSlides() == null) {
            cytologySample.setSlides(new ArrayList<>());
        }
        cytologySample.getSlides().add(newSmearSlide(nextSlideNumber(cytologySample), curUserId));
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void saveReadDraft(Integer cytologySampleId, String microscopyExam, String conclusion,
            String conclusionText, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        requireReadyForRead(cytologySample);
        applyReadFindings(cytologySample, microscopyExam, conclusion, conclusionText, curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void requestRepeat(Integer cytologySampleId, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        requireReadyForRead(cytologySample);
        cytologySample.setRepeatRequestedAt(DateUtil.getNowAsTimestamp());
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void requestSecondOpinion(Integer cytologySampleId, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        requireReadyForRead(cytologySample);
        cytologySample.setSecondOpinionRequestedAt(DateUtil.getNowAsTimestamp());
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);
    }

    @Transactional
    @Override
    public void orderIhc(Integer cytologySampleId, String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getSubtype() != CytologySubtype.FLUID) {
            throw new IllegalArgumentException("Order IHC is only for Fluid cytology");
        }
        requireReadyForRead(cytologySample);
        cytologySample.setIhcOrderedAt(DateUtil.getNowAsTimestamp());
        cytologySample.setSysUserId(curUserId);
        update(cytologySample);

        Program ihcProgram = programService.getMatch("code", "IHC").orElse(null);
        if (ihcProgram == null) {
            throw new IllegalStateException("IHC program is not configured");
        }
        ImmunohistochemistrySample immunoHistoSample = new ImmunohistochemistrySample();
        immunoHistoSample.setProgram(ihcProgram);
        immunoHistoSample.setQuestionnaireResponseUuid(cytologySample.getQuestionnaireResponseUuid());
        immunoHistoSample.setSample(cytologySample.getSample());
        immunoHistoSample.setReffered(true);
        immunoHistoSample.setSysUserId(curUserId);
        immunohistochemistrySampleService.save(immunoHistoSample);
    }

    @Transactional
    @Override
    public void signOut(Integer cytologySampleId, String microscopyExam, String conclusion, String conclusionText,
            String curUserId) {
        CytologySample cytologySample = get(cytologySampleId);
        if (cytologySample.getStatus() == CytologyStatus.COMPLETED) {
            return;
        }
        requireReadyForRead(cytologySample);
        applyReadFindings(cytologySample, microscopyExam, conclusion, conclusionText, curUserId);

        CytologySampleForm form = new CytologySampleForm();
        form.setSystemUserId(curUserId);
        form.setConclusion(conclusion);
        form.setConclusionText(conclusionText);
        form.setRelease(true);
        validateCytologySample(cytologySample, form);
        update(cytologySample);
    }

    private void applyCollectionFields(CytologySample cytologySample, CytologySampleForm form) {
        if (form == null) {
            return;
        }
        if (form.getCollectionSite() != null) {
            cytologySample.setCollectionSite(form.getCollectionSite());
        }
        if (form.getCollectionNotes() != null) {
            cytologySample.setCollectionNotes(form.getCollectionNotes());
        }
        if (form.getRadiologyReference() != null) {
            cytologySample.setRadiologyReference(form.getRadiologyReference());
        }
        if (form.getRoseAdequate() != null) {
            cytologySample.setRoseAdequate(form.getRoseAdequate());
        }
        if (form.getLastMenstrualPeriod() != null) {
            cytologySample.setLastMenstrualPeriod(form.getLastMenstrualPeriod());
        }
        if (form.getPreviousPapResult() != null) {
            cytologySample.setPreviousPapResult(form.getPreviousPapResult());
        }
        if (form.getFixationMethod() != null) {
            cytologySample.setFixationMethod(form.getFixationMethod());
        }
        if (form.getFluidVolume() != null) {
            cytologySample.setFluidVolume(form.getFluidVolume());
        }
        if (form.getFluidClarity() != null) {
            cytologySample.setFluidClarity(form.getFluidClarity());
        }
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

    private void requireReadyForRead(CytologySample cytologySample) {
        CytologyStatus status = cytologySample.getStatus();
        if (status != CytologyStatus.READY_FOR_CYTOPATHOLOGIST && status != CytologyStatus.SCREENING) {
            throw new IllegalArgumentException("case must be ready for cytopathologist read");
        }
    }

    private void applyReadFindings(CytologySample cytologySample, String microscopyExam, String conclusion,
            String conclusionText, String curUserId) {
        cytologySample.setMicroscopyExam(microscopyExam);
        cytologySample.setConclusion(conclusion);
        cytologySample.setConclusionText(conclusionText);
        cytologySample.setSysUserId(curUserId);
    }

    private CytologySlide findSlide(CytologySample cytologySample, Integer slideId) {
        if (cytologySample.getSlides() != null) {
            for (CytologySlide slide : cytologySample.getSlides()) {
                if (slideId.equals(slide.getId())) {
                    return slide;
                }
            }
        }
        throw new IllegalArgumentException("slide not found on this case");
    }

    private boolean isStainingComplete(CytologySample cytologySample) {
        List<CytologySlide> slides = cytologySample.getSlides() == null ? new ArrayList<>()
                : cytologySample.getSlides();
        if (slides.isEmpty()) {
            return false;
        }
        return slides.stream().allMatch(s -> s.getStainedAt() != null);
    }

    private int nextSlideNumber(CytologySample cytologySample) {
        int next = 1;
        if (cytologySample.getSlides() != null) {
            for (CytologySlide slide : cytologySample.getSlides()) {
                if (slide.getSlideNumber() != null && slide.getSlideNumber() >= next) {
                    next = slide.getSlideNumber() + 1;
                }
            }
        }
        return next;
    }

    private CytologySlide newSmearSlide(int slideNumber, String curUserId) {
        CytologySlide slide = new CytologySlide();
        slide.setSlideNumber(slideNumber);
        slide.setSlideType(CytologySlideType.SMEAR);
        slide.setLocation("S." + slideNumber);
        slide.setSysUserId(curUserId);
        return slide;
    }
}
