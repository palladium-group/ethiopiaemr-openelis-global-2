package org.openelisglobal.program.service.supply;

/**
 * A supply order could not be placed. {@link Code} is returned to the UI, which
 * shows a translated message for it.
 */
public class ProgramSupplyOrderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Code {
        /** pathologySupplyOrderEnabled is off. */
        NOT_ENABLED,
        /** OpenMRS REST connection or orderer provider not configured. */
        NOT_CONFIGURED,
        /** Case was entered in OpenELIS; there is no OpenMRS order/patient. */
        NO_OPENMRS_ORDER,
        /** OpenMRS has no order with the case's referring id. */
        ORDER_NOT_FOUND,
        /** Patient has no active visit in OpenMRS. */
        NO_ACTIVE_VISIT,
        /** Neither the visit nor the configuration gives a location. */
        NO_LOCATION, INVALID_ITEMS, CASE_CLOSED, NOT_FOUND,
        /** OpenMRS could not be reached or rejected a lookup. */
        OPENMRS_UNAVAILABLE
    }

    private final Code code;

    public ProgramSupplyOrderException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
