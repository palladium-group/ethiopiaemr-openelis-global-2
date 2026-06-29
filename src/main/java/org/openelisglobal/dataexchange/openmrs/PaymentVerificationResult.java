package org.openelisglobal.dataexchange.openmrs;

import java.time.Instant;

/**
 * Outcome of verifying OpenMRS payment status for a lab order and syncing the
 * remote {@code ServiceRequest} into the local FHIR store.
 */
public class PaymentVerificationResult {

    private final String orderUuid;
    private final OpenMrsPaymentStatus status;
    private final boolean collectionAllowed;
    private final boolean syncedToLocalFhir;
    private final Instant verifiedAt;

    public PaymentVerificationResult(String orderUuid, OpenMrsPaymentStatus status, boolean collectionAllowed,
            boolean syncedToLocalFhir, Instant verifiedAt) {
        this.orderUuid = orderUuid;
        this.status = status;
        this.collectionAllowed = collectionAllowed;
        this.syncedToLocalFhir = syncedToLocalFhir;
        this.verifiedAt = verifiedAt;
    }

    public static PaymentVerificationResult notApplicable() {
        return new PaymentVerificationResult(null, OpenMrsPaymentStatus.NOT_APPLICABLE, true, false, Instant.now());
    }

    public String getOrderUuid() {
        return orderUuid;
    }

    public OpenMrsPaymentStatus getStatus() {
        return status;
    }

    public boolean isCollectionAllowed() {
        return collectionAllowed;
    }

    public boolean isSyncedToLocalFhir() {
        return syncedToLocalFhir;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }
}
