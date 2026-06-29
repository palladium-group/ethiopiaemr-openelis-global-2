package org.openelisglobal.dataexchange.openmrs;

import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.Task;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Resolves the OpenMRS order UUID used as the FHIR {@code ServiceRequest} id.
 */
@Component
public class OpenMrsOrderUuidResolver {

    @Autowired
    private FhirUtil fhirUtil;

    public String resolve(ElectronicOrder electronicOrder) {
        if (electronicOrder == null) {
            return null;
        }
        String fromTask = resolveFromTaskData(electronicOrder.getData());
        if (!GenericValidator.isBlankOrNull(fromTask)) {
            return fromTask;
        }
        return GenericValidator.isBlankOrNull(electronicOrder.getExternalId()) ? null
                : electronicOrder.getExternalId().trim();
    }

    public String resolveFromTaskData(String taskJson) {
        if (GenericValidator.isBlankOrNull(taskJson)) {
            return null;
        }
        try {
            Task task = fhirUtil.getFhirParser().parseResource(Task.class, taskJson);
            if (task != null && task.hasBasedOn() && task.getBasedOnFirstRep().hasReferenceElement()) {
                return task.getBasedOnFirstRep().getReferenceElement().getIdPart();
            }
        } catch (RuntimeException e) {
            return null;
        }
        return null;
    }
}
