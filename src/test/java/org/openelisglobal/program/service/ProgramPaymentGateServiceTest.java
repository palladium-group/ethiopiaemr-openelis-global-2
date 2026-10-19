package org.openelisglobal.program.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentConfiguration;
import org.openelisglobal.dataexchange.openmrs.OpenMrsPaymentStatus;
import org.openelisglobal.dataexchange.openmrs.PaymentVerificationResult;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentVerificationService;
import org.openelisglobal.sample.valueholder.Sample;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProgramPaymentGateServiceTest {

    private static final String ORDER_UUID = "0773d273-e32c-4fc2-85e5-f82bdbedb598";

    @Mock
    private OpenMrsPaymentConfiguration paymentConfiguration;
    @Mock
    private OpenMrsPaymentVerificationService paymentVerificationService;

    @InjectMocks
    private ProgramPaymentGateService gateService;

    @Before
    public void setUp() {
        when(paymentConfiguration.isPathologyGateEnabled()).thenReturn(true);
    }

    @Test
    public void checkPayment_notApplicableWhenGateDisabled() {
        when(paymentConfiguration.isPathologyGateEnabled()).thenReturn(false);

        assertEquals(OpenMrsPaymentStatus.NOT_APPLICABLE, gateService.checkPayment(sampleWithOrder(ORDER_UUID)));
        verify(paymentVerificationService, never()).verifyOrderPayment(anyString(), anyBoolean());
    }

    @Test
    public void checkPayment_notApplicableForCaseEnteredInOpenElis() {
        assertEquals(OpenMrsPaymentStatus.NOT_APPLICABLE, gateService.checkPayment(sampleWithOrder(" ")));
        assertEquals(OpenMrsPaymentStatus.NOT_APPLICABLE, gateService.checkPayment(null));
        verify(paymentVerificationService, never()).verifyOrderPayment(anyString(), anyBoolean());
    }

    @Test
    public void checkPayment_readsFreshStatusForReferringOrder() {
        when(paymentVerificationService.verifyOrderPayment(ORDER_UUID, true))
                .thenReturn(new PaymentVerificationResult(ORDER_UUID, OpenMrsPaymentStatus.PENDING, false, true, null));

        assertEquals(OpenMrsPaymentStatus.PENDING, gateService.checkPayment(sampleWithOrder(ORDER_UUID)));
        verify(paymentVerificationService).verifyOrderPayment(ORDER_UUID, true);
    }

    @Test
    public void checkPayment_treatsMissingStatusAsUnknown() {
        when(paymentVerificationService.verifyOrderPayment(ORDER_UUID, true))
                .thenReturn(new PaymentVerificationResult(ORDER_UUID, null, false, false, null));

        assertEquals(OpenMrsPaymentStatus.UNKNOWN, gateService.checkPayment(sampleWithOrder(ORDER_UUID)));
    }

    @Test
    public void allowsProceed_onlyForPaidExemptedOrNotApplicable() {
        assertTrue(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.PAID));
        assertTrue(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.EXEMPTED));
        assertTrue(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.NOT_APPLICABLE));
        assertFalse(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.PENDING));
        assertFalse(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.POSTED));
        assertFalse(ProgramPaymentGateService.allowsProceed(OpenMrsPaymentStatus.UNKNOWN));
    }

    private Sample sampleWithOrder(String referringId) {
        Sample sample = new Sample();
        sample.setReferringId(referringId);
        return sample;
    }
}
