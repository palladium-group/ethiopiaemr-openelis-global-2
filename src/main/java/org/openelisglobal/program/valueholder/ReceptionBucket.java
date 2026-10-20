package org.openelisglobal.program.valueholder;

/**
 * Stage buckets shared by every service category on the reception dashboard.
 * Histopathology and cytopathology have their own status enums with almost no
 * overlapping names, so the dashboard filters on these buckets while each row
 * still displays its own program's stage.
 */
public enum ReceptionBucket {

    /** No specialist assigned yet, whatever the stage. */
    UNASSIGNED,
    /**
     * Order received / awaiting collection or first lab step ({@code RECEIVED} for
     * both histopathology and cytopathology).
     */
    RECEIVED,
    /**
     * Collected but held until the OpenMRS order is paid or exempted
     * ({@code AWAITING_PAYMENT} for both histopathology and cytopathology).
     */
    AWAITING_PAYMENT, IN_PROGRESS, AWAITING_REVIEW,
    /** Histopathology only; cytopathology has no equivalent stage. */
    ADDITIONAL_REQUEST, COMPLETED, ALL;
}
