package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    Long getCountUnassigned();

    /** Open cases with no pathologist (Reception unassigned queue list). */
    List<PathologySample> searchUnassigned(String searchTerm);

    Long getOpenCaseloadForPathologist(String pathologistId);

    /**
     * Marks the specimen physically collected (Sample/SampleItem.collectionDate =
     * now) and advances status from RECEIVED to GROSSING. Idempotent if already
     * collected.
     */
    void confirmReceived(Integer pathologySampleId, String curUserId);

    /**
     * Grossing Step 4: persist macroscopic description + cassette/blocks.
     * <ul>
     * <li>Biopsy: advance GROSSING → PROCESSING (requires at least one block).</li>
     * <li>Frozen section: skip paraffin processing/embedding; advance GROSSING →
     * SLICING (cryotomy). Creates a default block when none are supplied so slides
     * can be cut.</li>
     * </ul>
     */
    void sendToProcessing(Integer pathologySampleId, String grossExam, List<PathologyBlock> blocks, String curUserId);

    /**
     * Processing Step 5: mark the tissue processing run complete and advance
     * PROCESSING → EMBEDDING. Idempotent if already past PROCESSING.
     */
    void markProcessingComplete(Integer pathologySampleId, String curUserId);

    /**
     * Embedding Step 6: stamp embeddedAt on one cassette/block. When every block is
     * embedded, advances EMBEDDING → SLICING. Idempotent if the block is already
     * embedded.
     */
    void markBlockEmbedded(Integer pathologySampleId, Integer blockId, String curUserId);

    /**
     * Microtomy Step 7: create the next slide for a block (cut + ready to print).
     * Case must be in SLICING.
     */
    void cutSlide(Integer pathologySampleId, Integer blockId, String curUserId);

    /**
     * Microtomy cut labelled with a stain and role — used for special-stain rounds
     * where the new section is stained with something other than H&E (and optional
     * control slides are added).
     */
    void cutSlide(Integer pathologySampleId, Integer blockId, String stainType, PathologySlide.SlideRole slideRole,
            String curUserId);

    /**
     * Microtomy Step 7: confirm a cut slide. When every block has its planned
     * slides confirmed, advances SLICING → STAINING.
     */
    void confirmSlide(Integer pathologySampleId, Integer slideId, String curUserId);

    /**
     * Staining Step 8: mark a Microtomy-confirmed slide stained. When every such
     * slide is stained, advances STAINING → READY_PATHOLOGIST.
     */
    void markSlideStained(Integer pathologySampleId, Integer slideId, String curUserId);

    /**
     * The read: save microscopy findings + conclusions without touching
     * blocks/slides or releasing. Case must be in READY_PATHOLOGIST (or
     * ADDITIONAL_REQUEST).
     */
    void saveReadDraft(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, String curUserId);

    /**
     * The read / sign-out: save findings + conclusions, then finalize via the
     * existing release path (COMPLETED + results/FHIR). Does not rewrite
     * blocks/slides.
     */
    void signOut(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, String curUserId);

    /**
     * Pathologist requests one or more special stains from the read step. Closes
     * the current read round, records the requested stain(s) as techniques + an
     * OPENED request, and moves the case to ADDITIONAL_REQUEST (it stays on the
     * same case/accession). Case must be ready for read.
     */
    void requestSpecialStains(Integer pathologySampleId, String microscopyExam, String conclusionText,
            List<String> conclusionDictionaryIds, List<String> stainNames, String curUserId);

    /**
     * Tech picks up a special-stain request: moves ADDITIONAL_REQUEST → SLICING so
     * a new section can be cut from the existing block and stained. Idempotent once
     * staining is underway.
     */
    void startSpecialStain(Integer pathologySampleId, String curUserId);

    void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form);
}
