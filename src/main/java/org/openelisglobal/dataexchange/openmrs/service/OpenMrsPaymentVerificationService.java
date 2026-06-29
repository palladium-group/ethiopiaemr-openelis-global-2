package org.openelisglobal.dataexchange.openmrs.service;

import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;

public interface OpenMrsPaymentVerificationService {

    PaymentVerificationResult verifyAndSync(String orderUuid);

    PaymentVerificationResult verifyAndSync(String orderUuid, boolean bypassCache);

    PaymentVerificationResult verifyAndSyncForElectronicOrder(ElectronicOrder electronicOrder);

    PaymentVerificationResult verifyAndSyncForElectronicOrder(ElectronicOrder electronicOrder, boolean bypassCache);

    /**
     * Reads payment status from the local FHIR store without contacting the remote
     * server.
     */
    PaymentVerificationResult readLocalStatus(String orderUuid);

    /**
     * Populates payment fields on an incoming-order display row from the local FHIR
     * store. Does not contact the remote OpenMRS FHIR server.
     */
    void applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem displayItem, ElectronicOrder electronicOrder);

    /**
     * Same as
     * {@link #applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem, ElectronicOrder)}
     * but reuses an already-loaded local {@link ServiceRequest} to avoid an extra
     * FHIR read.
     */
    void applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem displayItem, ElectronicOrder electronicOrder,
            ServiceRequest localServiceRequest);

    void invalidateCache(String orderUuid);
}
