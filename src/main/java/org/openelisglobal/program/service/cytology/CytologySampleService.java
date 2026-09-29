package org.openelisglobal.program.service.cytology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.cytology.CytologySampleForm;
import org.openelisglobal.program.valueholder.cytology.CytologySample;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface CytologySampleService extends BaseObjectService<CytologySample, Integer> {
    List<CytologySample> getWithStatus(List<CytologyStatus> statuses);

    List<CytologySample> searchWithStatusAndTerm(List<CytologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer cytologySampleId, SystemUser systemUser, String curUserId);

    void assignCytoPathologist(Integer cytologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<CytologyStatus> statuses);

    Long getCountUnassigned();

    /** Open cases with no cytopathologist (Reception unassigned queue list). */
    List<CytologySample> searchUnassigned(String searchTerm);

    Long getOpenCaseloadForCytoPathologist(String cytoPathologistId);

    Long getCountWithStatusBetweenDates(List<CytologyStatus> statuses, Timestamp from, Timestamp to);

    void updateWithFormValues(Integer cytologySampleId, CytologySampleForm form);

    /**
     * Collection: stamp sample collectionDate, persist collection fields, create
     * default smear slide(s) when none exist, then advance RECEIVED → STAINING (or
     * CELL_BLOCK for Fluid).
     */
    void confirmCollection(Integer cytologySampleId, CytologySampleForm form, String curUserId);

    /** Fluid only: reject at Collection (clotted/autolyzed). */
    void rejectCollection(Integer cytologySampleId, String rejectionReason, String curUserId);

    /** Fluid Cell Block checklist item: centrifuge | prepare | slide. */
    void markCellBlockStep(Integer cytologySampleId, String step, String curUserId);

    /**
     * Staining: mark one smear/cell-block slide stained; when all done →
     * READY_FOR_CYTOPATHOLOGIST.
     */
    void markSlideStained(Integer cytologySampleId, Integer slideId, String curUserId);

    /**
     * Create an additional smear slide while in STAINING (or CELL_BLOCK for Fluid).
     */
    void addSmearSlide(Integer cytologySampleId, String curUserId);

    /** The read: save findings without releasing. */
    void saveReadDraft(Integer cytologySampleId, String microscopyExam, String conclusion, String conclusionText,
            String curUserId);

    /** Request repeat FNAC / smear (timestamp only; case stays ready for read). */
    void requestRepeat(Integer cytologySampleId, String curUserId);

    /** Request second opinion (timestamp only). */
    void requestSecondOpinion(Integer cytologySampleId, String curUserId);

    /** Fluid: create a referred IHC program case on the same Sample. */
    void orderIhc(Integer cytologySampleId, String curUserId);

    /** Sign-out: save findings and finalize (COMPLETED + results/FHIR). */
    void signOut(Integer cytologySampleId, String microscopyExam, String conclusion, String conclusionText,
            String curUserId);
}
