package org.openelisglobal.dataexchange.openmrs;

import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads site and application configuration for the OpenMRS payment gate.
 */
@Service
public class OpenMrsPaymentConfiguration {

    private final ConfigurationProperties configurationProperties;

    @Value("${" + OpenMrsPaymentConstants.APPLICATION_PROPERTY_CACHE_SECONDS + ":"
            + OpenMrsPaymentConstants.DEFAULT_CACHE_SECONDS + "}")
    private int cacheSeconds;

    @Autowired
    public OpenMrsPaymentConfiguration(ConfigurationProperties configurationProperties) {
        this.configurationProperties = configurationProperties;
    }

    public boolean isGateEnabled() {
        return configurationProperties.isPropertyValueEqual(Property.OPENMRS_PAYMENT_GATE_ENABLED, "true");
    }

    public String getPaymentStatusExtensionUrl() {
        String configuredUrl = configurationProperties.getPropertyValue(Property.OPENMRS_PAYMENT_EXTENSION_URL);
        if (StringUtils.isBlank(configuredUrl)) {
            return OpenMrsPaymentConstants.DEFAULT_PAYMENT_STATUS_EXTENSION_URL;
        }
        return configuredUrl.trim();
    }

    public int getCacheSeconds() {
        return cacheSeconds > 0 ? cacheSeconds : OpenMrsPaymentConstants.DEFAULT_CACHE_SECONDS;
    }
}
