package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.provider.validation.IAccessionNumberGenerator;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.order.action.IOrderPersister;
import org.openelisglobal.dataexchange.order.action.MessagePatient;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.reception.service.ReceptionApprovalSupport;
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
    private IStatusService statusService;

    @Override
    @Transactional
    public void createProgramSampleFromImport(Program programArg, Test testArg, MessagePatient messagePatient,
            OrderPriority priority, String externalOrderId, UUID questionnaireResponseUuid, Date collectionDate) {
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

        // Program orders arrive with the specimen already collected (a pathology specimen is taken at
        // the procedure that accompanies the order), so stamp the collection date -- from the order's
        // authored date when known, else the import time -- onto the sample, sample item, and analysis.
        // Without it the sample reads as "not collected" even though the case is already at grossing.
        Timestamp collectionTimestamp = collectionDate != null ? new Timestamp(collectionDate.getTime())
                : DateUtil.getNowAsTimestamp();

        // Sample
        Sample sample = new Sample();
        sample.setSysUserId(serviceUserId);
        sample.setEnteredDate(DateUtil.getNowAsSqlDate());
        sample.setReceivedTimestamp(DateUtil.getNowAsTimestamp());
        sample.setCollectionDate(collectionTimestamp);
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
        // questionnaire response
        ProgramSample programSample = newProgramSampleForProgram(program);
        programSample.setProgram(program);
        programSample.setSample(sample);
        programSample.setQuestionnaireResponseUuid(questionnaireResponseUuid);
        programSample.setSysUserId(serviceUserId);
        saveProgramSample(programSample);

        // SampleItem
        SampleItem sampleItem = new SampleItem();
        sampleItem.setSysUserId(serviceUserId);
        sampleItem.setSample(sample);
        sampleItem.setCollectionDate(collectionTimestamp);
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
        analysis.setStartedDate(new java.sql.Date(collectionTimestamp.getTime()));
        analysis.setStatusId(receptionApprovalSupport.resolveInitialAnalysisStatusId(false));
        analysis.setTestSection(test.getTestSection());
        analysis.setFhirUuid(UUID.randomUUID());
        analysisService.insert(analysis);

        // SampleHuman links the patient to the sample
        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSysUserId(serviceUserId);
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patient.getId());
        sampleHumanService.insert(sampleHuman);

        LogEvent.logInfo(this.getClass().getSimpleName(), "createProgramSampleFromImport",
                "created " + program.getProgramName() + " case for imported order " + externalOrderId
                        + " with accession " + sample.getAccessionNumber());
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
        // Use the stable program code from programs/*.json (PATH / IHC / CYTO), not the
        // display name — names can be renamed or localized and would silently fall through.
        String code = program.getCode() == null ? "" : program.getCode().trim();
        switch (code) {
        case "PATH":
            return new PathologySample();
        case "IHC":
            return new ImmunohistochemistrySample();
        case "CYTO":
            return new CytologySample();
        default:
            throw new IllegalStateException(
                    "unsupported program code '" + code + "' for program " + program.getProgramName()
                            + "; cannot create program case");
        }
    }

    private void saveProgramSample(ProgramSample programSample) {
        if (programSample instanceof PathologySample) {
            pathologySampleService.save((PathologySample) programSample);
        } else if (programSample instanceof ImmunohistochemistrySample) {
            immunohistochemistrySampleService.save((ImmunohistochemistrySample) programSample);
        } else {
            programSampleService.save(programSample);
        }
    }
}
