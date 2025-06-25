# OpenELIS-Global Odoo Integration

## Overview

The OpenELIS-Global Odoo integration provides a seamless connection between
laboratory information management and business operations. This integration
enables automatic creation of invoices in Odoo whenever tests are ordered in
OpenELIS, streamlining the billing process and reducing manual data entry. The
system maps laboratory tests to corresponding products in Odoo, ensuring
accurate pricing and inventory tracking. This integration is designed to be
flexible, allowing different laboratories to map their specific tests to
appropriate Odoo products and prices.

The integration follows a modular architecture, separating concerns between
communication, business logic, and configuration. It uses Odoo's XML-RPC API for
reliable communication, with proper error handling and logging to ensure
operational reliability. The system is designed to be easily configurable,
allowing administrators to set up test-to-product mappings without modifying
code.

## Architecture

### Core Components

#### 1. Event System

The integration is built around OpenELIS's event system, specifically listening
for sample creation events. The event listener subscribes to the event bus and
processes sample creation events as they occur. When a new sample is created,
the system automatically triggers the Odoo integration process. The event
listener handles both the sample data and associated patient information,
providing a complete context for invoice creation.

```java
@Component
@SuppressWarnings("unused")
public class SamplePatientUpdateDataCreatedEventListener
        implements EventSubscriber<SamplePatientUpdateDataCreatedEvent> {

    @Autowired
    private EventBus<SamplePatientUpdateDataCreatedEvent> eventBus;

    @PostConstruct
    public void init() {
        eventBus.subscribe(this);
    }

    @EventListener
    public void handleSamplePatientUpdateDataCreatedEvent(SamplePatientUpdateDataCreatedEvent event) {
        onEvent(event);
    }

    @Override
    public void onEvent(SamplePatientUpdateDataCreatedEvent event) {
        try {
            SamplePatientUpdateData updateData = event.getUpdateData();
            PatientManagementInfo patientInfo = event.getPatientInfo();
            // Process sample creation event
        } catch (Exception e) {
            LogEvent.logError(this.getClass().getSimpleName(), "onEvent",
                    "Error processing sample creation event: " + e.getMessage());
        }
    }
}
```

#### 2. Integration Service

The `OdooIntegrationService` serves as the central orchestrator of the
integration process. It receives sample data from the event system and manages
the entire workflow of creating invoices in Odoo. The service processes the
sample data, extracts relevant test information, and coordinates with other
components to ensure accurate invoice creation. It maintains transactional
integrity and provides comprehensive error handling to ensure reliable
operation.

#### 3. Odoo Client

The `OdooClient` manages all communication with the Odoo server through its
XML-RPC API. It handles authentication, session management, and all API
operations required for the integration. The client implements connection
pooling and retry mechanisms to ensure reliable communication, while providing
detailed logging of all interactions with the Odoo server.

#### 4. Configuration System

The `TestProductMapping` component provides a flexible and maintainable way to
manage the mapping between OpenELIS tests and Odoo products. It loads
configuration from environment variables or configuration files, validates the
mappings, and provides a clean interface for other components to access the
mapping data. The system is designed to support dynamic updates without
requiring system restart.

## Configuration

The configuration of the Odoo integration is designed to be flexible and
environment-agnostic. The system uses a combination of environment variables and
configuration files to manage different aspects of the integration. This
approach allows for easy deployment across different environments while
maintaining security and flexibility.

### Deployment Repository Setup

The integration is deployed using Docker containers, which ensures consistent
behavior across different environments. The deployment configuration is stored
in a separate repository to maintain a clean separation between the integration
code and its deployment configuration.

To set up the integration in a new environment:

1. Create a new repository for deployment configuration:

```bash
mkdir openelis-odoo-deployment
cd openelis-odoo-deployment
```

2. Create a Docker Compose file for both systems:

```yaml
version: "3"
services:
  openelis:
    image: openelis-global:latest
    environment:
      - ODOO_SERVER_URL=http://odoo:8069
      - ODOO_DATABASE_NAME=openelis
      - ODOO_USERNAME=admin
      - ODOO_PASSWORD=admin
      - ODOO_TEST_PRODUCT_MAPPING=test1=1,test2=2
      - ODOO_TEST_PRICE_MAPPING=test1=100.0,test2=150.0

  odoo:
    image: odoo:16.0
    environment:
      - POSTGRES_DB=openelis
      - POSTGRES_USER=odoo
      - POSTGRES_PASSWORD=odoo
```

The Docker Compose file defines two services: OpenELIS and Odoo. The OpenELIS
service is configured with environment variables that control the integration
behavior, while the Odoo service is configured with database connection
parameters. This setup ensures that both systems can communicate effectively
while maintaining proper isolation.

3. Create a configuration file for Odoo:

```yaml
# odoo.conf
[options]
admin_passwd = admin
db_host = db
db_port = 5432
db_user = odoo
db_password = odoo
addons_path = /mnt/extra-addons
```

The Odoo configuration file defines the database connection parameters and other
Odoo-specific settings. The `addons_path` parameter specifies where Odoo should
look for additional modules, which is important for any customizations or
extensions.

### Test-to-Product Mapping

The test-to-product mapping is a crucial aspect of the integration, as it
defines how OpenELIS tests correspond to Odoo products. This mapping is
configured using environment variables to allow for easy updates without
modifying code or restarting the application.

The mapping configuration uses a simple but effective format:

```bash
# Format: testId=productId,testId=productId,...
ODOO_TEST_PRODUCT_MAPPING=test1=1,test2=2,test3=3

# Format: testId=price,testId=price,...
ODOO_TEST_PRICE_MAPPING=test1=100.0,test2=150.0,test3=200.0
```

Each test ID is mapped to both a product ID and a price. The product ID
corresponds to a product in Odoo, while the price defines the cost of the test.
This dual mapping ensures that both the product and its price are correctly
associated with each test.

The mapping system is designed to be:

- Easy to update: Changes can be made by updating environment variables
- Validated: The system checks for missing or invalid mappings
- Maintainable: The simple format makes it easy to understand and modify
- Secure: Sensitive information is stored in environment variables

When the system starts, it loads these mappings into memory for quick access. If
any mappings are missing or invalid, the system logs appropriate warnings and
may prevent certain operations from proceeding until the configuration is
corrected.

## Future Features

### Phase 1: Enhanced Invoice Management

The first phase of enhancements will focus on improving invoice management
capabilities. This includes support for different invoice types,
customer-specific pricing, tax calculation, and payment tracking. These
improvements will provide more flexibility in handling different billing
scenarios and improve the accuracy of financial records.

### Phase 2: Sales Order Management

The second third phase will implement full sales order management capabilities.
This includes sales order creation, order status tracking, delivery management,
and customer portal integration. These features will provide a complete view of
the order lifecycle and improve customer service.

### Phase 4: Advanced Features

The final phase will introduce advanced features such as real-time
synchronization, batch processing, reporting integration, and customer
relationship management. These features will enhance the overall functionality
of the integration and provide more value to users.

## Documentation Links

- [Odoo External API Documentation](https://www.odoo.com/documentation/16.0/developer/reference/external_api.html) -
  Comprehensive guide to Odoo's external API

- [Odoo Invoice Management](https://www.odoo.com/documentation/16.0/applications/finance/accounting/customer_invoicing/invoice_management.html) -
  Documentation for invoice creation and management
