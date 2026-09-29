package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
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
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.dao.PathologySampleDAO;
import org.openelisglobal.program.util.ProgramSampleSearch;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion;
import org.openelisglobal.program.valueholder.pathology.PathologyConclusion.ConclusionType;
import org.openelisglobal.program.valueholder.pathology.PathologyRead;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestStatus;
import org.openelisglobal.program.valueholder.pathology.PathologyRequest.RequestType;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique;
import org.openelisglobal.program.valueholder.pathology.PathologyTechnique.TechniqueType;
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
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PathologySampleServiceImpl extends AuditableBaseObjectServiceImpl<PathologySample, Integer>
        implements PathologySampleService {

    @Autowired
    protected PathologySampleDAO baseObjectDAO;

    @Autowired
    protected SystemUserService systemUserService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private LogbookResultsPersistService logbookResultsPersistService;

    @Autowired
    private TestService testService;

    @Autowired
    private NoteService noteService;

    @Autowired
    private ImmunohistochemistrySampleService immunohistochemistrySampleService;

    @Autowired
    private TestSectionService testSectionService;

    @Autowired
    private FhirTransformService fhirTransformService;

    PathologySampleServiceImpl() {
        super(PathologySample.class);
        this.auditTrailLog = true;
    }

    @Override
    protected PathologySampleDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    public List<PathologySample> getWithStatus(List<PathologyStatus> statuses) {
        return baseObjectDAO.getWithStatus(statuses);
    }

    @Transactional
    @Override
    public void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        pathologySample.setTechnician(systemUser);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        pathologySample.setPathologist(systemUser);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Override
    public Long getCountWithStatus(List<PathologyStatus> statuses) {
        return baseObjectDAO.getCountWithStatus(statuses);
    }

    @Override
    public Long getCountUnassigned() {
        return baseObjectDAO.getCountUnassigned();
    }

    @Override
    public List<PathologySample> searchUnassigned(String searchTerm) {
        List<PathologySample> pathologySamples = baseObjectDAO.getUnassigned();
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                pathologySamples = baseObjectDAO.searchUnassignedWithAccessionNumber(searchTerm);
            } else {
                List<PathologySample> filtered = new ArrayList<>();
                pathologySamples.forEach(pathologySample -> {
                    Sample caseSample = pathologySample.getSample();
                    Patient patient = sampleService.getPatient(caseSample);
                    if (ProgramSampleSearch.matchesPatientOrAccession(caseSample, patient, searchTerm)) {
                        filtered.add(pathologySample);
                    }
                });
                pathologySamples = filtered;
            }
        }
        return pathologySamples;
    }

    @Override
    public Long getOpenCaseloadForPathologist(String pathologistId) {
        return baseObjectDAO.getOpenCaseloadForPathologist(pathologistId);
    }

    @Transactional
    @Override
    public void confirmReceived(Integer pathologySampleId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        Sample sample = pathologySample.getSample();
        Timestamp now = DateUtil.getNowAsTimestamp();

        if (sample.getCollectionDate() == null) {
            sample.setCollectionDate(now);
            sample.setSysUserId(curUserId);
            sampleService.update(sample);

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

        if (pathologySample.getStatus() == PathologyStatus.RECEIVED) {
            pathologySample.setStatus(PathologyStatus.GROSSING);
            pathologySample.setSysUserId(curUserId);
            update(pathologySample);
        }
    }

    @Transactional
    @Override
    public void sendToProcessing(Integer pathologySampleId, String grossExam, List<PathologyBlock> blocks,
            String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() != PathologyStatus.GROSSING
                && pathologySample.getStatus() != PathologyStatus.RECEIVED) {
            // Already past Grossing (PROCESSING / SLICING or later) — leave as-is.
            return;
        }
        if (pathologySample.getStatus() != PathologyStatus.GROSSING) {
            throw new IllegalArgumentException("case must be in GROSSING before send to processing");
        }

        boolean frozen = pathologySample.isFrozen();
        if (!frozen && (blocks == null || blocks.isEmpty())) {
            throw new IllegalArgumentException("at least one cassette is required");
        }

        pathologySample.setGrossExam(grossExam);
        if (pathologySample.getBlocks() == null) {
            pathologySample.setBlocks(new ArrayList<>());
        } else {
            pathologySample.getBlocks().removeAll(pathologySample.getBlocks());
        }
        if (blocks != null) {
            for (PathologyBlock block : blocks) {
                block.setId(null);
                pathologySample.getBlocks().add(block);
            }
        }
        if (frozen && pathologySample.getBlocks().isEmpty()) {
            PathologyBlock cryoBlock = new PathologyBlock();
            cryoBlock.setLocation("FS");
            pathologySample.getBlocks().add(cryoBlock);
        }

        if (frozen) {
            // Cryostat path: skip PROCESSING / EMBEDDING → Microtomy (cryotomy).
            pathologySample.setStatus(PathologyStatus.SLICING);
        } else {
            if (pathologySample.getProcessingStartedAt() == null) {
                pathologySample.setProcessingStartedAt(DateUtil.getNowAsTimestamp());
            }
            pathologySample.setStatus(PathologyStatus.PROCESSING);
        }
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void markProcessingComplete(Integer pathologySampleId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() != PathologyStatus.PROCESSING
                && pathologySample.getStatus() != PathologyStatus.GROSSING
                && pathologySample.getStatus() != PathologyStatus.RECEIVED
                && pathologySample.getStatus() != PathologyStatus.CUTTING) {
            // Already at EMBEDDING or later — leave as-is.
            return;
        }
        if (pathologySample.getStatus() != PathologyStatus.PROCESSING) {
            throw new IllegalArgumentException("case must be in PROCESSING before mark complete");
        }

        pathologySample.setStatus(PathologyStatus.EMBEDDING);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void markBlockEmbedded(Integer pathologySampleId, Integer blockId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() != PathologyStatus.EMBEDDING
                && pathologySample.getStatus() != PathologyStatus.PROCESSING
                && pathologySample.getStatus() != PathologyStatus.GROSSING
                && pathologySample.getStatus() != PathologyStatus.RECEIVED
                && pathologySample.getStatus() != PathologyStatus.CUTTING) {
            // Already at SLICING or later — leave as-is.
            return;
        }
        if (pathologySample.getStatus() != PathologyStatus.EMBEDDING) {
            throw new IllegalArgumentException("case must be in EMBEDDING before marking a cassette embedded");
        }
        if (blockId == null) {
            throw new IllegalArgumentException("block id is required");
        }
        if (pathologySample.getBlocks() == null || pathologySample.getBlocks().isEmpty()) {
            throw new IllegalArgumentException("case has no cassettes to embed");
        }

        PathologyBlock target = null;
        for (PathologyBlock block : pathologySample.getBlocks()) {
            if (blockId.equals(block.getId())) {
                target = block;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("cassette not found on this case");
        }

        if (target.getEmbeddedAt() == null) {
            target.setEmbeddedAt(DateUtil.getNowAsTimestamp());
            target.setSysUserId(curUserId);
        }

        boolean allEmbedded = pathologySample.getBlocks().stream().allMatch(b -> b.getEmbeddedAt() != null);
        if (allEmbedded) {
            pathologySample.setStatus(PathologyStatus.SLICING);
        }
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    /** Default planned slides per block for Microtomy (PDF Step 7). */
    private static final int PLANNED_SLIDES_PER_BLOCK = 1;

    @Transactional
    @Override
    public void cutSlide(Integer pathologySampleId, Integer blockId, String curUserId) {
        cutSlide(pathologySampleId, blockId, null, PathologySlide.SlideRole.PATIENT, curUserId);
    }

    @Transactional
    @Override
    public void cutSlide(Integer pathologySampleId, Integer blockId, String stainType,
            PathologySlide.SlideRole slideRole, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        requireSlicing(pathologySample);
        PathologyBlock block = findBlock(pathologySample, blockId);

        if (pathologySample.getSlides() == null) {
            pathologySample.setSlides(new ArrayList<>());
        }

        int nextSlideNumber = 1;
        for (PathologySlide existing : pathologySample.getSlides()) {
            if (blockId.equals(existing.getPathologyBlockId()) && existing.getSlideNumber() != null
                    && existing.getSlideNumber() >= nextSlideNumber) {
                nextSlideNumber = existing.getSlideNumber() + 1;
            }
        }

        String blockSuffix = StringUtils.isNotBlank(block.getLocation()) ? block.getLocation()
                : ("A" + (block.getBlockNumber() != null ? block.getBlockNumber() : blockId));
        PathologySlide slide = new PathologySlide();
        slide.setPathologyBlockId(blockId);
        slide.setSlideNumber(nextSlideNumber);
        slide.setLocation(blockSuffix + "." + nextSlideNumber);
        slide.setStainType(stainType);
        slide.setSlideRole(slideRole == null ? PathologySlide.SlideRole.PATIENT : slideRole);
        slide.setSysUserId(curUserId);
        pathologySample.getSlides().add(slide);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void confirmSlide(Integer pathologySampleId, Integer slideId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() != PathologyStatus.SLICING
                && pathologySample.getStatus() != PathologyStatus.EMBEDDING
                && pathologySample.getStatus() != PathologyStatus.PROCESSING
                && pathologySample.getStatus() != PathologyStatus.GROSSING
                && pathologySample.getStatus() != PathologyStatus.RECEIVED
                && pathologySample.getStatus() != PathologyStatus.CUTTING) {
            // Already at STAINING or later — leave as-is.
            return;
        }
        requireSlicing(pathologySample);
        if (slideId == null) {
            throw new IllegalArgumentException("slide id is required");
        }

        PathologySlide target = null;
        if (pathologySample.getSlides() != null) {
            for (PathologySlide slide : pathologySample.getSlides()) {
                if (slideId.equals(slide.getId())) {
                    target = slide;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("slide not found on this case");
        }
        if (target.getPathologyBlockId() == null) {
            throw new IllegalArgumentException("slide is not linked to a block");
        }

        if (target.getConfirmedAt() == null) {
            target.setConfirmedAt(DateUtil.getNowAsTimestamp());
            target.setSysUserId(curUserId);
        }

        if (isMicrotomyComplete(pathologySample)) {
            pathologySample.setStatus(PathologyStatus.STAINING);
        }
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void markSlideStained(Integer pathologySampleId, Integer slideId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() != PathologyStatus.STAINING
                && pathologySample.getStatus() != PathologyStatus.SLICING
                && pathologySample.getStatus() != PathologyStatus.EMBEDDING
                && pathologySample.getStatus() != PathologyStatus.PROCESSING
                && pathologySample.getStatus() != PathologyStatus.GROSSING
                && pathologySample.getStatus() != PathologyStatus.RECEIVED
                && pathologySample.getStatus() != PathologyStatus.CUTTING) {
            // Already at READY_PATHOLOGIST or later — leave as-is.
            return;
        }
        if (pathologySample.getStatus() != PathologyStatus.STAINING) {
            throw new IllegalArgumentException("case must be in STAINING before marking a slide stained");
        }
        if (slideId == null) {
            throw new IllegalArgumentException("slide id is required");
        }

        PathologySlide target = null;
        if (pathologySample.getSlides() != null) {
            for (PathologySlide slide : pathologySample.getSlides()) {
                if (slideId.equals(slide.getId())) {
                    target = slide;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("slide not found on this case");
        }
        if (target.getPathologyBlockId() == null || target.getConfirmedAt() == null) {
            throw new IllegalArgumentException("slide must be a confirmed microtomy slide");
        }

        if (target.getStainedAt() == null) {
            target.setStainedAt(DateUtil.getNowAsTimestamp());
            target.setSysUserId(curUserId);
        }

        if (isStainingComplete(pathologySample)) {
            pathologySample.setStatus(PathologyStatus.READY_PATHOLOGIST);
            // A special-stain round just finished — close its open request(s).
            completeOpenRequests(pathologySample);
        }
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void saveReadDraft(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        requireReadyForRead(pathologySample);
        applyReadFindings(pathologySample, microscopyExam, conclusionText, conclusionDictionaryIds, curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void signOut(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));

        if (pathologySample.getStatus() == PathologyStatus.COMPLETED) {
            return;
        }
        requireReadyForRead(pathologySample);
        applyReadFindings(pathologySample, microscopyExam, conclusionText, conclusionDictionaryIds, curUserId);
        finalizeActiveRound(pathologySample, curUserId);
        completeOpenRequests(pathologySample);

        PathologySampleForm form = new PathologySampleForm();
        form.setSystemUserId(curUserId);
        form.setConclusionText(conclusionText);
        form.setRelease(true);
        validatePathologySample(pathologySample, form);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void requestSpecialStains(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, List<String> stainNames, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        if (pathologySample.isFrozen()) {
            throw new IllegalArgumentException("special stain is not available on Frozen section cases");
        }
        requireReadyForRead(pathologySample);

        // Capture and close the current read round before the case leaves the
        // pathologist.
        applyReadFindings(pathologySample, microscopyExam, conclusionText, conclusionDictionaryIds, curUserId);
        finalizeActiveRound(pathologySample, curUserId);

        if (stainNames != null) {
            for (String stain : stainNames) {
                if (!GenericValidator.isBlankOrNull(stain)) {
                    pathologySample.getTechniques().add(createTechnique(stain, TechniqueType.TEXT));
                }
            }
        }
        String requestValue = (stainNames == null || stainNames.isEmpty()) ? "Special stain"
                : StringUtils.join(stainNames.stream().filter(s -> !GenericValidator.isBlankOrNull(s))
                        .collect(Collectors.toList()), ", ");
        pathologySample.getRequests().add(createRequest(requestValue, RequestType.TEXT, RequestStatus.OPENED));

        pathologySample.setStatus(PathologyStatus.ADDITIONAL_REQUEST);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    @Transactional
    @Override
    public void startSpecialStain(Integer pathologySampleId, String curUserId) {
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        if (pathologySample.getStatus() == PathologyStatus.SLICING
                || pathologySample.getStatus() == PathologyStatus.STAINING) {
            // Special stain already in progress — leave as-is.
            return;
        }
        if (pathologySample.getStatus() != PathologyStatus.ADDITIONAL_REQUEST) {
            throw new IllegalArgumentException("case must be in ADDITIONAL_REQUEST to start a special stain");
        }
        // Re-enter Microtomy so the tech can cut a new section from the existing block.
        pathologySample.setStatus(PathologyStatus.SLICING);
        pathologySample.setSysUserId(curUserId);
        update(pathologySample);
    }

    private void requireReadyForRead(PathologySample pathologySample) {
        PathologyStatus status = pathologySample.getStatus();
        if (status != PathologyStatus.READY_PATHOLOGIST && status != PathologyStatus.ADDITIONAL_REQUEST) {
            throw new IllegalArgumentException("case must be ready for pathologist read");
        }
    }

    /**
     * Updates microscopy + conclusions only. Leaves blocks, slides, techniques,
     * requests, reports untouched so Microtomy/Staining metadata is preserved.
     */
    private void applyReadFindings(PathologySample pathologySample, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, String curUserId) {
        pathologySample.setMicroscopyExam(microscopyExam);
        if (pathologySample.getConclusions() == null) {
            pathologySample.setConclusions(new ArrayList<>());
        } else {
            pathologySample.getConclusions().removeAll(pathologySample.getConclusions());
        }
        pathologySample.getConclusions().add(createConclusion(conclusionText, ConclusionType.TEXT));
        if (conclusionDictionaryIds != null) {
            for (String dictionaryId : conclusionDictionaryIds) {
                if (!GenericValidator.isBlankOrNull(dictionaryId)) {
                    pathologySample.getConclusions().add(createConclusion(dictionaryId, ConclusionType.DICTIONARY));
                }
            }
        }
        recordActiveRound(pathologySample, microscopyExam, conclusionText, conclusionDictionaryIds);
        pathologySample.setSysUserId(curUserId);
    }

    /**
     * Writes the current findings into the case's active (non-finalized) read
     * round, creating a new round if the previous one is already finalized. Rounds
     * are append-only history; the sample's own microscopyExam/conclusions remain
     * the "latest" mirror used by the FHIR sign-out path.
     */
    private void recordActiveRound(PathologySample pathologySample, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds) {
        if (pathologySample.getReads() == null) {
            pathologySample.setReads(new ArrayList<>());
        }
        PathologyRead round = getActiveRound(pathologySample);
        if (round == null) {
            round = new PathologyRead();
            round.setRoundNumber(nextRoundNumber(pathologySample));
            round.setFinalized(false);
            pathologySample.getReads().add(round);
        }
        round.setMicroscopyExam(microscopyExam);
        round.setConclusionText(conclusionText);
        if (conclusionDictionaryIds == null) {
            round.setConclusionDictionaryIds(null);
        } else {
            round.setConclusionDictionaryIds(StringUtils.join(conclusionDictionaryIds.stream()
                    .filter(id -> !GenericValidator.isBlankOrNull(id)).collect(Collectors.toList()), ","));
        }
    }

    /**
     * The latest read round that has not been finalized yet, or null when every
     * round is closed.
     */
    private PathologyRead getActiveRound(PathologySample pathologySample) {
        if (pathologySample.getReads() == null) {
            return null;
        }
        return pathologySample.getReads().stream().filter(r -> !Boolean.TRUE.equals(r.getFinalized()))
                .max(Comparator.comparingInt(r -> r.getRoundNumber() == null ? 0 : r.getRoundNumber())).orElse(null);
    }

    private int nextRoundNumber(PathologySample pathologySample) {
        if (pathologySample.getReads() == null || pathologySample.getReads().isEmpty()) {
            return 1;
        }
        return pathologySample.getReads().stream().mapToInt(r -> r.getRoundNumber() == null ? 0 : r.getRoundNumber())
                .max().orElse(0) + 1;
    }

    /**
     * Closes the active round (stamps reviewer/time) so the next read opens a fresh
     * round.
     */
    private void finalizeActiveRound(PathologySample pathologySample, String curUserId) {
        PathologyRead round = getActiveRound(pathologySample);
        if (round != null) {
            round.setFinalized(true);
            round.setReviewedAt(DateUtil.getNowAsTimestamp());
            if (!GenericValidator.isBlankOrNull(curUserId)) {
                round.setReviewedBy(systemUserService.get(curUserId));
            }
        }
    }

    /**
     * Marks every OPENED special-stain request COMPLETED (called when the case
     * returns to review).
     */
    private void completeOpenRequests(PathologySample pathologySample) {
        if (pathologySample.getRequests() == null) {
            return;
        }
        pathologySample.getRequests().stream().filter(r -> r.getStatus() == RequestStatus.OPENED)
                .forEach(r -> r.setStatus(RequestStatus.COMPLETED));
    }

    private void requireSlicing(PathologySample pathologySample) {
        if (pathologySample.getStatus() != PathologyStatus.SLICING) {
            throw new IllegalArgumentException("case must be in SLICING (microtomy) for this action");
        }
    }

    private PathologyBlock findBlock(PathologySample pathologySample, Integer blockId) {
        if (blockId == null) {
            throw new IllegalArgumentException("block id is required");
        }
        if (pathologySample.getBlocks() == null) {
            throw new IllegalArgumentException("case has no blocks");
        }
        for (PathologyBlock block : pathologySample.getBlocks()) {
            if (blockId.equals(block.getId())) {
                return block;
            }
        }
        throw new IllegalArgumentException("block not found on this case");
    }

    /**
     * Each block has at least {@link #PLANNED_SLIDES_PER_BLOCK} confirmed slides,
     * and every block-linked slide on the case is confirmed (extra cuts must be
     * confirmed too).
     */
    private boolean isMicrotomyComplete(PathologySample pathologySample) {
        if (pathologySample.getBlocks() == null || pathologySample.getBlocks().isEmpty()) {
            return false;
        }
        List<PathologySlide> slides = pathologySample.getSlides() == null ? new ArrayList<>()
                : pathologySample.getSlides();
        for (PathologyBlock block : pathologySample.getBlocks()) {
            List<PathologySlide> forBlock = slides.stream()
                    .filter(s -> block.getId() != null && block.getId().equals(s.getPathologyBlockId()))
                    .collect(Collectors.toList());
            long confirmed = forBlock.stream().filter(s -> s.getConfirmedAt() != null).count();
            if (confirmed < PLANNED_SLIDES_PER_BLOCK) {
                return false;
            }
            if (confirmed != forBlock.size()) {
                return false;
            }
        }
        return true;
    }

    /** Every Microtomy-confirmed (block-linked) slide has stainedAt set. */
    private boolean isStainingComplete(PathologySample pathologySample) {
        List<PathologySlide> slides = pathologySample.getSlides() == null ? new ArrayList<>()
                : pathologySample.getSlides();
        List<PathologySlide> toStain = slides.stream()
                .filter(s -> s.getPathologyBlockId() != null && s.getConfirmedAt() != null)
                .collect(Collectors.toList());
        if (toStain.isEmpty()) {
            return false;
        }
        return toStain.stream().allMatch(s -> s.getStainedAt() != null);
    }

    private PathologySample copyPathologySample(PathologySample oldPathologySample) {
        PathologySample pathologySample = new PathologySample();
        pathologySample.setBlocks(new ArrayList<>(oldPathologySample.getBlocks()));
        pathologySample.setConclusions(new ArrayList<>(oldPathologySample.getConclusions()));
        pathologySample.setGrossExam(oldPathologySample.getGrossExam());
        pathologySample.setId(oldPathologySample.getId());
        pathologySample.setLastupdated(oldPathologySample.getLastupdated());
        pathologySample.setMicroscopyExam(oldPathologySample.getMicroscopyExam());
        pathologySample.setProcessingStartedAt(oldPathologySample.getProcessingStartedAt());
        pathologySample.setPathologist(oldPathologySample.getPathologist());
        pathologySample.setProgram(oldPathologySample.getProgram());
        pathologySample.setQuestionnaireResponseUuid(oldPathologySample.getQuestionnaireResponseUuid());
        pathologySample.setReports(new ArrayList<>(oldPathologySample.getReports()));
        pathologySample.setReads(oldPathologySample.getReads() == null ? new ArrayList<>()
                : new ArrayList<>(oldPathologySample.getReads()));
        pathologySample.setRequests(new ArrayList<>(oldPathologySample.getRequests()));
        pathologySample.setSample(oldPathologySample.getSample());
        pathologySample.setSlides(new ArrayList<>(oldPathologySample.getSlides()));
        pathologySample.setStatus(oldPathologySample.getStatus());
        pathologySample.setSubtype(oldPathologySample.getSubtype());
        pathologySample.setLinkedFromPathologySampleId(oldPathologySample.getLinkedFromPathologySampleId());
        pathologySample.setTechnician(oldPathologySample.getTechnician());
        pathologySample.setTechniques(new ArrayList<>(oldPathologySample.getTechniques()));
        return pathologySample;
    }

    @Transactional
    @Override
    public void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form) {
        // copying is so we get an object that is detached from hibernate
        PathologySample pathologySample = copyPathologySample(get(pathologySampleId));
        pathologySample.setSysUserId(form.getSystemUserId());
        if (!GenericValidator.isBlankOrNull(form.getAssignedPathologistId())) {
            pathologySample.setPathologist(systemUserService.get(form.getAssignedPathologistId()));
        }
        if (!GenericValidator.isBlankOrNull(form.getAssignedTechnicianId())) {
            pathologySample.setTechnician(systemUserService.get(form.getAssignedTechnicianId()));
        }
        pathologySample.setStatus(form.getStatus());
        pathologySample.getBlocks().removeAll(pathologySample.getBlocks());
        if (form.getBlocks() != null)
            form.getBlocks().stream().forEach(e -> e.setId(null));
        pathologySample.getBlocks().addAll(form.getBlocks());
        pathologySample.getSlides().removeAll(pathologySample.getSlides());
        if (form.getSlides() != null)
            form.getSlides().stream().forEach(e -> e.setId(null));
        pathologySample.getSlides().addAll(form.getSlides());
        pathologySample.setGrossExam(form.getGrossExam());
        pathologySample.setMicroscopyExam(form.getMicroscopyExam());
        pathologySample.getConclusions().removeAll(pathologySample.getConclusions());
        pathologySample.getConclusions().add(createConclusion(form.getConclusionText(), ConclusionType.TEXT));
        if (form.getConclusions() != null)
            pathologySample.getConclusions().addAll(form.getConclusions().stream()
                    .map(e -> createConclusion(e, ConclusionType.DICTIONARY)).collect(Collectors.toList()));
        pathologySample.getRequests().removeAll(pathologySample.getRequests());
        if (form.getRequests() != null) {
            pathologySample.getRequests()
                    .addAll(form.getRequests().stream()
                            .map(e -> createRequest(e.getValue(), RequestType.DICTIONARY, e.getStatus()))
                            .collect(Collectors.toList()));
        }
        pathologySample.getTechniques().removeAll(pathologySample.getTechniques());
        if (form.getTechniques() != null)
            pathologySample.getTechniques().addAll(form.getTechniques().stream()
                    .map(e -> createTechnique(e, TechniqueType.DICTIONARY)).collect(Collectors.toList()));
        pathologySample.getReports().removeAll(pathologySample.getReports());
        if (form.getReports() != null)
            form.getReports().stream().forEach(e -> e.setId(null));
        pathologySample.getReports().addAll(form.getReports());
        if (form.getRelease()) {
            validatePathologySample(pathologySample, form);
        }
        if (form.getReferToImmunoHistoChemistry()) {
            referToImmunoHistoChemistry(pathologySample, form);
        }
        try {
            update(pathologySample);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw e;
        }
    }

    private void validatePathologySample(PathologySample pathologySample, PathologySampleForm form) {
        pathologySample.setStatus(PathologyStatus.COMPLETED);
        Sample sample = pathologySample.getSample();
        Patient patient = sampleService.getPatient(sample);
        ResultsUpdateDataSet actionDataSet = new ResultsUpdateDataSet(form.getSystemUserId());
        // Collected for the FHIR sync-back below (completes the order's referring Task
        // so the result
        // returns to the ordering physician in OpenMRS).
        List<Analysis> finalizedAnalyses = new ArrayList<>();
        ArrayList<Result> resultUpdateList = new ArrayList<>();

        ResultsLoadUtility resultsUtility = SpringContext.getBean(ResultsLoadUtility.class);
        List<TestResultItem> testResultItems = resultsUtility.getGroupedTestsForSample(sample);
        for (TestResultItem testResultItem : testResultItems) {
            if (!testResultItem.getIsGroupSeparator()) {
                if (ResultType.isTextOnlyVariant(testResultItem.getResultType())) {
                    // Return the pathologist's conclusion as the structured result value (so it
                    // reaches the
                    // ordering physician), falling back to the generic "see report" text when none
                    // was given.
                    String conclusionText = form.getConclusionText();
                    testResultItem.setResultValue(GenericValidator.isBlankOrNull(conclusionText)
                            ? MessageUtil.getMessage("result.pathology.seereport")
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

                    // analysis.setStartedDateForDisplay(testResultItem.getTestDate());

                    // This needs to be refactored -- part of the logic is in
                    // getStatusForTestResult. RetroCI over rides to whatever was set before
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

                    // this code is pulled from LogbookResultsRestController
                    // addResult(result, testResultItem, analysis, results.size() > 1,
                    // actionDataSet, useTechnicianName);
                    //
                    // if (analysisShouldBeUpdated(testResultItem, result, supportReferrals)) {
                    // updateAnalysis(testResultItem, testResultItem.getTestDate(),
                    // analysis, statusRuleSet);
                    // }
                }
                analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
                analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
                finalizedAnalyses.add(analysis);
            }
        }

        logbookResultsPersistService.persistDataSet(actionDataSet, ResultUpdateRegister.getRegisteredUpdaters(),
                form.getSystemUserId());
        // Re-load the sample as a managed entity and mark it finished so the change
        // flushes with this
        // transaction (the pathologySample here is a detached copy, so updating its
        // Sample directly trips
        // optimistic locking).
        Sample finishedSample = sampleService.get(sample.getId());
        finishedSample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));

        // Push the pathology result to the local FHIR store exactly as Result
        // Validation does: build the
        // Observation/DiagnosticReport for the finalized analysis and complete the
        // order's referring Task,
        // so OpenMRS's FetchTaskUpdates returns the result to the ordering physician.
        // The transform is
        // @Async and reads committed state, so defer it until after this transaction
        // commits (otherwise the
        // async thread can't see the analysis/results just saved). Failure must not
        // affect the completion.
        final List<Analysis> analysesForFhir = finalizedAnalyses;
        final ArrayList<Result> resultsForFhir = resultUpdateList;
        final Sample sampleForFhir = finishedSample;
        final String microscopicFindingForFhir = pathologySample.getMicroscopyExam();
        final String conclusionTextForFhir = form.getConclusionText();
        final String grossFindingForFhir = pathologySample.getGrossExam();
        final List<String> conclusionDictionaryIdsForFhir = pathologySample.getConclusions() == null ? new ArrayList<>()
                : pathologySample.getConclusions().stream().filter(c -> c.getType() == ConclusionType.DICTIONARY)
                        .map(PathologyConclusion::getValue).filter(v -> !GenericValidator.isBlankOrNull(v))
                        .collect(Collectors.toList());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    fhirTransformService.transformPersistResultValidationFhirObjects(new ArrayList<>(), analysesForFhir,
                            resultsForFhir, new ArrayList<>(), new ArrayList<>(Arrays.asList(sampleForFhir)),
                            new ArrayList<>(), microscopicFindingForFhir, conclusionTextForFhir,
                            conclusionDictionaryIdsForFhir, grossFindingForFhir);
                } catch (FhirLocalPersistingException e) {
                    LogEvent.logError(PathologySampleServiceImpl.class.getSimpleName(), "validatePathologySample",
                            "could not push pathology result to FHIR for sample " + sampleForFhir.getAccessionNumber()
                                    + ": " + e.getMessage());
                }
            }
        });

        // Frozen section leftover tissue → lab-only permanent Biopsy case (no new
        // OpenMRS order).
        if (pathologySample.isFrozen()) {
            try {
                createPermanentBiopsyAfterFrozen(pathologySample, form.getSystemUserId());
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "validatePathologySample",
                        "could not create permanent Biopsy after Frozen section " + pathologySample.getId() + ": "
                                + e.getMessage());
            }
        }
    }

    /**
     * After Frozen section sign-out, auto-create a linked Biopsy
     * {@link PathologySample} for permanent formalin processing of leftover tissue.
     * Lab-only (no EMR order / referring id).
     */
    private void createPermanentBiopsyAfterFrozen(PathologySample frozenCase, String systemUserId) {
        if (frozenCase == null || frozenCase.getId() == null || frozenCase.getSample() == null) {
            return;
        }
        Test biopsyTest = testService.getTestByDescription("Histopathology examination");
        if (biopsyTest == null) {
            List<Test> loincMatches = testService.getTestsByLoincCode("11529-5");
            if (loincMatches != null && !loincMatches.isEmpty()) {
                biopsyTest = loincMatches.get(0);
            }
        }
        if (biopsyTest == null) {
            throw new IllegalStateException(
                    "Histopathology examination test (LOINC 11529-5) not found; cannot create permanent Biopsy");
        }

        Sample frozenSample = frozenCase.getSample();
        Patient patient = sampleService.getPatient(frozenSample);
        if (patient == null) {
            throw new IllegalStateException("no patient on frozen sample " + frozenSample.getAccessionNumber());
        }

        org.openelisglobal.common.provider.validation.IAccessionNumberGenerator accessionGenerator = org.openelisglobal.sample.util.AccessionNumberUtil
                .getMainAccessionNumberGenerator();
        if (accessionGenerator == null) {
            throw new IllegalStateException("no accession number generator; cannot create permanent Biopsy");
        }

        java.sql.Date enteredDate = DateUtil.getNowAsSqlDate();
        Timestamp receivedTimestamp = DateUtil.getNowAsTimestamp();

        Sample permanentSample = new Sample();
        permanentSample.setSysUserId(systemUserId);
        permanentSample.setEnteredDate(enteredDate);
        permanentSample.setReceivedTimestamp(receivedTimestamp);
        permanentSample.setDomain(ConfigurationProperties.getInstance().getPropertyValue("domain.human"));
        permanentSample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Entered));
        permanentSample.setPriority(org.openelisglobal.sample.valueholder.OrderPriority.ROUTINE);
        permanentSample.setFhirUuid(java.util.UUID.randomUUID());
        permanentSample.setAccessionNumber(accessionGenerator.getNextAvailableAccessionNumber("", true));
        sampleService.insertDataWithAccessionNumber(permanentSample);

        PathologySample permanentCase = new PathologySample();
        permanentCase.setProgram(frozenCase.getProgram());
        permanentCase.setSample(permanentSample);
        permanentCase.setQuestionnaireResponseUuid(frozenCase.getQuestionnaireResponseUuid());
        permanentCase.setStatus(PathologyStatus.RECEIVED);
        permanentCase.setSubtype(PathologySample.PathologySubtype.BIOPSY);
        permanentCase.setLinkedFromPathologySampleId(frozenCase.getId());
        permanentCase.setPathologist(frozenCase.getPathologist());
        permanentCase.setSysUserId(systemUserId);
        save(permanentCase);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSysUserId(systemUserId);
        sampleItem.setSample(permanentSample);
        List<org.openelisglobal.typeofsample.valueholder.TypeOfSample> types = SpringContext
                .getBean(org.openelisglobal.typeofsample.service.TypeOfSampleService.class)
                .getTypeOfSampleForTest(biopsyTest.getId());
        if (types == null || types.isEmpty()) {
            throw new IllegalStateException("no sample type for Histopathology examination");
        }
        sampleItem.setTypeOfSample(types.get(0));
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(SpringContext.getBean(IStatusService.class)
                .getStatusID(org.openelisglobal.common.services.StatusService.SampleStatus.Entered));
        sampleItem.setFhirUuid(java.util.UUID.randomUUID());
        sampleItemService.insert(sampleItem);

        Analysis analysis = new Analysis();
        analysis.setTest(biopsyTest);
        analysis.setIsReportable(biopsyTest.getIsReportable());
        analysis.setAnalysisType("MANUAL");
        analysis.setSampleItem(sampleItem);
        analysis.setSysUserId(systemUserId);
        analysis.setRevision(ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision"));
        analysis.setStartedDate(enteredDate);
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        analysis.setTestSection(biopsyTest.getTestSection());
        analysis.setFhirUuid(java.util.UUID.randomUUID());
        analysisService.insert(analysis);

        org.openelisglobal.samplehuman.valueholder.SampleHuman sampleHuman = new org.openelisglobal.samplehuman.valueholder.SampleHuman();
        sampleHuman.setSysUserId(systemUserId);
        sampleHuman.setSampleId(permanentSample.getId());
        sampleHuman.setPatientId(patient.getId());
        SpringContext.getBean(org.openelisglobal.samplehuman.service.SampleHumanService.class).insert(sampleHuman);

        Note note = noteService.createSavableNote(
                analysis, NoteType.INTERNAL, "Permanent Biopsy created after Frozen section case "
                        + frozenSample.getAccessionNumber() + " (leftover tissue)",
                "Permanent after Frozen section", systemUserId);
        if (!noteService.duplicateNoteExists(note)) {
            noteService.saveAll(java.util.Collections.singletonList(note));
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "createPermanentBiopsyAfterFrozen",
                "created permanent Biopsy " + permanentSample.getAccessionNumber() + " linked from frozen case "
                        + frozenCase.getId());
    }

    private void referToImmunoHistoChemistry(PathologySample pathologySample, PathologySampleForm form) {
        List<Test> immunoHistologyTests = new ArrayList<>();
        if (!form.getImmunoHistoChemistryTestIds().isEmpty()) {
            form.getImmunoHistoChemistryTestIds().forEach(id -> {
                Test t = testService.get(id);
                if (t != null) {
                    immunoHistologyTests.add(t);
                }
            });
        }

        ImmunohistochemistrySample immunoHistoSample = immunohistochemistrySampleService
                .getByPathologySampleId(pathologySample.getId());
        if (immunoHistoSample == null) {
            immunoHistoSample = new ImmunohistochemistrySample();
        }
        immunoHistoSample.setProgram(pathologySample.getProgram());
        immunoHistoSample.setQuestionnaireResponseUuid(pathologySample.getQuestionnaireResponseUuid());
        immunoHistoSample.setSample(pathologySample.getSample());
        immunoHistoSample.setPathologySample(pathologySample);
        immunoHistoSample.setReffered(true);
        immunohistochemistrySampleService.save(immunoHistoSample);

        if (immunoHistologyTests.isEmpty()) {
            return;
        }
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(pathologySample.getSample().getId());
        if (analyses == null || analyses.isEmpty()) {
            return;
        }
        Analysis currentAnalysis = analyses.get(0);
        immunoHistologyTests.forEach(test -> {
            CreateNewAnalysis(test, currentAnalysis, pathologySample.getProgram().getProgramName(),
                    form.getSystemUserId());
        });
    }

    private void CreateNewAnalysis(Test immunoHistologyTest, Analysis currentAnalysis, String programmeName,
            String systemUserId) {
        Analysis analysis = new Analysis();
        analysis.setTest(immunoHistologyTest);
        analysis.setIsReportable(currentAnalysis.getIsReportable());
        analysis.setAnalysisType(currentAnalysis.getAnalysisType());
        analysis.setRevision(currentAnalysis.getRevision());
        analysis.setStartedDate(DateUtil.getNowAsSqlDate());
        analysis.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NotStarted));
        analysis.setParentAnalysis(currentAnalysis);
        analysis.setSampleItem(currentAnalysis.getSampleItem());
        TestSection testSection = testSectionService.getTestSectionByName("Immunohistochemistry");
        analysis.setTestSection(testSection);
        analysis.setSampleTypeName(currentAnalysis.getSampleTypeName());
        analysis.setSysUserId(systemUserId);
        analysisService.insert(analysis);

        List<Note> notes = new ArrayList<>();
        Note note = noteService.createSavableNote(analysis, NoteType.INTERNAL,
                "Refered From Pathology Programme : " + programmeName + "to Immunohistochemistry",
                "Refered to Immunohistochemistry", systemUserId);
        if (!noteService.duplicateNoteExists(note)) {
            notes.add(note);
        }
        noteService.saveAll(notes);
    }

    private PathologyConclusion createConclusion(String text, ConclusionType type) {
        PathologyConclusion conclusion = new PathologyConclusion();
        conclusion.setValue(text);
        conclusion.setType(type);
        return conclusion;
    }

    private PathologyRequest createRequest(String text, RequestType type, RequestStatus status) {
        PathologyRequest request = new PathologyRequest();
        request.setValue(text);
        request.setType(type);
        request.setStatus(status);
        return request;
    }

    private PathologyTechnique createTechnique(String text, TechniqueType type) {
        PathologyTechnique request = new PathologyTechnique();
        request.setValue(text);
        request.setType(type);
        return request;
    }

    @Override
    public List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm) {
        List<PathologySample> pathologySamples = baseObjectDAO.getWithStatus(statuses);
        if (StringUtils.isNotBlank(searchTerm)) {
            Sample sample = sampleService.getSampleByAccessionNumber(searchTerm);
            if (sample != null) {
                pathologySamples = baseObjectDAO.searchWithStatusAndAccesionNumber(statuses, searchTerm);
            } else {
                List<PathologySample> filteredpathologySamples = new ArrayList<>();
                pathologySamples.forEach(pathologySample -> {
                    Sample caseSample = pathologySample.getSample();
                    Patient patient = sampleService.getPatient(caseSample);
                    if (ProgramSampleSearch.matchesPatientOrAccession(caseSample, patient, searchTerm)) {
                        filteredpathologySamples.add(pathologySample);
                    }
                });
                pathologySamples = filteredpathologySamples;
            }
        }

        return pathologySamples;
    }

    @Override
    public Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to) {
        return baseObjectDAO.getCountWithStatusBetweenDates(statuses, from, to);
    }
}
