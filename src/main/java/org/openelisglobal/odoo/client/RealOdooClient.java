package org.openelisglobal.odoo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component
public class RealOdooClient implements OdooConnection {

    private final OdooClient odooClient;

    private boolean available = false;

    @Autowired
    public RealOdooClient(OdooClient odooClient) {
        this.odooClient = odooClient;
        try {
            odooClient.init();
            available = true;
            log.info("Successfully connected to Odoo at startup.");
        } catch (Exception e) {
            available = false;
            log.error("Failed to connect to Odoo at startup: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public Integer create(String model, List<Map<String, Object>> dataParams) {
        if (!available) throw new IllegalStateException("Odoo is not available");
        return odooClient.create(model, dataParams);
    }
}
