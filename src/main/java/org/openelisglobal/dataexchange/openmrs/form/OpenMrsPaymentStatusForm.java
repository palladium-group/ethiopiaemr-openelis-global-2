package org.openelisglobal.dataexchange.openmrs.form;

import java.time.Instant;

public class OpenMrsPaymentStatusForm {

    private String orderUuid;
    private String status;
    private boolean collectionAllowed;
    private boolean syncedToLocalFhir;
    private String verifiedAt;

    public static OpenMrsPaymentStatusForm from(String orderUuid, String status, boolean collectionAllowed,
            boolean syncedToLocalFhir, Instant verifiedAt) {
        OpenMrsPaymentStatusForm form = new OpenMrsPaymentStatusForm();
        form.setOrderUuid(orderUuid);
        form.setStatus(status);
        form.setCollectionAllowed(collectionAllowed);
        form.setSyncedToLocalFhir(syncedToLocalFhir);
        form.setVerifiedAt(verifiedAt == null ? null : verifiedAt.toString());
        return form;
    }

    public String getOrderUuid() {
        return orderUuid;
    }

    public void setOrderUuid(String orderUuid) {
        this.orderUuid = orderUuid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCollectionAllowed() {
        return collectionAllowed;
    }

    public void setCollectionAllowed(boolean collectionAllowed) {
        this.collectionAllowed = collectionAllowed;
    }

    public boolean isSyncedToLocalFhir() {
        return syncedToLocalFhir;
    }

    public void setSyncedToLocalFhir(boolean syncedToLocalFhir) {
        this.syncedToLocalFhir = syncedToLocalFhir;
    }

    public String getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(String verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
