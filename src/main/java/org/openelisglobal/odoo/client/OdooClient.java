package org.openelisglobal.odoo.client;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.odoo.exception.OdooConnectionException;
import org.openelisglobal.odoo.exception.OdooOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Client class for handling XML-RPC communication with Odoo. This is a
 * simplified version for the MVP that focuses on invoice creation.
 */
@Component
public class OdooClient {

    @Value("${odoo.server.url}")
    private String serverUrl;

    @Value("${odoo.database.name}")
    private String databaseName;

    @Value("${odoo.username}")
    private String username;

    @Value("${odoo.password}")
    private String password;

    private XmlRpcClient client;
    private Integer userId; // Odoo user ID returned after authentication

    /**
     * Initializes the Odoo client with the configured connection parameters.
     */
    public void init() {
        try {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(serverUrl + "/xmlrpc/2/common"));
            config.setEnabledForExtensions(true);

            client = new XmlRpcClient();
            client.setConfig(config);

            // Authenticate and get user ID
            Object[] params = new Object[] { databaseName, username, password, new HashMap<>() };
            userId = (Integer) client.execute("authenticate", params);

            LogEvent.logInfo(this.getClass().getSimpleName(), "init",
                    "Successfully initialized Odoo client with user ID: " + userId);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "init",
                    "Error initializing Odoo client: " + e.getMessage());
            throw new OdooConnectionException("Failed to initialize Odoo client", e);
        }
    }

    /**
     * Creates a new invoice in Odoo.
     *
     * @param data The data to create the invoice with
     * @return The ID of the created invoice
     */
    public Integer createInvoice(Map<String, Object> data) {
        try {
            Object[] params = new Object[] { databaseName, userId, password, "account.move", "create", data };
            return (Integer) client.execute("execute_kw", params);
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "createInvoice",
                    "Error creating invoice in Odoo: " + e.getMessage());
            throw new OdooOperationException("Failed to create invoice in Odoo", e);
        }
    }
}
