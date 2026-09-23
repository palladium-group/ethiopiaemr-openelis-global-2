package org.openelisglobal.program.valueholder;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

/**
 * One row of the merged pathology reception dashboard.
 *
 * <p>Histopathology and cytopathology cases live in separate tables with independent id sequences,
 * so the same numeric id exists in both. {@link #getRowId()} (category code + case id) is the only
 * identifier unique across the merged list, and the category also decides which case view the row
 * opens and which assignment endpoint it posts to.
 */
public class ProgramCaseDisplayItem {

    private String rowId;

    private String serviceCategoryCode;

    private String serviceCategory;

    private Integer caseId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date requestDate;

    /** Program-specific status name, e.g. GROSSING or CELL_BLOCK. */
    private String statusCode;

    /** Program-specific stage shown in the Status column, e.g. "Grossing", "Cell block". */
    private String status;

    /** Clinical sample type, e.g. Biopsy, Morphology, FNAC, Pap smear. */
    private String subtype;

    private String firstName;

    private String lastName;

    private String requester;

    private String assignedTechnician;

    /** Pathologist for histopathology, cytopathologist for cytopathology. */
    private String assignedSpecialist;

    private String labNumber;

    private String patientPK;

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getServiceCategoryCode() {
        return serviceCategoryCode;
    }

    public void setServiceCategoryCode(String serviceCategoryCode) {
        this.serviceCategoryCode = serviceCategoryCode;
    }

    public String getServiceCategory() {
        return serviceCategory;
    }

    public void setServiceCategory(String serviceCategory) {
        this.serviceCategory = serviceCategory;
    }

    public Integer getCaseId() {
        return caseId;
    }

    public void setCaseId(Integer caseId) {
        this.caseId = caseId;
    }

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
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

    public String getAssignedSpecialist() {
        return assignedSpecialist;
    }

    public void setAssignedSpecialist(String assignedSpecialist) {
        this.assignedSpecialist = assignedSpecialist;
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
}
