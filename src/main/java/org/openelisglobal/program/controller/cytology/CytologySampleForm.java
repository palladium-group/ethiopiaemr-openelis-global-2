package org.openelisglobal.program.controller.cytology;

import java.util.Base64;
import java.util.List;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis;
import org.openelisglobal.program.valueholder.cytology.CytologyReport;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;
import org.openelisglobal.program.valueholder.cytology.CytologySlide;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy;

public class CytologySampleForm {

    private CytologyStatus status;

    private List<CytologySlideForm> slides;

    private String assignedCytoPathologistId;

    private String assignedTechnicianId;

    private CytologySpecimenAdequacy specimenAdequacy;

    private String systemUserId;

    private Boolean release = false;

    private CytologyDiagnosis diagnosis;

    private List<CytologyReportForm> reports;

    // Collection / The read (workflow rail)
    private String collectionSite;
    private String collectionNotes;
    private String radiologyReference;
    private Boolean roseAdequate;
    private String lastMenstrualPeriod;
    private String previousPapResult;
    private String fixationMethod;
    private String fluidVolume;
    private String fluidClarity;
    private String rejectionReason;
    private String microscopyExam;
    private String conclusion;
    private String conclusionText;
    /** Fluid cell-block checklist: centrifuge | prepare | slide */
    private String cellBlockStep;

    public CytologyStatus getStatus() {
        return status;
    }

    public void setStatus(CytologyStatus status) {
        this.status = status;
    }

    public List<CytologySlideForm> getSlides() {
        return slides;
    }

    public void setSlides(List<CytologySlideForm> slides) {
        this.slides = slides;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(String systemUserId) {
        this.systemUserId = systemUserId;
    }

    public Boolean getRelease() {
        return release;
    }

    public void setRelease(Boolean release) {
        this.release = release;
    }

    public String getAssignedCytoPathologistId() {
        return assignedCytoPathologistId;
    }

    public void setAssignedCytoPathologistId(String assignedCytoPathologistId) {
        this.assignedCytoPathologistId = assignedCytoPathologistId;
    }

    public CytologySpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public void setSpecimenAdequacy(CytologySpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public CytologyDiagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(CytologyDiagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<CytologyReportForm> getReports() {
        return reports;
    }

    public void setReports(List<CytologyReportForm> reports) {
        this.reports = reports;
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

    public String getCellBlockStep() {
        return cellBlockStep;
    }

    public void setCellBlockStep(String cellBlockStep) {
        this.cellBlockStep = cellBlockStep;
    }

    public static class CytologySlideForm extends CytologySlide {

        private static final long serialVersionUID = 3142138533368581327L;

        private String base64Image;

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
            String[] imageInfo = base64Image.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setImage(Base64.getDecoder().decode(imageInfo[1]));
        }
    }

    public static class CytologyReportForm extends CytologyReport {

        private static final long serialVersionUID = 3142138533368581327L;

        private String base64Image;

        public String getBase64Image() {
            return base64Image;
        }

        public void setBase64Image(String base64Image) {
            this.base64Image = base64Image;
            String[] imageInfo = base64Image.split(";base64,", 2);

            setFileType(imageInfo[0]);
            setImage(Base64.getDecoder().decode(imageInfo[1]));
        }
    }
}
