package org.openelisglobal.dataexchange.openmrs.service;

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
     * Populates payment fields on an incoming-order display row using remote FHIR
     * verification.
     */
    void applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem displayItem, ElectronicOrder electronicOrder);

    void invalidateCache(String orderUuid);
}
