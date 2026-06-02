package org.openelisglobal.reception.form;

import jakarta.validation.constraints.NotBlank;

public class ReceptionApproveForm {

    @NotBlank(message = "Accession number is required")
    private String accessionNumber;

    private String notes;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
