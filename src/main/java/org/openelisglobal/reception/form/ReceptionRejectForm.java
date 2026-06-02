package org.openelisglobal.reception.form;

import jakarta.validation.constraints.NotBlank;

public class ReceptionRejectForm {

    @NotBlank(message = "Accession number is required")
    private String accessionNumber;

    @NotBlank(message = "Rejection reason is required")
    private String rejectReasonId;

    private String notes;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getRejectReasonId() {
        return rejectReasonId;
    }

    public void setRejectReasonId(String rejectReasonId) {
        this.rejectReasonId = rejectReasonId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
