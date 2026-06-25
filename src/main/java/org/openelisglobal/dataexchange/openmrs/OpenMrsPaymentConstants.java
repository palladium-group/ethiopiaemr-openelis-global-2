package org.openelisglobal.dataexchange.openmrs;

/**
 * Shared constants for OpenMRS payment verification against FHIR
 * {@code ServiceRequest} extensions.
 */
public final class OpenMrsPaymentConstants {

    public static final String DEFAULT_PAYMENT_STATUS_EXTENSION_URL = "https://palladiumethiopia.com/fhir/ext/payment-status";

    public static final String APPLICATION_PROPERTY_CACHE_SECONDS = "org.openelisglobal.openmrs.payment.cache.seconds";

    public static final int DEFAULT_CACHE_SECONDS = 60;

    public static final int MAX_CACHE_ENTRIES = 500;

    private OpenMrsPaymentConstants() {
    }
}
