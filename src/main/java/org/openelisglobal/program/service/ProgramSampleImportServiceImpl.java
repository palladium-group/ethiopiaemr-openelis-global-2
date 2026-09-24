package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.service.cytology.CytologySampleService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.reception.service.ReceptionApprovalSupport;
import org.openelisglobal.requester.service.SampleRequesterService;
import org.openelisglobal.requester.valueholder.SampleRequester;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramSampleImportServiceImpl implements ProgramSampleImportService {

    private static final String DEFAULT_ANALYSIS_TYPE = "MANUAL";

    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleRequesterService sampleRequesterService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private TestService testService;
    @Autowired
    private ProgramService programService;
    @Autowired
    private ReceptionApprovalSupport receptionApprovalSupport;
    @Autowired
    private PathologySampleService pathologySampleService;
    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;
    @Autowired
    private ProgramSampleService programSampleService;
    @Autowired
    private CytologySampleService cytologySampleService;
    @Autowired
    private IStatusService statusService;

    @Override
    @Transactional
    public void createProgramSampleFromImport(Program programArg, Test testArg, MessagePatient messagePatient,
            OrderPriority priority, String externalOrderId, UUID questionnaireResponseUuid, Date collectionDate,
            Provider requestingProvider) {
        createProgramSampleFromImport(programArg, testArg, messagePatient, priority, externalOrderId,
                questionnaireResponseUuid, collectionDate, requestingProvider, null);
    }

    @Override
    @Transactional
    public void createProgramSampleFromImport(Program programArg, Test testArg, MessagePatient messagePatient,
            OrderPriority priority, String externalOrderId, UUID questionnaireResponseUuid, Date collectionDate,
            Provider requestingProvider, String programSubtypeText) {
        // Idempotency guard: the poller can process the same remote task more than once (e.g. it runs
        // once per configured remote store path, and again on any cycle before the task status flips),
        // so skip if a sample for this order already exists. The standard electronic-order import gets
        // this from DBOrderExistanceChecker; the program branch needs its own guard.
        if (!sampleService.getSamplesByReferringId(externalOrderId).isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "createProgramSampleFromImport",
                    "a sample for imported order " + externalOrderId + " already exists; skipping duplicate import");
            return;
        }

        // Re-load within this transaction so the entities are attached (they were
        // resolved during
        // interpretation, potentially in a different persistence context).
        Test test = testService.get(testArg.getId());
        Program program = programService.get(programArg.getId());

        IOrderPersister orderPersister = SpringContext.getBean(IOrderPersister.class);
        String serviceUserId = orderPersister.getServiceUserId();
        Patient patient = orderPersister.persistPatientData(messagePatient);

        // EMR pathology/cytology orders arrive as e-orders only. Physical sample collection happens
        // later in OpenELIS by the lab user, so leave collectionDate unset. receivedTimestamp is set
        // to now (order received by the lab) because sample.received_date is NOT NULL. Caller-supplied
        // collectionDate is ignored for this reason.
        if (collectionDate != null) {
            LogEvent.logDebug(this.getClass().getSimpleName(), "createProgramSampleFromImport",
                    "ignoring collectionDate for imported order " + externalOrderId
                            + "; sample collection is recorded later by the lab");
        }
        java.sql.Date enteredDate = DateUtil.getNowAsSqlDate();
        Timestamp receivedTimestamp = DateUtil.getNowAsTimestamp();

        // Sample
        Sample sample = new Sample();
        sample.setSysUserId(serviceUserId);
        sample.setEnteredDate(enteredDate);
        sample.setReceivedTimestamp(receivedTimestamp);
        // intentionally not set: collectionDate — lab collects later
        sample.setReferringId(externalOrderId);
        sample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
        sample.setStatusId(statusService.getStatusID(OrderStatus.Entered));
        if (priority != null) {
            sample.setPriority(priority);
        }
        sample.setFhirUuid(UUID.randomUUID());
        // insertDataWithAccessionNumber does not itself generate the accession number, so reserve the
        // next one from the configured generator (accession_number is NOT NULL).
        IAccessionNumberGenerator accessionGenerator = AccessionNumberUtil.getMainAccessionNumberGenerator();
        if (accessionGenerator == null) {
            throw new IllegalStateException(
                    "no accession number generator configured; cannot create " + program.getProgramName() + " case");
        }
        sample.setAccessionNumber(accessionGenerator.getNextAvailableAccessionNumber("", true));
        sampleService.insertDataWithAccessionNumber(sample);

        // Program sample (e.g. PathologySample), linked to the already-imported
        // questionnaire response. Cytopathology subtype comes from the ordered test/LOINC
        // (preferred) with sample-type text as a fallback for older orders.
        ProgramSample programSample = newProgramSampleForProgram(program, test, programSubtypeText);
        programSample.setProgram(program);
        programSample.setSample(sample);
        programSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);
        programSample.setSysUserId(serviceUserId);
        saveProgramSample(programSample);

        // SampleItem
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSysUserId(serviceUserId);
        sampleItem.setSample(sample);
        // intentionally not set: collectionDate — lab collects later
        sampleItem.setTypeOfSample(resolveTypeOfSample(test));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
        sampleItem.setFhirUuid(UUID.randomUUID());
        sampleItemService.insert(sampleItem);

        // Analysis for the program test
        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType(DEFAULT_ANALYSIS_TYPE);
        analysis.setSampleItem(sampleItem);
        analysis.setSysUserId(serviceUserId);
        analysis.setRevision(ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision"));
        analysis.setStartedDate(enteredDate);
        analysis.setStatusId(receptionApprovalSupport.resolveInitialAnalysisStatusId(false));
        analysis.setTestSection(test.getTestSection());
        analysis.setFhirUuid(UUID.randomUUID());
        analysisService.insert(analysis);

        // SampleHuman links the patient to the sample
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSysUserId(serviceUserId);
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patient.getId());
        if (requestingProvider != null && !GenericValidator.isBlankOrNull(requestingProvider.getId())) {
            sampleHuman.setProviderId(requestingProvider.getId());
        }
        sampleHumanService.insert(sampleHuman);

        linkRequestingProvider(sample, requestingProvider, serviceUserId);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createProgramSampleFromImport",
                "created " + program.getProgramName() + " case for imported order " + externalOrderId
                        + " with accession " + sample.getAccessionNumber()
                        + " (sample not collected yet; collectionDate unset)");
    }

    /**
     * Persists the ordering physician so Reception can show Requesting physician
     * (SampleOrderService reads sample_requester of type provider).
     */
    private void linkRequestingProvider(Sample sample, Provider requestingProvider, String serviceUserId) {
        if (requestingProvider == null || requestingProvider.getPerson() == null
                || GenericValidator.isBlankOrNull(requestingProvider.getPerson().getId())) {
            return;
        }
        SampleRequester sampleRequester = new SampleRequester();
        sampleRequester.setSampleId(Long.parseLong(sample.getId()));
        sampleRequester.setRequesterId(Long.parseLong(requestingProvider.getPerson().getId()));
        sampleRequester.setRequesterTypeId(TableIdService.getInstance().PROVIDER_REQUESTER_TYPE_ID);
        sampleRequester.setSysUserId(serviceUserId);
        sampleRequesterService.insert(sampleRequester);
    }

    private TypeOfSample resolveTypeOfSample(Test test) {
        List<TypeOfSample> types = typeOfSampleService.getTypeOfSampleForTest(test.getId());
        if (types == null || types.isEmpty()) {
            // Fail fast: a SampleItem with no type breaks later case completion. Seed
            // sampletype_test for the program test (see liquibase 027) before routing.
            throw new IllegalStateException(
                    "no sample type configured for program test " + test.getId() + " (" + test.getName()
                            + "); cannot create program case");
        }
        return types.get(0);
    }

    /**
     * Package-private for unit testing. Maps the stable program code from
     * {@code programs/*.json} (PATH / IHC / CYTO) to the matching program-sample entity.
     */
    ProgramSample newProgramSampleForProgram(Program program) {
        return newProgramSampleForProgram(program, null, null);
    }

    ProgramSample newProgramSampleForProgram(Program program, String programSubtypeText) {
        return newProgramSampleForProgram(program, null, programSubtypeText);
    }

    ProgramSample newProgramSampleForProgram(Program program, Test test, String programSubtypeText) {
        // Use the stable program code from programs/*.json (PATH / IHC / CYTO), not the
        // display name — names can be renamed or localized and would silently fall through.
        String code = program.getCode() == null ? "" : program.getCode().trim();
        switch (code) {
        case "PATH":
            PathologySample pathologySample = new PathologySample();
            pathologySample.setStatus(PathologySample.PathologyStatus.RECEIVED);
            pathologySample.setSubtype(resolvePathologySubtype(test, programSubtypeText));
            return pathologySample;
        case "IHC":
            return new ImmunohistochemistrySample();
        case "CYTO":
            CytologySample cytologySample = new CytologySample();
            cytologySample.setStatus(CytologySample.CytologyStatus.RECEIVED);
            cytologySample.setSubtype(resolveCytologySubtype(test, programSubtypeText));
            return cytologySample;
        default:
            throw new IllegalStateException(
                    "unsupported program code '" + code + "' for program " + program.getProgramName()
                            + "; cannot create program case");
        }
    }

    /**
     * Prefer the ordered test's LOINC (one TestOrder per histopathology sample type). Fall back to
     * sample-type text for older orders.
     */
    static PathologySample.PathologySubtype resolvePathologySubtype(Test test, String programSubtypeText) {
        PathologySample.PathologySubtype fromLoinc = resolvePathologySubtypeFromLoinc(
                test == null ? null : test.getLoinc());
        if (fromLoinc != null) {
            return fromLoinc;
        }
        return resolvePathologySubtypeFromText(programSubtypeText);
    }

    /** Package-private for unit testing. */
    static PathologySample.PathologySubtype resolvePathologySubtypeFromLoinc(String loinc) {
        if (GenericValidator.isBlankOrNull(loinc)) {
            return null;
        }
        switch (loinc.trim()) {
        case "97005-7":
            return PathologySample.PathologySubtype.FROZEN;
        case "11529-5":
        case "22637-3": // legacy Morphology — Biopsy rail
            return PathologySample.PathologySubtype.BIOPSY;
        default:
            return null;
        }
    }

    static PathologySample.PathologySubtype resolvePathologySubtypeFromText(String programSubtypeText) {
        if (GenericValidator.isBlankOrNull(programSubtypeText)) {
            return PathologySample.PathologySubtype.BIOPSY;
        }
        String normalized = programSubtypeText.toLowerCase().trim();
        if (normalized.contains("frozen")) {
            return PathologySample.PathologySubtype.FROZEN;
        }
        return PathologySample.PathologySubtype.BIOPSY;
    }

    /**
     * Prefer the ordered test's LOINC (one TestOrder per cytopathology sample type). Fall back to
     * sample-type text from supportingInfo for older orders that still share one LOINC.
     */
    static CytologySample.CytologySubtype resolveCytologySubtype(Test test, String programSubtypeText) {
        CytologySample.CytologySubtype fromLoinc = resolveCytologySubtypeFromLoinc(test == null ? null : test.getLoinc());
        if (fromLoinc != null) {
            return fromLoinc;
        }
        return resolveCytologySubtypeFromText(programSubtypeText);
    }

    /** Package-private for unit testing. */
    static CytologySample.CytologySubtype resolveCytologySubtypeFromLoinc(String loinc) {
        if (GenericValidator.isBlankOrNull(loinc)) {
            return null;
        }
        switch (loinc.trim()) {
        case "33716-2":
            return CytologySample.CytologySubtype.FNAC;
        case "97003-2":
            return CytologySample.CytologySubtype.IMAGE_GUIDED_FNAC;
        case "97004-0":
            return CytologySample.CytologySubtype.FLUID;
        case "10524-7":
            return CytologySample.CytologySubtype.PAP_SMEAR;
        default:
            return null;
        }
    }

    /**
     * Maps the ordering system's sample-type text onto a cytopathology subtype. Unknown or missing
     * text falls back to FNAC.
     */
    static CytologySample.CytologySubtype resolveCytologySubtypeFromText(String programSubtypeText) {
        if (GenericValidator.isBlankOrNull(programSubtypeText)) {
            return CytologySample.CytologySubtype.FNAC;
        }
        String normalized = programSubtypeText.toLowerCase().trim();
        if (normalized.contains("image") || normalized.contains("guided")) {
            return CytologySample.CytologySubtype.IMAGE_GUIDED_FNAC;
        }
        if (normalized.contains("pap")) {
            return CytologySample.CytologySubtype.PAP_SMEAR;
        }
        if (normalized.contains("fluid")) {
            return CytologySample.CytologySubtype.FLUID;
        }
        return CytologySample.CytologySubtype.FNAC;
    }

    /** @deprecated use {@link #resolveCytologySubtypeFromText(String)} */
    static CytologySample.CytologySubtype resolveCytologySubtype(String programSubtypeText) {
        return resolveCytologySubtypeFromText(programSubtypeText);
    }

    private void saveProgramSample(ProgramSample programSample) {
        if (programSample instanceof PathologySample) {
            pathologySampleService.save((PathologySample) programSample);
        } else if (programSample instanceof ImmunohistochemistrySample) {
            immunohistochemistrySampleService.save((ImmunohistochemistrySample) programSample);
        } else if (programSample instanceof CytologySample) {
            cytologySampleService.save((CytologySample) programSample);
        } else {
            programSampleService.save(programSample);
        }
    }
}
