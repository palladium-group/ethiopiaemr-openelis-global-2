package org.openelisglobal.odoo.config;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import lombok.Getter;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Component;

/**
 * Loads and provides mapping between OpenELIS test codes and Odoo product info
 * (name, price) from a properties file (odoo-test-product-mapping.properties).
 */
@Component
public class TestProductMapping {

    private static final String ENV_MAPPING_FILE = "ODOO_MAPPING_FILE";
    private static final String DEFAULT_FILE = "odoo-test-product-mapping.properties";
    private static final String PREFIX = "odoo.test.product.map.";

    @Getter
    public static class TestProductInfo {
        private final String productName;
        private final double price;

        public TestProductInfo(String productName, double price) {
            this.productName = productName;
            this.price = price;
        }

    }

    private final Map<String, TestProductInfo> testToProductInfo = new HashMap<>();

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        boolean loaded = loadFromEnvironment(props) || loadFromClasspath(props);

        if (!loaded) {
            LogEvent.logError(getClass().getSimpleName(), "init",
                    "No mapping file could be loaded (environment or classpath).");
            return;
        }

        int mappingsLoaded = parseMappings(props);

        LogEvent.logInfo(getClass().getSimpleName(), "init", "Total mappings loaded: " + mappingsLoaded);

        if (mappingsLoaded == 0) {
            LogEvent.logWarn(getClass().getSimpleName(), "init", "No valid mappings found.");
        }
    }

    private boolean loadFromEnvironment(Properties props) {
        String path = System.getenv(ENV_MAPPING_FILE);
        if (path != null && !path.isBlank()) {
            try (InputStream in = new FileInputStream(path)) {
                props.load(in);
                LogEvent.logInfo(getClass().getSimpleName(), "loadFromEnvironment",
                        "Loaded mapping file from ODOO_MAPPING_FILE: " + path);
                return true;
            } catch (IOException e) {
                LogEvent.logError(getClass().getSimpleName(), "loadFromEnvironment",
                        "Failed to load mapping from path " + path + ": " + e.getMessage());
            }
        }
        return false;
    }

    private boolean loadFromClasspath(Properties props) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_FILE)) {
            if (in != null) {
                props.load(in);
                LogEvent.logInfo(getClass().getSimpleName(), "loadFromClasspath",
                        "Loaded mapping file from classpath: " + DEFAULT_FILE);
                return true;
            } else {
                LogEvent.logWarn(getClass().getSimpleName(), "loadFromClasspath",
                        "Mapping file not found on classpath: " + DEFAULT_FILE);
            }
        } catch (IOException e) {
            LogEvent.logError(getClass().getSimpleName(), "loadFromClasspath",
                    "Failed to load mapping from classpath: " + e.getMessage());
        }
        return false;
    }

    private int parseMappings(Properties props) {
        int count = 0;
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith(PREFIX))
                continue;

            String testCode = key.substring(PREFIX.length());
            String value = props.getProperty(key);
            String[] parts = value.split(",");

            if (parts.length != 2) {
                LogEvent.logError(getClass().getSimpleName(), "parseMappings",
                        "Invalid format for " + key + ": " + value);
                continue;
            }

            String productName = parts[0].trim();
            try {
                double price = Double.parseDouble(parts[1].trim());
                testToProductInfo.put(testCode, new TestProductInfo(productName, price));
                count++;
            } catch (NumberFormatException e) {
                LogEvent.logError(getClass().getSimpleName(), "parseMappings",
                        "Invalid price for " + key + ": " + parts[1]);
            }
        }
        return count;
    }

    public boolean hasValidMapping(String testCode) {
        return testToProductInfo.containsKey(testCode);
    }

    public String getProductName(String testCode) {
        TestProductInfo info = testToProductInfo.get(testCode);
        return info != null ? info.getProductName() : null;
    }

    public Double getPrice(String testCode) {
        TestProductInfo info = testToProductInfo.get(testCode);
        return info != null ? info.getPrice() : null;
    }
}
