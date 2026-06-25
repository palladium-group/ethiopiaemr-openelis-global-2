package org.openelisglobal.dataexchange.openmrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class OpenMrsPaymentConfigurationTest {

    @Mock
    private ConfigurationProperties configurationProperties;

    private OpenMrsPaymentConfiguration configuration;

    @Before
    public void setUp() {
        configuration = new OpenMrsPaymentConfiguration(configurationProperties);
        ReflectionTestUtils.setField(configuration, "cacheSeconds", 45);
    }

    @Test
    public void isGateEnabled_whenPropertyTrue() {
        when(configurationProperties.isPropertyValueEqual(Property.OPENMRS_PAYMENT_GATE_ENABLED, "true")).thenReturn(true);
        assertTrue(configuration.isGateEnabled());
    }

    @Test
    public void isGateEnabled_whenPropertyFalse() {
        when(configurationProperties.isPropertyValueEqual(Property.OPENMRS_PAYMENT_GATE_ENABLED, "true")).thenReturn(false);
        assertFalse(configuration.isGateEnabled());
    }

    @Test
    public void getPaymentStatusExtensionUrl_usesConfiguredValue() {
        when(configurationProperties.getPropertyValue(Property.OPENMRS_PAYMENT_EXTENSION_URL))
                .thenReturn("https://example.org/payment-status");
        assertEquals("https://example.org/payment-status", configuration.getPaymentStatusExtensionUrl());
    }

    @Test
    public void getPaymentStatusExtensionUrl_fallsBackToDefaultWhenBlank() {
        when(configurationProperties.getPropertyValue(Property.OPENMRS_PAYMENT_EXTENSION_URL)).thenReturn("  ");
        assertEquals(OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL,
                configuration.getPaymentStatusExtensionUrl());
    }

    @Test
    public void getCacheSeconds_returnsInjectedValue() {
        assertEquals(45, configuration.getCacheSeconds());
    }

    @Test
    public void getCacheSeconds_fallsBackWhenNonPositive() {
        ReflectionTestUtils.setField(configuration, "cacheSeconds", 0);
        assertEquals(OpenMrsPaymentConstants.DEFAULT_CACHE_SECONDS, configuration.getCacheSeconds());
    }
}
