package org.openelisglobal.program.service.hematopathology;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.program.controller.hematopathology.HematopathologySampleForm;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample;
import org.openelisglobal.program.valueholder.hematopathology.HematopathologySample.HematopathologyStatus;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface HematopathologySampleService extends BaseObjectService<HematopathologySample, Integer> {

    List<HematopathologySample> getWithStatus(List<HematopathologyStatus> statuses);

    List<HematopathologySample> searchWithStatusAndTerm(List<HematopathologyStatus> statuses, String searchTerm);

    /** Open cases with no hematopathologist (reception Unassigned bucket). */
    List<HematopathologySample> searchUnassigned(String searchTerm);

    Long getCountWithStatus(List<HematopathologyStatus> statuses);

    Long getCountWithStatusBetweenDates(List<HematopathologyStatus> statuses, Timestamp from, Timestamp to);

    Long getCountUnassigned();

    Long getOpenCaseloadForHematopathologist(String hematopathologistId);

    void assignTechnician(Integer hematopathologySampleId, SystemUser systemUser, String curUserId);

    void assignHematopathologist(Integer hematopathologySampleId, SystemUser systemUser, String curUserId);

    /**
     * Collection: stamp the sample collected, keep the collection fields, then
     * advance RECEIVED → SMEAR_PREP (morphology) / PROCESSING (flow), or to
     * AWAITING_PAYMENT when the pathology payment gate holds the case.
     *
     * @return the payment status evaluated, or null when already past collection
     */
    OpenMrsPaymentStatus confirmCollection(Integer hematopathologySampleId, HematopathologySampleForm form,
            String curUserId);

    /** Re-checks payment for a held case and releases it when paid / exempted. */
    OpenMrsPaymentStatus checkPayment(Integer hematopathologySampleId, String curUserId);

    /** Reject at Collection (clotted / hemolysed / no spicules / too old). */
    void rejectCollection(Integer hematopathologySampleId, String rejectionReason, String curUserId);

    /**
     * One lab step: smearPrepared | smearStained (morphology), processing |
     * acquisition (flow cytometry). See {@link HematopathologyWorkflow}.
     */
    void markStep(Integer hematopathologySampleId, String step, String curUserId);

    /** Review &amp; report: save findings without releasing. */
    void saveReadDraft(Integer hematopathologySampleId, HematopathologySampleForm form, String curUserId);

    /** Sign-out: save findings, finalize results and push them to OpenMRS. */
    void signOut(Integer hematopathologySampleId, HematopathologySampleForm form, String curUserId);
}
