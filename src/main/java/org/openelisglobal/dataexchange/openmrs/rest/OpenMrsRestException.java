package org.openelisglobal.dataexchange.openmrs.rest;

/**
 * OpenMRS REST call failed: unreachable (status 0) or answered with a non-2xx
 * status. The message is OpenMRS's own error message when it sent one.
 */
public class OpenMrsRestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public OpenMrsRestException(int status, String message) {
        super(message);
        this.status = status;
    }

    public OpenMrsRestException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public int getStatus() {
        return status;
    }
}
