package org.openelisglobal.program.valueholder.cytology;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

@Entity
@Table(name = "cytology_sample")
public class CytologySample extends ProgramSample {

    public enum CytologyStatus {
        RECEIVED("Received"), CELL_BLOCK("Cell block"), STAINING("Staining"),
        /** Legacy statuses kept so pre-workflow rows still load; mapped onto the rail. */
        PREPARING_SLIDES("Preparing slides"), SCREENING("Screening"),
        READY_FOR_CYTOPATHOLOGIST("Ready for Cytopathologist"), REJECTED("Rejected"), COMPLETED("Completed");

        private String display;

        CytologyStatus(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    /**
     * Which cytopathology test the case is. All four share one case-page template; the subtype only
     * decides which Collection / Cell block / The read slots apply.
     */
    public enum CytologySubtype {
        FNAC("FNAC"), IMAGE_GUIDED_FNAC("Image-guided FNAC"), PAP_SMEAR("Pap smear"), FLUID("Fluid cytology");

        private String display;

        CytologySubtype(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    @Valid
    @OneToOne
    @JoinColumn(name = "technician_id", referencedColumnName = "id")
    private SystemUser technician;

    @Valid
    @OneToOne
    @JoinColumn(name = "cytopathologist_id", referencedColumnName = "id")
    private SystemUser cytoPathologist;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status")
    private CytologyStatus status = CytologyStatus.RECEIVED;

    @Enumerated(EnumType.STRING)
    @Column(name = "subtype")
    private CytologySubtype subtype = CytologySubtype.FNAC;

    // Collection (shared)
    @Column(name = "collection_site")
    private String collectionSite;

    @Column(name = "collection_notes")
    private String collectionNotes;

    @Column(name = "collection_confirmed_at")
    private Timestamp collectionConfirmedAt;

    // Collection (Image-guided FNAC)
    @Column(name = "radiology_reference")
    private String radiologyReference;

    @Column(name = "rose_adequate")
    private Boolean roseAdequate;

    // Collection (Pap smear)
    @Column(name = "last_menstrual_period")
    private String lastMenstrualPeriod;

    @Column(name = "previous_pap_result")
    private String previousPapResult;

    @Column(name = "fixation_method")
    private String fixationMethod;

    // Collection (Fluid cytology)
    @Column(name = "fluid_volume")
    private String fluidVolume;

    @Column(name = "fluid_clarity")
    private String fluidClarity;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    // Cell block (Fluid cytology only)
    @Column(name = "cell_block_centrifuged_at")
    private Timestamp cellBlockCentrifugedAt;

    @Column(name = "cell_block_prepared_at")
    private Timestamp cellBlockPreparedAt;

    @Column(name = "cell_block_slide_at")
    private Timestamp cellBlockSlideAt;

    // The read
    @Column(name = "microscopy_exam")
    private String microscopyExam;

    @Column(name = "conclusion", length = 4000)
    private String conclusion;

    @Column(name = "conclusion_text")
    private String conclusionText;

    @Column(name = "repeat_requested_at")
    private Timestamp repeatRequestedAt;

    @Column(name = "second_opinion_requested_at")
    private Timestamp secondOpinionRequestedAt;

    @Column(name = "ihc_ordered_at")
    private Timestamp ihcOrderedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_sample_id")
    private List<CytologySlide> slides;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "specimen_adequacy_id", referencedColumnName = "id")
    private CytologySpecimenAdequacy specimenAdequacy;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_diagnosis_id", referencedColumnName = "id")
    private CytologyDiagnosis diagnosis;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "cytology_sample_id")
    private List<CytologyReport> reports;

    public SystemUser getTechnician() {
        return technician;
    }

    public String getTechnician_Audit() {
        if (technician == null) {
            return null;
        } else {
            return technician.getDisplayName();
        }
    }

    public void setTechnician(SystemUser technician) {
        this.technician = technician;
    }

    public CytologyStatus getStatus() {
        return status;
    }

    public void setStatus(CytologyStatus status) {
        this.status = status;
    }

    public CytologySubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(CytologySubtype subtype) {
        this.subtype = subtype;
    }

    public String getCollectionSite() {
        return collectionSite;
    }

    public void setCollectionSite(String collectionSite) {
        this.collectionSite = collectionSite;
    }

    public String getCollectionNotes() {
        return collectionNotes;
    }

    public void setCollectionNotes(String collectionNotes) {
        this.collectionNotes = collectionNotes;
    }

    public Timestamp getCollectionConfirmedAt() {
        return collectionConfirmedAt;
    }

    public void setCollectionConfirmedAt(Timestamp collectionConfirmedAt) {
        this.collectionConfirmedAt = collectionConfirmedAt;
    }

    public String getRadiologyReference() {
        return radiologyReference;
    }

    public void setRadiologyReference(String radiologyReference) {
        this.radiologyReference = radiologyReference;
    }

    public Boolean getRoseAdequate() {
        return roseAdequate;
    }

    public void setRoseAdequate(Boolean roseAdequate) {
        this.roseAdequate = roseAdequate;
    }

    public String getLastMenstrualPeriod() {
        return lastMenstrualPeriod;
    }

    public void setLastMenstrualPeriod(String lastMenstrualPeriod) {
        this.lastMenstrualPeriod = lastMenstrualPeriod;
    }

    public String getPreviousPapResult() {
        return previousPapResult;
    }

    public void setPreviousPapResult(String previousPapResult) {
        this.previousPapResult = previousPapResult;
    }

    public String getFixationMethod() {
        return fixationMethod;
    }

    public void setFixationMethod(String fixationMethod) {
        this.fixationMethod = fixationMethod;
    }

    public String getFluidVolume() {
        return fluidVolume;
    }

    public void setFluidVolume(String fluidVolume) {
        this.fluidVolume = fluidVolume;
    }

    public String getFluidClarity() {
        return fluidClarity;
    }

    public void setFluidClarity(String fluidClarity) {
        this.fluidClarity = fluidClarity;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getCellBlockCentrifugedAt() {
        return cellBlockCentrifugedAt;
    }

    public void setCellBlockCentrifugedAt(Timestamp cellBlockCentrifugedAt) {
        this.cellBlockCentrifugedAt = cellBlockCentrifugedAt;
    }

    public Timestamp getCellBlockPreparedAt() {
        return cellBlockPreparedAt;
    }

    public void setCellBlockPreparedAt(Timestamp cellBlockPreparedAt) {
        this.cellBlockPreparedAt = cellBlockPreparedAt;
    }

    public Timestamp getCellBlockSlideAt() {
        return cellBlockSlideAt;
    }

    public void setCellBlockSlideAt(Timestamp cellBlockSlideAt) {
        this.cellBlockSlideAt = cellBlockSlideAt;
    }

    public String getMicroscopyExam() {
        return microscopyExam;
    }

    public void setMicroscopyExam(String microscopyExam) {
        this.microscopyExam = microscopyExam;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public Timestamp getRepeatRequestedAt() {
        return repeatRequestedAt;
    }

    public void setRepeatRequestedAt(Timestamp repeatRequestedAt) {
        this.repeatRequestedAt = repeatRequestedAt;
    }

    public Timestamp getSecondOpinionRequestedAt() {
        return secondOpinionRequestedAt;
    }

    public void setSecondOpinionRequestedAt(Timestamp secondOpinionRequestedAt) {
        this.secondOpinionRequestedAt = secondOpinionRequestedAt;
    }

    public Timestamp getIhcOrderedAt() {
        return ihcOrderedAt;
    }

    public void setIhcOrderedAt(Timestamp ihcOrderedAt) {
        this.ihcOrderedAt = ihcOrderedAt;
    }

    public List<CytologySlide> getSlides() {
        return slides;
    }

    public String getSlides_Audit() {
        if (slides == null) {
            return null;
        } else {
            return StringUtils
                    .join(slides.stream().map(e -> "File Type: " + e.getFileType() + ", Location: " + e.getLocation())
                            .collect(Collectors.toList()), "; ");
        }
    }

    public void setSlides(List<CytologySlide> slides) {
        this.slides = slides;
    }

    public SystemUser getCytoPathologist() {
        return cytoPathologist;
    }

    public String getCytoPathologist_Audit() {
        if (cytoPathologist == null) {
            return null;
        } else {
            return cytoPathologist.getDisplayName();
        }
    }

    public void setCytoPathologist(SystemUser cytoPathologist) {
        this.cytoPathologist = cytoPathologist;
    }

    public CytologySpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public String getSpecimenAdequacy_Audit() {
        if (specimenAdequacy == null) {
            return null;
        } else {
            return "Result Type: " + specimenAdequacy.getResultType() + ", Satisfaction: "
                    + specimenAdequacy.getSatisfaction() == null ? "" : specimenAdequacy.getSatisfaction().getDisplay();
        }
    }

    public void setSpecimenAdequacy(CytologySpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public CytologyDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public String getDiagnosis_Audit() {
        if (diagnosis == null) {
            return null;
        } else {
            return "Negative Diagnosis: " + diagnosis.getNegativeDiagnosis();
        }
    }

    public void setDiagnosis(CytologyDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<CytologyReport> getReports() {
        return reports;
    }

    public String getReports_Audit() {
        if (reports == null) {
            return null;
        } else {
            return StringUtils.join(reports.stream()
                    .map(e -> "File Type: " + e.getFileType() + ", Report Type: "
                            + (e.getReportType() == null ? "" : e.getReportType().getDisplay()))
                    .collect(Collectors.toList()), "; ");
        }
    }

    public void setReports(List<CytologyReport> reports) {
        this.reports = reports;
    }
}
