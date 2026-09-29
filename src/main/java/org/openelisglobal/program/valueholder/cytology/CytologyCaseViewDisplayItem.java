package org.openelisglobal.program.valueholder.cytology;

import java.util.List;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.DiagnosisCategory;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy.SpecimenAdequancySatisfaction;

public class CytologyCaseViewDisplayItem extends CytologyDisplayItem {

    private String age;

    private String sex;

    private String referringFacility;

    private String department;

    private String requester;

    private Questionnaire programQuestionnaire;

    private QuestionnaireResponse programQuestionnaireResponse;

    private String assignedTechnicianId;

    private String assignedPathologistId;

    private List<CytologySlide> slides;

    private SpecimenAdequacy specimenAdequacy;

    private Diagnosis diagnosis;

    private List<CytologyReport> reports;

    private CytologySample.CytologySubtype subtype;

    private java.util.Date collectionDate;

    private String collectionSite;

    private String collectionNotes;

    private java.sql.Timestamp collectionConfirmedAt;

    private String radiologyReference;

    private Boolean roseAdequate;

    private String lastMenstrualPeriod;

    private String previousPapResult;

    private String fixationMethod;

    private String fluidVolume;

    private String fluidClarity;

    private String rejectionReason;

    private java.sql.Timestamp cellBlockCentrifugedAt;

    private java.sql.Timestamp cellBlockPreparedAt;

    private java.sql.Timestamp cellBlockSlideAt;

    private String microscopyExam;

    private String conclusion;

    private String conclusionText;

    private java.sql.Timestamp repeatRequestedAt;

    private java.sql.Timestamp secondOpinionRequestedAt;

    private java.sql.Timestamp ihcOrderedAt;

    public String getAge() {
        return age;
    }

    public List<CytologyReport> getReports() {
        return reports;
    }

    public void setReports(List<CytologyReport> reports) {
        this.reports = reports;
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

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
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

    public String getAssignedPathologistId() {
        return assignedPathologistId;
    }

    public void setAssignedPathologistId(String assignedPathologistId) {
        this.assignedPathologistId = assignedPathologistId;
    }

    public List<CytologySlide> getSlides() {
        return slides;
    }

    public void setSlides(List<CytologySlide> slides) {
        this.slides = slides;
    }

    public SpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public void setSpecimenAdequacy(SpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public CytologySample.CytologySubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(CytologySample.CytologySubtype subtype) {
        this.subtype = subtype;
    }

    public java.util.Date getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(java.util.Date collectionDate) {
        this.collectionDate = collectionDate;
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

    public java.sql.Timestamp getCollectionConfirmedAt() {
        return collectionConfirmedAt;
    }

    public void setCollectionConfirmedAt(java.sql.Timestamp collectionConfirmedAt) {
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

    public java.sql.Timestamp getCellBlockCentrifugedAt() {
        return cellBlockCentrifugedAt;
    }

    public void setCellBlockCentrifugedAt(java.sql.Timestamp cellBlockCentrifugedAt) {
        this.cellBlockCentrifugedAt = cellBlockCentrifugedAt;
    }

    public java.sql.Timestamp getCellBlockPreparedAt() {
        return cellBlockPreparedAt;
    }

    public void setCellBlockPreparedAt(java.sql.Timestamp cellBlockPreparedAt) {
        this.cellBlockPreparedAt = cellBlockPreparedAt;
    }

    public java.sql.Timestamp getCellBlockSlideAt() {
        return cellBlockSlideAt;
    }

    public void setCellBlockSlideAt(java.sql.Timestamp cellBlockSlideAt) {
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

    public java.sql.Timestamp getRepeatRequestedAt() {
        return repeatRequestedAt;
    }

    public void setRepeatRequestedAt(java.sql.Timestamp repeatRequestedAt) {
        this.repeatRequestedAt = repeatRequestedAt;
    }

    public java.sql.Timestamp getSecondOpinionRequestedAt() {
        return secondOpinionRequestedAt;
    }

    public void setSecondOpinionRequestedAt(java.sql.Timestamp secondOpinionRequestedAt) {
        this.secondOpinionRequestedAt = secondOpinionRequestedAt;
    }

    public java.sql.Timestamp getIhcOrderedAt() {
        return ihcOrderedAt;
    }

    public void setIhcOrderedAt(java.sql.Timestamp ihcOrderedAt) {
        this.ihcOrderedAt = ihcOrderedAt;
    }

    public static class SpecimenAdequacy {

        private List<IdValuePair> values;

        private SpecimenAdequancySatisfaction satisfaction;

        public List<IdValuePair> getValues() {
            return values;
        }

        public void setValues(List<IdValuePair> values) {
            this.values = values;
        }

        public SpecimenAdequancySatisfaction getSatisfaction() {
            return satisfaction;
        }

        public void setSatisfaction(SpecimenAdequancySatisfaction satisfaction) {
            this.satisfaction = satisfaction;
        }
    }

    public static class Diagnosis {

        private Boolean negativeDiagnosis = true;

        private List<DiagnosisResultsMap> diagnosisResultsMaps;

        public Boolean getNegativeDiagnosis() {
            return negativeDiagnosis;
        }

        public void setNegativeDiagnosis(Boolean negativeDiagnosis) {
            this.negativeDiagnosis = negativeDiagnosis;
        }

        public List<DiagnosisResultsMap> getDiagnosisResultsMaps() {
            return diagnosisResultsMaps;
        }

        public void setDiagnosisResultsMaps(List<DiagnosisResultsMap> diagnosisResultsMaps) {
            this.diagnosisResultsMaps = diagnosisResultsMaps;
        }

        public static class DiagnosisResultsMap {

            private List<IdValuePair> results;

            private DiagnosisCategory category;

            private CytologyDiagnosisResultType resultType;

            public CytologyDiagnosisResultType getResultType() {
                return resultType;
            }

            public void setResultType(CytologyDiagnosisResultType resultType) {
                this.resultType = resultType;
            }

            public DiagnosisCategory getCategory() {
                return category;
            }

            public void setCategory(DiagnosisCategory category) {
                this.category = category;
            }

            public List<IdValuePair> getResults() {
                return results;
            }

            public void setResults(List<IdValuePair> results) {
                this.results = results;
            }
        }
    }
}
