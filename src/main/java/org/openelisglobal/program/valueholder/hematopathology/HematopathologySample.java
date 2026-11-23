package org.openelisglobal.program.valueholder.hematopathology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * A hematopathology case (Peripheral morphology, Bone marrow aspirate
 * morphology or Flow cytometry). All three share one case-page template; the
 * subtype decides which workflow steps and result fields apply.
 */
@Entity
@Table(name = "hematopathology_sample")
public class HematopathologySample extends ProgramSample {

    public enum HematopathologyStatus {
        RECEIVED("Received"),
        /**
         * Collected, but the OpenMRS order is not yet paid or exempted; held before the
         * first lab step.
         */
        AWAITING_PAYMENT("Awaiting payment"),
        /** Morphology tests: smear preparation and staining. */
        SMEAR_PREP("Smear prep & staining"),
        /** Flow cytometry: antibody panel staining and RBC lysis. */
        PROCESSING("Processing"),
        /** Flow cytometry: run on the cytometer, gating and analysis. */
        ACQUISITION("Acquisition & analysis"), READY_FOR_HEMATOPATHOLOGIST("Ready for Hematopathologist"),
        REJECTED("Rejected"), COMPLETED("Completed");

        private final String display;

        HematopathologyStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    public enum HematopathologySubtype {
        PERIPHERAL_MORPHOLOGY("Peripheral morphology"), BONE_MARROW_ASPIRATE("Bone marrow aspirate"),
        FLOW_CYTOMETRY("Flow cytometry");

        private final String display;

        HematopathologySubtype(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public boolean isMorphology() {
            return this != FLOW_CYTOMETRY;
        }
    }

    @Valid
    @OneToOne
    @JoinColumn(name = "technician_id", referencedColumnName = "id")
    private SystemUser technician;

    @Valid
    @OneToOne
    @JoinColumn(name = "hematopathologist_id", referencedColumnName = "id")
    private SystemUser hematopathologist;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status")
    private HematopathologyStatus status = HematopathologyStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtype")
    private HematopathologySubtype subtype = HematopathologySubtype.PERIPHERAL_MORPHOLOGY;

    // Collection
    @Column(name = "collection_notes", length = 4000)
    private String collectionNotes;

    /** Bone marrow: spicules seen in the aspirate. */
    @Column(name = "spicules_present")
    private Boolean spiculesPresent;

    /** Flow cytometry: hours since collection when received. */
    @Column(name = "sample_age_hours")
    private String sampleAgeHours;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "collection_confirmed_at")
    private Timestamp collectionConfirmedAt;

    // Morphology: smear prep & staining
    @Column(name = "smear_prepared_at")
    private Timestamp smearPreparedAt;

    @Column(name = "smear_stained_at")
    private Timestamp smearStainedAt;

    // Flow cytometry: processing / acquisition
    @Column(name = "processing_completed_at")
    private Timestamp processingCompletedAt;

    @Column(name = "acquisition_completed_at")
    private Timestamp acquisitionCompletedAt;

    // Review & report — morphology
    @Column(name = "smear_quality", length = 4000)
    private String smearQuality;

    @Column(name = "peripheral_blood_morphology", length = 4000)
    private String peripheralBloodMorphology;

    @Column(name = "bone_marrow_morphology", length = 4000)
    private String boneMarrowMorphology;

    // Review & report — flow cytometry
    @Column(name = "specimen_viability", length = 4000)
    private String specimenViability;

    @Column(name = "antibody_panel", length = 4000)
    private String antibodyPanel;

    @Column(name = "gating_findings", length = 4000)
    private String gatingFindings;

    // Review & report — shared
    @Column(name = "conclusion", length = 4000)
    private String conclusion;

    @Column(name = "report_comment", length = 4000)
    private String comment;

    public SystemUser getTechnician() {
        return technician;
    }

    public String getTechnician_Audit() {
        return technician == null ? null : technician.getDisplayName();
    }

    public void setTechnician(SystemUser technician) {
        this.technician = technician;
    }

    public SystemUser getHematopathologist() {
        return hematopathologist;
    }

    public String getHematopathologist_Audit() {
        return hematopathologist == null ? null : hematopathologist.getDisplayName();
    }

    public void setHematopathologist(SystemUser hematopathologist) {
        this.hematopathologist = hematopathologist;
    }

    public HematopathologyStatus getStatus() {
        return status;
    }

    public void setStatus(HematopathologyStatus status) {
        this.status = status;
    }

    public HematopathologySubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(HematopathologySubtype subtype) {
        this.subtype = subtype;
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
