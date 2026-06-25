package org.openelisglobal.dataexchange.openmrs.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentOrderScope;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.springframework.context.MessageSource;

@RunWith(MockitoJUnitRunner.Silent.class)
public class OpenMrsPaymentGateServiceTest {

    @Mock
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Mock
    private OpenMrsPaymentOrderScope paymentOrderScope;
    @Mock
    private OpenMrsPaymentVerificationService paymentVerificationService;

    @InjectMocks
    private OpenMrsPaymentGateService gateService;

    @Before
    public void setUp() {
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(eq("openmrs.payment.required"), any(), any(Locale.class)))
                .thenReturn("Payment required");
        MessageUtil.setMessageSource(messageSource);
        when(paymentConfiguration.isGateEnabled()).thenReturn(true);
        when(paymentOrderScope.isSubjectToPaymentGate(any())).thenReturn(true);
    }

    @Test
    public void assertSampleCollectionAllowed_skipsWhenGateDisabled() {
        when(paymentConfiguration.isGateEnabled()).thenReturn(false);
        SamplePatientUpdateData updateData = mockUpdateDataWithOrder(new ElectronicOrder());

        gateService.assertSampleCollectionAllowed(updateData);

        verify(paymentVerificationService, never()).verifyAndSyncForElectronicOrder(any());
    }

    @Test
    public void assertSampleCollectionAllowed_allowsPaidOrder() {
        ElectronicOrder order = new ElectronicOrder();
        order.setId("1");
        when(paymentVerificationService.verifyAndSyncForElectronicOrder(order, false))
                .thenReturn(new PaymentVerificationResult("uuid", OpenMrsPaymentStatus.PAID, true, true, null));

        gateService.assertSampleCollectionAllowed(mockUpdateDataWithOrder(order));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assertSampleCollectionAllowed_blocksPendingOrder() {
        ElectronicOrder order = new ElectronicOrder();
        order.setId("1");
        when(paymentVerificationService.verifyAndSyncForElectronicOrder(order, false))
                .thenReturn(new PaymentVerificationResult("uuid", OpenMrsPaymentStatus.PENDING, false, true, null));

        gateService.assertSampleCollectionAllowed(mockUpdateDataWithOrder(order));
    }

    @Test
    public void assertSampleCollectionAllowed_skipsHl7Orders() {
        ElectronicOrder order = new ElectronicOrder();
        order.setId("1");
        when(paymentOrderScope.isSubjectToPaymentGate(order)).thenReturn(false);
        SamplePatientUpdateData updateData = mockUpdateDataWithOrder(order);

        gateService.assertSampleCollectionAllowed(updateData);

        verify(paymentVerificationService, never()).verifyAndSyncForElectronicOrder(any(), eq(false));
    }

    private SamplePatientUpdateData mockUpdateDataWithOrder(ElectronicOrder order) {
        order.setId("1");
        SamplePatientUpdateData updateData = mock(SamplePatientUpdateData.class);
        List<ElectronicOrder> orders = new ArrayList<>();
        orders.add(order);
        when(updateData.getElectronicOrders()).thenReturn(orders);
        when(updateData.getElectronicOrder()).thenReturn(null);
        return updateData;
    }
}
