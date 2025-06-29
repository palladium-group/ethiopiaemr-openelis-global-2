package org.openelisglobal.odoo.config;

import jakarta.annotation.PostConstruct;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.exception.OdooConnectionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration class for Odoo integration settings. This class validates the
 * configuration and ensures all required settings are present.
 */
@Component
public class OdooConfig {

    @Value("${odoo.server.url}")
    private String serverUrl;

    @Value("${odoo.database.name}")
    private String databaseName;

    @Value("${odoo.username}")
    private String username;

    @Value("${odoo.password}")
    private String password;

    @Value("${odoo.connection.pool.size:10}")
    private int connectionPoolSize;

    @Value("${odoo.connection.pool.timeout.connection:5000}")
    private int connectionTimeout;

    @Value("${odoo.connection.pool.timeout.reply:30000}")
    private int replyTimeout;

    @PostConstruct
    public void validate() {
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Starting Odoo configuration validation...");
        validateRequiredField("odoo.server.url", serverUrl);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.server.url: " + serverUrl);
        validateRequiredField("odoo.database.name", databaseName);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.database.name: " + databaseName);
        validateRequiredField("odoo.username", username);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.username: " + username);
        validateRequiredField("odoo.password", password);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.password: [MASKED]");
        validateConnectionPoolSize();
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.connection.pool.size: " + connectionPoolSize);
        validateTimeouts();
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.connection.pool.timeout.connection: " + connectionTimeout);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Validated odoo.connection.pool.timeout.reply: " + replyTimeout);
        LogEvent.logInfo(this.getClass().getSimpleName(), "validate", "Odoo configuration validation completed. Summary: [serverUrl=" + serverUrl + ", databaseName=" + databaseName + ", username=" + username + ", password=[MASKED], poolSize=" + connectionPoolSize + ", connectionTimeout=" + connectionTimeout + ", replyTimeout=" + replyTimeout + "]");
    }

    private void validateRequiredField(String fieldName, String value) {
        if (value == null || value.trim().isEmpty()) {
            String message = "Required Odoo configuration field is missing: " + fieldName;
            LogEvent.logError(this.getClass().getSimpleName(), "validate", message);
            throw new OdooConnectionException(message);
        }
    }

    private void validateConnectionPoolSize() {
        if (connectionPoolSize < 1) {
            String message = "Odoo connection pool size must be greater than 0";
            LogEvent.logError(this.getClass().getSimpleName(), "validate", message);
            throw new OdooConnectionException(message);
        }
    }

    private void validateTimeouts() {
        if (connectionTimeout < 1000) {
            String message = "Odoo connection timeout must be at least 1000ms";
            LogEvent.logError(this.getClass().getSimpleName(), "validate", message);
            throw new OdooConnectionException(message);
        }
        if (replyTimeout < 5000) {
            String message = "Odoo reply timeout must be at least 5000ms";
            LogEvent.logError(this.getClass().getSimpleName(), "validate", message);
            throw new OdooConnectionException(message);
        }
    }
}
 