package org.openelisglobal.dataexchange.openmrs.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.GenericValidator;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.exception.FhirLocalPersistingException;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.openmrs.OpenMrsOrderUuidResolver;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConstants;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentExtensionReader;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentOrderScope;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OpenMrsPaymentVerificationServiceImpl implements OpenMrsPaymentVerificationService {

    private static final class CacheEntry {
        private final PaymentVerificationResult result;
        private final long expiresAtMillis;

        private CacheEntry(PaymentVerificationResult result, long expiresAtMillis) {
            this.result = result;
            this.expiresAtMillis = expiresAtMillis;
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> verificationCache = new ConcurrentHashMap<>();

    @Autowired
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Autowired
    private OpenMrsPaymentOrderScope paymentOrderScope;
    @Autowired
    private OpenMrsOrderUuidResolver orderUuidResolver;
    @Autowired
    private OpenMrsPaymentExtensionReader extensionReader;
    @Autowired
    private FhirUtil fhirUtil;
    @Autowired
    private FhirContext fhirContext;
    @Autowired
    private FhirConfig fhirConfig;
    @Autowired
    private FhirPersistanceService fhirPersistanceService;

    @Override
    public PaymentVerificationResult verifyAndSync(String orderUuid) {
        return verifyAndSync(orderUuid, false);
    }

    @Override
    public PaymentVerificationResult verifyAndSync(String orderUuid, boolean bypassCache) {
        if (!paymentConfiguration.isGateEnabled()) {
            return PaymentVerificationResult.notApplicable();
        }
        if (GenericValidator.isBlankOrNull(orderUuid)) {
            return buildResult(null, OpenMrsPaymentStatus.UNKNOWN, false, false);
        }

        String normalizedUuid = orderUuid.trim();
        if (!bypassCache) {
            PaymentVerificationResult cached = getCachedResult(normalizedUuid);
            if (cached != null) {
                return cached;
            }
        } else {
            invalidateCache(normalizedUuid);
        }

        ServiceRequest remoteServiceRequest = readRemoteServiceRequest(normalizedUuid);
        if (remoteServiceRequest == null) {
            return buildResult(normalizedUuid, OpenMrsPaymentStatus.UNKNOWN, false, false);
        }

        OpenMrsPaymentStatus status = extensionReader.readPaymentStatus(remoteServiceRequest,
                paymentConfiguration.getPaymentStatusExtensionUrl());
        boolean synced = syncToLocalFhir(remoteServiceRequest);
        PaymentVerificationResult result = buildResult(normalizedUuid, status, status.allowsSampleCollection(), synced);
        if (shouldCacheResult(result)) {
            putCachedResult(normalizedUuid, result);
        }
        return result;
    }

    @Override
    public PaymentVerificationResult verifyAndSyncForElectronicOrder(ElectronicOrder electronicOrder) {
        return verifyAndSyncForElectronicOrder(electronicOrder, false);
    }

    @Override
    public PaymentVerificationResult verifyAndSyncForElectronicOrder(ElectronicOrder electronicOrder,
            boolean bypassCache) {
        if (!paymentConfiguration.isGateEnabled() || !paymentOrderScope.isSubjectToPaymentGate(electronicOrder)) {
            return PaymentVerificationResult.notApplicable();
        }
        String orderUuid = orderUuidResolver.resolve(electronicOrder);
        return verifyAndSync(orderUuid, bypassCache);
    }

    @Override
    public void applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem displayItem,
            ElectronicOrder electronicOrder) {
        applyPaymentStatusToDisplayItem(displayItem, electronicOrder, null);
    }

    @Override
    public void applyPaymentStatusToDisplayItem(ElectronicOrderDisplayItem displayItem, ElectronicOrder electronicOrder,
            ServiceRequest localServiceRequest) {
        if (!paymentConfiguration.isGateEnabled()) {
            displayItem.setOpenMrsPaymentStatus(OpenMrsPaymentStatus.NOT_APPLICABLE.name());
            displayItem.setCollectionAllowed(true);
            return;
        }
        if (!paymentOrderScope.isSubjectToPaymentGate(electronicOrder)) {
            displayItem.setOpenMrsPaymentStatus(OpenMrsPaymentStatus.NOT_APPLICABLE.name());
            displayItem.setCollectionAllowed(true);
            return;
        }

        String orderUuid = orderUuidResolver.resolve(electronicOrder);
        displayItem.setOpenMrsOrderUuid(orderUuid);
        if (GenericValidator.isBlankOrNull(orderUuid)) {
            displayItem.setOpenMrsPaymentStatus(OpenMrsPaymentStatus.UNKNOWN.name());
            displayItem.setCollectionAllowed(false);
            return;
        }

        PaymentVerificationResult result = localServiceRequest == null ? readLocalStatus(orderUuid.trim())
                : readLocalStatusFromServiceRequest(orderUuid.trim(), localServiceRequest);
        OpenMrsPaymentStatus status = result.getStatus() == null ? OpenMrsPaymentStatus.UNKNOWN : result.getStatus();
        displayItem.setOpenMrsPaymentStatus(status.name());
        displayItem.setCollectionAllowed(result.isCollectionAllowed());
    }

    @Override
    public PaymentVerificationResult readLocalStatus(String orderUuid) {
        if (!paymentConfiguration.isGateEnabled()) {
            return PaymentVerificationResult.notApplicable();
        }
        if (GenericValidator.isBlankOrNull(orderUuid)) {
            return buildResult(null, OpenMrsPaymentStatus.UNKNOWN, false, false);
        }

        String normalizedUuid = orderUuid.trim();
        ServiceRequest localServiceRequest = readLocalServiceRequest(normalizedUuid);
        if (localServiceRequest == null) {
            return buildResult(normalizedUuid, OpenMrsPaymentStatus.UNKNOWN, false, false);
        }

        return readLocalStatusFromServiceRequest(normalizedUuid, localServiceRequest);
    }

    private PaymentVerificationResult readLocalStatusFromServiceRequest(String orderUuid,
            ServiceRequest localServiceRequest) {
        OpenMrsPaymentStatus status = extensionReader.readPaymentStatus(localServiceRequest,
                paymentConfiguration.getPaymentStatusExtensionUrl());
        return buildResult(orderUuid, status, status.allowsSampleCollection(), false);
    }

    @Override
    public void invalidateCache(String orderUuid) {
        if (!GenericValidator.isBlankOrNull(orderUuid)) {
            verificationCache.remove(orderUuid.trim());
        }
    }

    private boolean shouldCacheResult(PaymentVerificationResult result) {
        return result.getStatus() != OpenMrsPaymentStatus.UNKNOWN;
    }

    private PaymentVerificationResult getCachedResult(String orderUuid) {
        CacheEntry entry = verificationCache.get(orderUuid);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAtMillis) {
            verificationCache.remove(orderUuid, entry);
            return null;
        }
        return entry.result;
    }

    private void putCachedResult(String orderUuid, PaymentVerificationResult result) {
        if (verificationCache.size() >= OpenMrsPaymentConstants.MAX_CACHE_ENTRIES) {
            evictOneCacheEntry();
        }
        long ttlMillis = paymentConfiguration.getCacheSeconds() * 1000L;
        verificationCache.put(orderUuid, new CacheEntry(result, System.currentTimeMillis() + ttlMillis));
    }

    private void evictOneCacheEntry() {
        String oldestKey = null;
        long oldestExpiry = Long.MAX_VALUE;
        for (Map.Entry<String, CacheEntry> entry : verificationCache.entrySet()) {
            if (entry.getValue().expiresAtMillis < oldestExpiry) {
                oldestExpiry = entry.getValue().expiresAtMillis;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            verificationCache.remove(oldestKey);
        }
    }

    private ServiceRequest readRemoteServiceRequest(String orderUuid) {
        String[] remotePaths = fhirConfig.getRemoteStorePaths();
        if (remotePaths == null || remotePaths.length == 0) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "readRemoteServiceRequest",
                    "No remote FHIR store configured for payment verification");
            return null;
        }

        for (String remotePath : remotePaths) {
            if (StringUtils.isBlank(remotePath)) {
                continue;
            }
            try {
                IGenericClient remoteClient = createRemoteFhirClient(remotePath);
                return remoteClient.read().resource(ServiceRequest.class).withId(orderUuid).execute();
            } catch (ResourceNotFoundException e) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "readRemoteServiceRequest",
                        "ServiceRequest not found on remote FHIR server: " + orderUuid);
            } catch (RuntimeException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "readRemoteServiceRequest",
                        "Failed to read ServiceRequest from remote FHIR: " + e.getMessage());
                LogEvent.logError(e);
            }
        }
        return null;
    }

    private ServiceRequest readLocalServiceRequest(String orderUuid) {
        if (StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            return null;
        }
        try {
            IGenericClient localClient = fhirUtil.getFhirClient(fhirConfig.getLocalFhirStorePath());
            return localClient.read().resource(ServiceRequest.class).withId(orderUuid).execute();
        } catch (ResourceNotFoundException e) {
            return null;
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "readLocalServiceRequest",
                    "Failed to read ServiceRequest from local FHIR: " + e.getMessage());
            LogEvent.logError(e);
            return null;
        }
    }

    private boolean syncToLocalFhir(ServiceRequest remoteServiceRequest) {
        if (remoteServiceRequest == null || StringUtils.isBlank(fhirConfig.getLocalFhirStorePath())) {
            return false;
        }
        try {
            fhirPersistanceService.updateFhirResourceInFhirStore(remoteServiceRequest);
            return true;
        } catch (FhirLocalPersistingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "syncToLocalFhir",
                    "Failed to sync ServiceRequest to local FHIR store: " + e.getMessage());
            LogEvent.logError(e);
            return false;
        }
    }

    private IGenericClient createRemoteFhirClient(String remotePath) {
        IGenericClient remoteClient = fhirContext.newRestfulGenericClient(remotePath);
        String localPath = fhirConfig.getLocalFhirStorePath();
        if (StringUtils.isNotBlank(fhirConfig.getUsername()) && !StringUtils.equals(remotePath, localPath)) {
            IClientInterceptor authInterceptor = new BasicAuthInterceptor(fhirConfig.getUsername(),
                    fhirConfig.getPassword());
            remoteClient.registerInterceptor(authInterceptor);
        }
        return remoteClient;
    }

    private PaymentVerificationResult buildResult(String orderUuid, OpenMrsPaymentStatus status,
            boolean collectionAllowed, boolean syncedToLocalFhir) {
        return new PaymentVerificationResult(orderUuid, status, collectionAllowed, syncedToLocalFhir, Instant.now());
    }
}
