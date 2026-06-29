package org.openelisglobal.dataexchange.openmrs.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * Integration tests for {@link OpenMrsPaymentRestController}.
 */
public class OpenMrsPaymentRestControllerIntegrationTest extends BaseWebContextSensitiveTest {

    private static final String FHIR_ORDER_ID = "9101";

    @Autowired
    private SiteInformationService siteInformationService;

    private String originalGateEnabledValue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        executeDataSetWithStateManagement("testdata/openmrs-payment-gate.xml");
        SiteInformation gateSetting = siteInformationService.getSiteInformationByName("openmrsPaymentGateEnabled");
        originalGateEnabledValue = gateSetting.getValue();
        enablePaymentGate();
    }

    @After
    public void tearDown() {
        if (originalGateEnabledValue != null) {
            setGateEnabled(originalGateEnabledValue);
        }
    }

    @Test
    public void refreshElectronicOrderPaymentStatus_returnsNotFoundForMissingOrder() throws Exception {
        mockMvc.perform(get("/rest/openmrs-payment/electronic-order/99999").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void refreshElectronicOrderPaymentStatus_returnsFailClosedStatusForFhirOrder() throws Exception {
        mockMvc.perform(
                get("/rest/openmrs-payment/electronic-order/" + FHIR_ORDER_ID).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.collectionAllowed").value(false));
    }

    @Test
    public void getPaymentStatus_returnsBadRequestWhenOrderUuidMissing() throws Exception {
        mockMvc.perform(get("/rest/openmrs-payment/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getPaymentStatus_returnsFailClosedStatusWhenRemoteFhirUnavailable() throws Exception {
        mockMvc.perform(get("/rest/openmrs-payment/status").param("orderUuid", "order-uuid-9101")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.orderUuid").value("order-uuid-9101"))
                .andExpect(jsonPath("$.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.collectionAllowed").value(false));
    }

    private void enablePaymentGate() {
        setGateEnabled("true");
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
