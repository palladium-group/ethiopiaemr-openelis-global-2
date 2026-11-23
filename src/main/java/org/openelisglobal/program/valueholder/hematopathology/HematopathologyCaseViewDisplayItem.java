package org.openelisglobal.program.valueholder.hematopathology;

import java.sql.Timestamp;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

/**
 * Everything the hematopathology case page shows. {@code paymentStatus} is set
 * only by the request that checked payment.
 */
public class HematopathologyCaseViewDisplayItem extends HematopathologyDisplayItem {

    private String age;
    private String sex;
    private String referringFacility;
    private String department;
    private String paymentStatus;
    private Questionnaire programQuestionnaire;
    private QuestionnaireResponse programQuestionnaireResponse;
    private String assignedTechnicianId;
    private String assignedHematopathologistId;
    private java.util.Date collectionDate;
    private String collectionNotes;
    private Boolean spiculesPresent;
    private String sampleAgeHours;
    private String rejectionReason;
    private Timestamp collectionConfirmedAt;
    private Timestamp smearPreparedAt;
    private Timestamp smearStainedAt;
    private Timestamp processingCompletedAt;
    private Timestamp acquisitionCompletedAt;
    private String smearQuality;
    private String peripheralBloodMorphology;
    private String boneMarrowMorphology;
    private String specimenViability;
    private String antibodyPanel;
    private String gatingFindings;
    private String conclusion;
    private String comment;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Questionnaire getProgramQuestionnaire() {
        return programQuestionnaire;
    }

    public void setProgramQuestionnaire(Questionnaire programQuestionnaire) {
        this.programQuestionnaire = programQuestionnaire;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedHematopathologistId() {
        return assignedHematopathologistId;
    }

    public void setAssignedHematopathologistId(String assignedHematopathologistId) {
        this.assignedHematopathologistId = assignedHematopathologistId;
    }

    public java.util.Date getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(java.util.Date collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getCollectionNotes() {
        return collectionNotes;
    }

    public void setCollectionNotes(String collectionNotes) {
        this.collectionNotes = collectionNotes;
    }

    public Boolean getSpiculesPresent() {
        return spiculesPresent;
    }

    public void setSpiculesPresent(Boolean spiculesPresent) {
        this.spiculesPresent = spiculesPresent;
    }

    public String getSampleAgeHours() {
        return sampleAgeHours;
    }

    public void setSampleAgeHours(String sampleAgeHours) {
        this.sampleAgeHours = sampleAgeHours;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getCollectionConfirmedAt() {
        return collectionConfirmedAt;
    }

    public void setCollectionConfirmedAt(Timestamp collectionConfirmedAt) {
        this.collectionConfirmedAt = collectionConfirmedAt;
    }

    public Timestamp getSmearPreparedAt() {
        return smearPreparedAt;
    }

    public void setSmearPreparedAt(Timestamp smearPreparedAt) {
        this.smearPreparedAt = smearPreparedAt;
    }

    public Timestamp getSmearStainedAt() {
        return smearStainedAt;
    }

    public void setSmearStainedAt(Timestamp smearStainedAt) {
        this.smearStainedAt = smearStainedAt;
    }

    public Timestamp getProcessingCompletedAt() {
        return processingCompletedAt;
    }

    public void setProcessingCompletedAt(Timestamp processingCompletedAt) {
        this.processingCompletedAt = processingCompletedAt;
    }

    public Timestamp getAcquisitionCompletedAt() {
        return acquisitionCompletedAt;
    }

    public void setAcquisitionCompletedAt(Timestamp acquisitionCompletedAt) {
        this.acquisitionCompletedAt = acquisitionCompletedAt;
    }

    public String getSmearQuality() {
        return smearQuality;
    }

    public void setSmearQuality(String smearQuality) {
        this.smearQuality = smearQuality;
    }

    public String getPeripheralBloodMorphology() {
        return peripheralBloodMorphology;
    }

    public void setPeripheralBloodMorphology(String peripheralBloodMorphology) {
        this.peripheralBloodMorphology = peripheralBloodMorphology;
    }

    public String getBoneMarrowMorphology() {
        return boneMarrowMorphology;
    }

    public void setBoneMarrowMorphology(String boneMarrowMorphology) {
        this.boneMarrowMorphology = boneMarrowMorphology;
    }

    public String getSpecimenViability() {
        return specimenViability;
    }

    public void setSpecimenViability(String specimenViability) {
        this.specimenViability = specimenViability;
    }

    public String getAntibodyPanel() {
        return antibodyPanel;
    }

    public void setAntibodyPanel(String antibodyPanel) {
        this.antibodyPanel = antibodyPanel;
    }

    public String getGatingFindings() {
        return gatingFindings;
    }

    public void setGatingFindings(String gatingFindings) {
        this.gatingFindings = gatingFindings;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
