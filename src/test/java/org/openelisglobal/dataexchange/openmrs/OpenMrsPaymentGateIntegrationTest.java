package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentGateService;
import org.openelisglobal.dataexchange.openmrs.service.OpenMrsPaymentVerificationService;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.service.SamplePatientEntryService;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Integration tests for OpenMRS payment gate enforcement in the service layer.
 */
public class OpenMrsPaymentGateIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String FHIR_ORDER_ID = "9101";
    private static final String HL7_ORDER_ID = "9102";

    @Autowired
    private OpenMrsPaymentGateService paymentGateService;
    @Autowired
    private SamplePatientEntryService samplePatientEntryService;
    @Autowired
    private ElectronicOrderService electronicOrderService;
    @Autowired
    private SiteInformationService siteInformationService;

    private OpenMrsPaymentVerificationService originalVerificationService;
    private String originalGateEnabledValue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/openmrs-payment-gate.xml");
        originalVerificationService = (OpenMrsPaymentVerificationService) ReflectionTestUtils
                .getField(paymentGateService, "paymentVerificationService");
        SiteInformation gateSetting = siteInformationService.getSiteInformationByName("openmrsPaymentGateEnabled");
        originalGateEnabledValue = gateSetting.getValue();
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(paymentGateService, "paymentVerificationService", originalVerificationService);
        restoreGateEnabled(originalGateEnabledValue);
    }

    @Test
    public void assertSampleCollectionAllowed_blocksUnpaidFhirOrderWhenGateEnabled() {
        enablePaymentGate();

        try {
            paymentGateService.assertSampleCollectionAllowed(updateDataForOrder(FHIR_ORDER_ID));
            fail("Expected LIMSRuntimeException for unpaid FHIR order");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Sample collection is not allowed"));
            assertTrue(e.getMessage().contains("UNKNOWN"));
        }
    }

    @Test
    public void persistData_blocksUnpaidFhirOrderBeforeSampleSave() {
        enablePaymentGate();

        try {
            samplePatientEntryService.persistData(updateDataForOrder(FHIR_ORDER_ID), null, new PatientManagementInfo(),
                    new SamplePatientEntryForm(), mock(HttpServletRequest.class));
            fail("Expected LIMSRuntimeException before sample persistence");
        } catch (LIMSRuntimeException e) {
            assertTrue(e.getMessage().contains("Sample collection is not allowed"));
            assertTrue(e.getMessage().contains("UNKNOWN"));
        }
    }

    @Test
    public void assertSampleCollectionAllowed_allowsPaidFhirOrderWhenGateEnabled() {
        enablePaymentGate();
        OpenMrsPaymentVerificationService verificationService = mock(OpenMrsPaymentVerificationService.class);
        ReflectionTestUtils.setField(paymentGateService, "paymentVerificationService", verificationService);

        when(verificationService.verifyAndSyncForElectronicOrder(any(ElectronicOrder.class), eq(true))).thenReturn(
                new PaymentVerificationResult("order-uuid-9101", OpenMrsPaymentStatus.PAID, true, true, null));

        paymentGateService.assertSampleCollectionAllowed(updateDataForOrder(FHIR_ORDER_ID));
        verify(verificationService).verifyAndSyncForElectronicOrder(any(ElectronicOrder.class), eq(true));
    }

    @Test
    public void assertSampleCollectionAllowed_skipsHl7OrdersWhenGateEnabled() {
        enablePaymentGate();
        OpenMrsPaymentVerificationService verificationService = mock(OpenMrsPaymentVerificationService.class);
        ReflectionTestUtils.setField(paymentGateService, "paymentVerificationService", verificationService);

        paymentGateService.assertSampleCollectionAllowed(updateDataForOrder(HL7_ORDER_ID));

        verify(verificationService, never()).verifyAndSyncForElectronicOrder(any(), eq(true));
    }

    @Test
    public void assertSampleCollectionAllowed_skipsWhenGateDisabled() {
        disablePaymentGate();
        OpenMrsPaymentVerificationService verificationService = mock(OpenMrsPaymentVerificationService.class);
        ReflectionTestUtils.setField(paymentGateService, "paymentVerificationService", verificationService);

        paymentGateService.assertSampleCollectionAllowed(updateDataForOrder(FHIR_ORDER_ID));

        verify(verificationService, never()).verifyAndSyncForElectronicOrder(any(), eq(true));
    }

    private SamplePatientUpdateData updateDataForOrder(String electronicOrderId) {
        ElectronicOrder order = electronicOrderService.get(electronicOrderId);
        SamplePatientUpdateData updateData = new SamplePatientUpdateData("1");
        ReflectionTestUtils.setField(updateData, "electronicOrders", List.of(order));
        return updateData;
    }

    private void enablePaymentGate() {
        setGateEnabled("true");
    }

    private void disablePaymentGate() {
        setGateEnabled("false");
    }

    private void restoreGateEnabled(String value) {
        if (value != null) {
            setGateEnabled(value);
        }
    }

    private void setGateEnabled(String value) {
        SiteInformation gateSetting = siteInformationService.getSiteInformationByName("openmrsPaymentGateEnabled");
        gateSetting.setValue(value);
        siteInformationService.persistData(gateSetting, false);
        ConfigurationProperties.loadDBValuesIntoConfiguration();
        if (!ConfigurationProperties.getInstance().isPropertyValueEqual(Property.OPENMRS_PAYMENT_GATE_ENABLED, value)) {
            ConfigurationProperties.getInstance().setPropertyValue(Property.OPENMRS_PAYMENT_GATE_ENABLED, value);
            ConfigurationProperties.forceReload();
        }
    }
}
