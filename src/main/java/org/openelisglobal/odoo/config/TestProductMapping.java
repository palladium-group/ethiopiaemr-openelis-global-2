package org.openelisglobal.odoo.config;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

/**
 * Loads and provides mapping between OpenELIS test codes and Odoo product info
 * (name, price) from a properties file (odoo-test-product-mapping.properties).
 * <p>
 * The file is loaded from the path specified by the ODOO_MAPPING_FILE
 * environment variable, or from the classpath if not set.
 */
@Component
public class TestProductMapping {

    private static final String ENV_MAPPING_FILE = "ODOO_MAPPING_FILE";
    private static final String DEFAULT_FILE = "odoo-test-product-mapping.properties";
    private static final String PREFIX = "odoo.test.product.map.";

    public static class TestProductInfo {
        public final String productName;
        public final double price;

        public TestProductInfo(String productName, double price) {
            this.productName = productName;
            this.price = price;
        }
    }

    private final Map<String, TestProductInfo> testToProductInfo = new HashMap<>();

    @PostConstruct
    public void init() {
        loadMappings();
    }

    private void loadMappings() {
        Properties props = new Properties();
        String mappingFilePath = System.getenv(ENV_MAPPING_FILE);
        boolean loaded = false;
        if (mappingFilePath != null && !mappingFilePath.isEmpty()) {
            try (InputStream in = new FileInputStream(mappingFilePath)) {
                props.load(in);
                loaded = true;
                LogEvent.logInfo(this.getClass().getSimpleName(), "loadMappings",
                        "Loaded mapping file from ODOO_MAPPING_FILE: " + mappingFilePath);
            } catch (IOException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                        "Failed to load mapping file from ODOO_MAPPING_FILE: " + mappingFilePath + ", error: "
                                + e.getMessage());
            }
        }
        if (!loaded) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_FILE)) {
                if (in != null) {
                    props.load(in);
                    loaded = true;
                    LogEvent.logInfo(this.getClass().getSimpleName(), "loadMappings",
                            "Loaded mapping file from classpath: " + DEFAULT_FILE);
                } else {
                    LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                            "Mapping file not found on classpath: " + DEFAULT_FILE);
                }
            } catch (IOException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                        "Failed to load mapping file from classpath: " + e.getMessage());
            }
        }
        int mappingsLoaded = 0;
        if (loaded) {
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith(PREFIX)) {
                    String testCode = key.substring(PREFIX.length());
                    String value = props.getProperty(key);
                    String[] parts = value.split(",");
                    if (parts.length == 2) {
                        String productName = parts[0].trim();
                        try {
                            double price = Double.parseDouble(parts[1].trim());
                            testToProductInfo.put(testCode, new TestProductInfo(productName, price));
                            mappingsLoaded++;
                        } catch (NumberFormatException e) {
                            LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                                    "Invalid price for " + key + ": " + value);
                        }
                    } else {
                        LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                                "Invalid mapping format for " + key + ": " + value);
                    }
                }
            }
            LogEvent.logInfo(this.getClass().getSimpleName(), "loadMappings",
                    "Total mappings loaded: " + mappingsLoaded);
            if (mappingsLoaded == 0) {
                LogEvent.logWarn(this.getClass().getSimpleName(), "loadMappings",
                        "No Odoo test-product mappings loaded!");
            }
        } else {
            LogEvent.logError(this.getClass().getSimpleName(), "loadMappings",
                    "No mapping file could be loaded (env or classpath)");
        }
    }

    /**
     * Retrieves the Odoo product name for a given test code.
     * 
     * @param testCode The OpenELIS test code
     * @return The Odoo product name, or null if not found
     */
    public String getProductName(String testCode) {
        TestProductInfo info = testToProductInfo.get(testCode);
        return info != null ? info.productName : null;
    }

    /**
     * Retrieves the price for a given test code.
     * 
     * @param testCode The OpenELIS test code
     * @return The price, or null if not found
     */
    public Double getPrice(String testCode) {
        TestProductInfo info = testToProductInfo.get(testCode);
        return info != null ? info.price : null;
    }

    /**
     * Checks if a test code has a valid mapping.
     * 
     * @param testCode The OpenELIS test code
     * @return true if mapping exists
     */
    public boolean hasValidMapping(String testCode) {
        return testToProductInfo.containsKey(testCode);
    }
}
