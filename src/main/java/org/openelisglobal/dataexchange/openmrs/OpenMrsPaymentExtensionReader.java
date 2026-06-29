package org.openelisglobal.dataexchange.openmrs;

import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Type;
import org.springframework.stereotype.Component;

/**
 * Reads the OpenMRS payment-status extension from a FHIR
 * {@code ServiceRequest}.
 */
@Component
public class OpenMrsPaymentExtensionReader {

    public OpenMrsPaymentStatus readPaymentStatus(ServiceRequest serviceRequest, String extensionUrl) {
        if (serviceRequest == null || GenericValidator.isBlankOrNull(extensionUrl)) {
            return OpenMrsPaymentStatus.UNKNOWN;
        }
        for (Extension extension : serviceRequest.getExtension()) {
            if (extensionUrl.equals(extension.getUrl()) && extension.getValue() != null) {
                return OpenMrsPaymentStatus.fromCode(readCodeValue(extension.getValue()));
            }
        }
        return OpenMrsPaymentStatus.UNKNOWN;
    }

    private String readCodeValue(Type value) {
        if (value instanceof CodeType) {
            return ((CodeType) value).getValue();
        }
        return value.primitiveValue();
    }
}
