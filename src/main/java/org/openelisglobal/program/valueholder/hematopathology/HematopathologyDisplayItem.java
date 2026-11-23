package org.openelisglobal.program.valueholder.hematopathology;

/**
 * One hematopathology case in a list (reception dashboard row source).
 */
public class HematopathologyDisplayItem {

    private Integer caseId;
    private HematopathologySample.HematopathologyStatus status;
    private HematopathologySample.HematopathologySubtype subtype;
    private String requestDate;
    private String firstName;
    private String lastName;
    private String labNumber;
    private String patientPK;
    private String requester;
    private String assignedTechnician;
    private String assignedHematopathologist;

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public HematopathologySample.HematopathologyStatus getStatus() {
        return status;
    }

    public void setStatus(HematopathologySample.HematopathologyStatus status) {
        this.status = status;
    }

    public HematopathologySample.HematopathologySubtype getSubtype() {
        return subtype;
    }

    public void setSubtype(HematopathologySample.HematopathologySubtype subtype) {
        this.subtype = subtype;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        this.requestDate = requestDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(String assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public String getAssignedHematopathologist() {
        return assignedHematopathologist;
    }

    public void setAssignedHematopathologist(String assignedHematopathologist) {
        this.assignedHematopathologist = assignedHematopathologist;
    }
}
