package org.openelisglobal.dataexchange.openmrs;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.siteinformation.service.SiteInformationService;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Ensures OpenMRS payment gate site settings exist for integration tests
 * without truncating {@code site_information} via DBUnit (other tests may clear
 * it).
 */
public final class OpenMrsPaymentGateTestSupport {

    private static final String GATE_ENABLED_NAME = "openmrsPaymentGateEnabled";
    private static final String EXTENSION_URL_NAME = "openmrsPaymentExtensionUrl";

    private OpenMrsPaymentGateTestSupport() {
    }

    public static void ensurePaymentGateSiteSettings(JdbcTemplate jdbcTemplate) {
        ensureSampleEntryConfigDomain(jdbcTemplate);
        insertSiteInformationIfMissing(jdbcTemplate, 9103, GATE_ENABLED_NAME,
                "Require OpenMRS payment before collecting samples for FHIR electronic orders", "false", "boolean",
                "instructions.openmrs.payment.gate");
        insertSiteInformationIfMissing(jdbcTemplate, 9104, EXTENSION_URL_NAME,
                "FHIR extension URL for OpenMRS payment status on ServiceRequest resources",
                "https://palladiumethiopia.com/fhir/ext/payment-status", "text",
                "instructions.openmrs.payment.ext.url");
        ConfigurationProperties.loadDBValuesIntoConfiguration();
    }

    public static SiteInformation requireGateSetting(SiteInformationService siteInformationService) {
        SiteInformation gateSetting = siteInformationService.getSiteInformationByName(GATE_ENABLED_NAME);
        if (gateSetting == null) {
            throw new IllegalStateException(GATE_ENABLED_NAME + " site_information row missing from test fixtures");
        }
        return gateSetting;
    }

    private static void ensureSampleEntryConfigDomain(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                INSERT INTO site_information_domain (id, name, description)
                SELECT 10, 'sampleEntryConfig',
                       'Configuration for those items which can appear on the sample entry form'
                WHERE NOT EXISTS (
                    SELECT 1 FROM site_information_domain WHERE name = 'sampleEntryConfig'
                )
                """);
    }

    private static void insertSiteInformationIfMissing(JdbcTemplate jdbcTemplate, int id, String name,
            String description, String value, String valueType, String instructionKey) {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM site_information WHERE name = ?",
                Integer.class, name);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        INSERT INTO site_information (
                            id, name, description, value, encrypted, domain_id, value_type, instruction_key, "group", lastupdated
                        )
                        SELECT ?, ?, ?, ?, false, d.id, ?, ?, 0, NOW()
                        FROM site_information_domain d
                        WHERE d.name = 'sampleEntryConfig'
                        LIMIT 1
                        """,
                id, name, description, value, valueType, instructionKey);
    }
}
