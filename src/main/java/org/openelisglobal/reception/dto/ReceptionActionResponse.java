package org.openelisglobal.reception.dto;

public class ReceptionActionResponse {

    private String accessionNumber;
    private int analysesUpdated;
    private String message;
    private boolean recollectionOrderCreated;
    private String newElectronicOrderExternalId;
    private String newElectronicOrderId;

    public ReceptionActionResponse() {
    }

    public ReceptionActionResponse(String accessionNumber, int analysesUpdated, String message) {
        this.accessionNumber = accessionNumber;
        this.analysesUpdated = analysesUpdated;
        this.message = message;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public int getAnalysesUpdated() {
        return analysesUpdated;
    }

    public void setAnalysesUpdated(int analysesUpdated) {
        this.analysesUpdated = analysesUpdated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRecollectionOrderCreated() {
        return recollectionOrderCreated;
    }

    public void setRecollectionOrderCreated(boolean recollectionOrderCreated) {
        this.recollectionOrderCreated = recollectionOrderCreated;
    }

    public String getNewElectronicOrderExternalId() {
        return newElectronicOrderExternalId;
    }

    public void setNewElectronicOrderExternalId(String newElectronicOrderExternalId) {
        this.newElectronicOrderExternalId = newElectronicOrderExternalId;
    }

    public String getNewElectronicOrderId() {
        return newElectronicOrderId;
    }

    public void setNewElectronicOrderId(String newElectronicOrderId) {
        this.newElectronicOrderId = newElectronicOrderId;
    }
}
