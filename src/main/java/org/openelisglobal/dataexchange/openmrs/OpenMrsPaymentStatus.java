package org.openelisglobal.dataexchange.openmrs;

import org.apache.commons.lang3.StringUtils;

/**
 * Payment status carried on the OpenMRS FHIR {@code ServiceRequest} payment
 * extension and aligned with KenyaEMR Cashier {@code BillStatus} codes.
 */
public enum OpenMrsPaymentStatus {

    PENDING, POSTED, PAID, EXEMPTED, UNKNOWN,
    /** Gate disabled or order is not subject to OpenMRS payment verification. */
    NOT_APPLICABLE;

    public boolean allowsSampleCollection() {
        return this == PAID || this == EXEMPTED;
    }

    public boolean blocksSampleCollection() {
        return this != NOT_APPLICABLE && !allowsSampleCollection();
    }

    public static OpenMrsPaymentStatus fromCode(String code) {
        if (StringUtils.isBlank(code)) {
            return UNKNOWN;
        }
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
