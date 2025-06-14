package org.openelisglobal.odoo.config;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration class for mapping OpenELIS test IDs to Odoo product IDs and
 * prices. This class maintains the mapping between OpenELIS tests and their
 * corresponding Odoo products and prices, which is used for integration with
 * the Odoo system.
 */
@Component
public class TestProductMapping {

    @Value("${odoo.test.product.mapping}")
    private String productMappingString;

    @Value("${odoo.test.price.mapping}")
    private String priceMappingString;

    private final Map<String, Integer> testToProductMap = new HashMap<>();
    private final Map<String, Double> testToPriceMap = new HashMap<>();

    /**
     * Initializes the mapping by parsing the configuration strings. This method is
     * called after dependency injection is complete.
     */
    @PostConstruct
    public void init() {
        parseProductMapping();
        parsePriceMapping();
    }

    /**
     * Parses the product mapping string and populates the testToProductMap. The
     * input string should be in the format "testId=productId,testId=productId,..."
     * Invalid entries are silently skipped.
     */
    private void parseProductMapping() {
        if (productMappingString != null && !productMappingString.isEmpty()) {
            String[] entries = productMappingString.split(",");
            for (String entry : entries) {
                String[] kv = entry.split("=");
                if (kv.length == 2) {
                    try {
                        testToProductMap.put(kv[0].trim(), Integer.parseInt(kv[1].trim()));
                    } catch (NumberFormatException e) {
                        LogEvent.logError(this.getClass().getSimpleName(), "parseProductMapping",
                                "Invalid product ID format in mapping: " + entry);
                    }
                }
            }
        }
    }

    /**
     * Parses the price mapping string and populates the testToPriceMap. The input
     * string should be in the format "testId=price,testId=price,..." Invalid
     * entries are silently skipped.
     */
    private void parsePriceMapping() {
        if (priceMappingString != null && !priceMappingString.isEmpty()) {
            String[] entries = priceMappingString.split(",");
            for (String entry : entries) {
                String[] kv = entry.split("=");
                if (kv.length == 2) {
                    try {
                        testToPriceMap.put(kv[0].trim(), Double.parseDouble(kv[1].trim()));
                    } catch (NumberFormatException e) {
                        LogEvent.logError(this.getClass().getSimpleName(), "parsePriceMapping",
                                "Invalid price format in mapping: " + entry);
                    }
                }
            }
        }
    }

    /**
     * Retrieves the Odoo product ID for a given test ID.
     *
     * @param testId The OpenELIS test ID to look up
     * @return The corresponding Odoo product ID, or null if not found
     */
    public Integer getProductId(String testId) {
        return testToProductMap.get(testId);
    }

    /**
     * Retrieves the price for a given test ID.
     *
     * @param testId The OpenELIS test ID to look up
     * @return The price of the test, or null if not found
     */
    public Double getPrice(String testId) {
        return testToPriceMap.get(testId);
    }

    /**
     * Checks if a test ID has a valid product mapping.
     *
     * @param testId The OpenELIS test ID to check
     * @return true if the test has both a product ID and price mapping
     */
    public boolean hasValidMapping(String testId) {
        return testToProductMap.containsKey(testId) && testToPriceMap.containsKey(testId);
    }
}
