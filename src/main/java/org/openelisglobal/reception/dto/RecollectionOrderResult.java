package org.openelisglobal.reception.dto;

public class RecollectionOrderResult {

    private final String electronicOrderId;
    private final String externalOrderId;

    public RecollectionOrderResult(String electronicOrderId, String externalOrderId) {
        this.electronicOrderId = electronicOrderId;
        this.externalOrderId = externalOrderId;
    }

    public String getElectronicOrderId() {
        return electronicOrderId;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }
}
