package org.openelisglobal.program.controller.hematopathology;

/**
 * Request body for the hematopathology case rail: Collection fields and the
 * Review &amp; report findings.
 */
public class HematopathologySampleForm {

    private String collectionNotes;
    private Boolean spiculesPresent;
    private String sampleAgeHours;
    private String rejectionReason;
    private String smearQuality;
    private String peripheralBloodMorphology;
    private String boneMarrowMorphology;
    private String specimenViability;
    private String antibodyPanel;
    private String gatingFindings;
    private String conclusion;
    private String comment;

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
