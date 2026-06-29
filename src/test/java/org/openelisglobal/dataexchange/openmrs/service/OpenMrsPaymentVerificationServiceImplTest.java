package org.openelisglobal.dataexchange.openmrs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.fhir.FhirConfig;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.dataexchange.fhir.service.FhirPersistanceService;
import org.openelisglobal.dataexchange.openmrs.OpenMrsOrderUuidResolver;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConstants;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentExtensionReader;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentOrderScope;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderDisplayItem;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrderType;

@RunWith(MockitoJUnitRunner.class)
public class OpenMrsPaymentVerificationServiceImplTest {

    private static final String REMOTE_FHIR_PATH = "http://remote-fhir/fhir/";
    private static final String LOCAL_FHIR_PATH = "https://local-fhir/fhir/";
    private static final String ORDER_UUID = "lab-order-uuid";
    private static final String EXTENSION_URL = OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL;

    @Mock
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Mock
    private OpenMrsPaymentOrderScope paymentOrderScope;
    @Mock
    private OpenMrsOrderUuidResolver orderUuidResolver;
    @Mock
    private OpenMrsPaymentExtensionReader extensionReader;
    @Mock
    private FhirUtil fhirUtil;
    @Mock
    private FhirContext fhirContext;
    @Mock
    private FhirConfig fhirConfig;
    @Mock
    private FhirPersistanceService fhirPersistanceService;
    @Mock
    private IGenericClient remoteClient;
    @Mock
    private IRead read;
    @Mock
    private IReadTyped<ServiceRequest> readTyped;
    @Mock
    private IReadExecutable<ServiceRequest> readExecutable;

    @InjectMocks
    private OpenMrsPaymentVerificationServiceImpl verificationService;

    @Before
    public void setUp() {
        when(paymentConfiguration.isGateEnabled()).thenReturn(true);
        when(paymentConfiguration.getPaymentStatusExtensionUrl()).thenReturn(EXTENSION_URL);
        when(paymentConfiguration.getCacheSeconds()).thenReturn(60);
        when(fhirConfig.getRemoteStorePaths()).thenReturn(new String[] { REMOTE_FHIR_PATH });
        when(fhirConfig.getLocalFhirStorePath()).thenReturn(LOCAL_FHIR_PATH);
        when(fhirConfig.getUsername()).thenReturn("openelis");
        when(fhirConfig.getPassword()).thenReturn("secret");
        when(fhirContext.newRestfulGenericClient(REMOTE_FHIR_PATH)).thenReturn(remoteClient);
        when(fhirUtil.getFhirClient(LOCAL_FHIR_PATH)).thenReturn(remoteClient);
        when(remoteClient.read()).thenReturn(read);
        when(read.resource(ServiceRequest.class)).thenReturn(readTyped);
        when(readTyped.withId(ORDER_UUID)).thenReturn(readExecutable);
    }

    @Test
    public void verifyAndSync_registersSingleAuthInterceptorOnRemoteClient() throws Exception {
        ServiceRequest paidRequest = serviceRequestWithStatus("PAID");
        when(readExecutable.execute()).thenReturn(paidRequest);
        when(extensionReader.readPaymentStatus(paidRequest, EXTENSION_URL)).thenReturn(OpenMrsPaymentStatus.PAID);

        verificationService.verifyAndSync(ORDER_UUID, false);

        verify(fhirContext).newRestfulGenericClient(REMOTE_FHIR_PATH);
        verify(fhirUtil, never()).getFhirClient(eq(REMOTE_FHIR_PATH));
        verify(remoteClient, times(1)).registerInterceptor(any(BasicAuthInterceptor.class));
    }

    @Test
    public void verifyAndSync_bypassCacheRefetchesAfterPaidResultCached() throws Exception {
        ServiceRequest pendingRequest = serviceRequestWithStatus("PENDING");
        ServiceRequest paidRequest = serviceRequestWithStatus("PAID");

        when(readExecutable.execute()).thenReturn(pendingRequest, paidRequest);
        when(extensionReader.readPaymentStatus(pendingRequest, EXTENSION_URL)).thenReturn(OpenMrsPaymentStatus.PENDING);
        when(extensionReader.readPaymentStatus(paidRequest, EXTENSION_URL)).thenReturn(OpenMrsPaymentStatus.PAID);

        assertFalse(verificationService.verifyAndSync(ORDER_UUID, false).isCollectionAllowed());
        assertTrue(verificationService.verifyAndSync(ORDER_UUID, true).isCollectionAllowed());
        verify(readExecutable, times(2)).execute();
    }

    @Test
    public void verifyAndSync_doesNotCacheUnknownResults() throws Exception {
        when(readExecutable.execute()).thenThrow(new RuntimeException("remote down"));

        verificationService.verifyAndSync(ORDER_UUID, false);
        verificationService.verifyAndSync(ORDER_UUID, false);

        verify(readExecutable, times(2)).execute();
    }

    @Test
    public void applyPaymentStatusToDisplayItem_usesLocalVerificationForFhirOrders() throws Exception {
        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder.setType(ElectronicOrderType.FHIR);
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        when(paymentOrderScope.isSubjectToPaymentGate(electronicOrder)).thenReturn(true);
        when(orderUuidResolver.resolve(electronicOrder)).thenReturn(ORDER_UUID);

        ServiceRequest paidRequest = serviceRequestWithStatus("PAID");
        when(readExecutable.execute()).thenReturn(paidRequest);
        when(extensionReader.readPaymentStatus(paidRequest, EXTENSION_URL)).thenReturn(OpenMrsPaymentStatus.PAID);

        verificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);

        assertEquals(OpenMrsPaymentStatus.PAID.name(), displayItem.getOpenMrsPaymentStatus());
        assertTrue(displayItem.isCollectionAllowed());
        assertEquals(ORDER_UUID, displayItem.getOpenMrsOrderUuid());
        verify(fhirConfig, never()).getRemoteStorePaths();
    }

    @Test
    public void applyPaymentStatusToDisplayItem_reusesProvidedLocalServiceRequest() {
        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder.setType(ElectronicOrderType.FHIR);
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        when(paymentOrderScope.isSubjectToPaymentGate(electronicOrder)).thenReturn(true);
        when(orderUuidResolver.resolve(electronicOrder)).thenReturn(ORDER_UUID);

        ServiceRequest paidRequest = serviceRequestWithStatus("PAID");
        when(extensionReader.readPaymentStatus(paidRequest, EXTENSION_URL)).thenReturn(OpenMrsPaymentStatus.PAID);

        verificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder, paidRequest);

        assertEquals(OpenMrsPaymentStatus.PAID.name(), displayItem.getOpenMrsPaymentStatus());
        assertTrue(displayItem.isCollectionAllowed());
        verify(fhirUtil, never()).getFhirClient(any());
    }

    @Test
    public void applyPaymentStatusToDisplayItem_skipsNonFhirOrders() {
        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder.setType(ElectronicOrderType.HL7_V2);
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        when(paymentOrderScope.isSubjectToPaymentGate(electronicOrder)).thenReturn(false);

        verificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);

        assertEquals(OpenMrsPaymentStatus.NOT_APPLICABLE.name(), displayItem.getOpenMrsPaymentStatus());
        assertTrue(displayItem.isCollectionAllowed());
        verify(fhirUtil, never()).getFhirClient(any());
    }

    @Test
    public void applyPaymentStatusToDisplayItem_failClosedWhenOrderUuidMissing() {
        ElectronicOrder electronicOrder = new ElectronicOrder();
        electronicOrder.setType(ElectronicOrderType.FHIR);
        ElectronicOrderDisplayItem displayItem = new ElectronicOrderDisplayItem();

        when(paymentOrderScope.isSubjectToPaymentGate(electronicOrder)).thenReturn(true);
        when(orderUuidResolver.resolve(electronicOrder)).thenReturn(null);

        verificationService.applyPaymentStatusToDisplayItem(displayItem, electronicOrder);

        assertEquals(OpenMrsPaymentStatus.UNKNOWN.name(), displayItem.getOpenMrsPaymentStatus());
        assertFalse(displayItem.isCollectionAllowed());
    }

    private static ServiceRequest serviceRequestWithStatus(String statusCode) {
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(ORDER_UUID);
        serviceRequest.addExtension().setUrl(EXTENSION_URL).setValue(new CodeType(statusCode));
        return serviceRequest;
    }
}
