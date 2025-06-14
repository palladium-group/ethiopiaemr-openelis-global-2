# OpenELIS-Global Odoo Integration

This document describes the integration between OpenELIS-Global and Odoo for
managing sales orders and inventory.

## Overview

The integration allows OpenELIS-Global to:

- Create sales orders in Odoo when tests are ordered
- Update inventory in Odoo when tests are performed
- Map OpenELIS tests to Odoo products and prices

## Architecture

The integration consists of the following components:

1. **OdooClient**: Handles XML-RPC communication with Odoo

   - Connection pooling for better performance
   - Automatic retry for transient failures
   - Secure credential management

2. **OdooIntegrationService**: Business logic for integration

   - Creates sales orders
   - Updates inventory
   - Handles test-to-product mapping

3. **TestProductMapping**: Configuration for test mappings
   - Maps OpenELIS test IDs to Odoo product IDs
   - Maps OpenELIS test IDs to prices

## Configuration

### Environment Variables

The integration can be configured using the following environment variables:

```bash
ODOO_SERVER_URL=http://localhost:8069
ODOO_DATABASE_NAME=openelis
ODOO_USERNAME=admin
ODOO_API_KEY=your-api-key
ODOO_CONNECTION_POOL_SIZE=10
ODOO_CONNECTION_TIMEOUT=5000
ODOO_REPLY_TIMEOUT=30000
ODOO_TEST_PRODUCT_MAPPING=test1=1,test2=2,test3=3
ODOO_TEST_PRICE_MAPPING=test1=100.0,test2=150.0,test3=200.0
```

### Configuration File

Alternatively, you can configure the integration in `application-odoo.yml`:

```yaml
odoo:
  server:
    url: ${ODOO_SERVER_URL:http://localhost:8069}
  database:
    name: ${ODOO_DATABASE_NAME:openelis}
  username: ${ODOO_USERNAME:admin}
  api:
    key: ${ODOO_API_KEY:your-api-key}
  connection:
    pool:
      size: ${ODOO_CONNECTION_POOL_SIZE:10}
      timeout:
        connection: ${ODOO_CONNECTION_TIMEOUT:5000}
        reply: ${ODOO_REPLY_TIMEOUT:30000}
  test:
    product:
      mapping: ${ODOO_TEST_PRODUCT_MAPPING:test1=1,test2=2,test3=3}
    price:
      mapping: ${ODOO_TEST_PRICE_MAPPING:test1=100.0,test2=150.0,test3=200.0}
```

## Test-to-Product Mapping

### Current Implementation

The current implementation uses a simple string format to map OpenELIS tests to
Odoo products and prices:

```
testId=productId,testId=productId,...
testId=price,testId=price,...
```

Example:

```
test1=1,test2=2,test3=3
test1=100.0,test2=150.0,test3=200.0
```

### Recommended Implementation

Based on Odoo's best practices, we should consider the following improvements:

1. **Product Template vs Product Variant**:

   - Odoo uses a two-level product structure:
     - Product Template: Base product with common attributes
     - Product Variant: Specific instance with unique attributes
   - For tests, we should:
     - Create a product template for each test type
     - Use variants for different price points or configurations

2. **Product Categories**:

   - Organize tests into product categories in Odoo
   - Example structure:
     ```
     Laboratory Tests/
     ├── Hematology/
     │   ├── Complete Blood Count
     │   └── Blood Type
     ├── Chemistry/
     │   ├── Glucose
     │   └── Cholesterol
     └── Microbiology/
         ├── Culture
         └── Sensitivity
     ```

3. **Pricing Strategy**:

   - Use Odoo's pricelist feature instead of hardcoded prices
   - Benefits:
     - Support for different currencies
     - Customer-specific pricing
     - Time-based pricing
     - Volume discounts

4. **Inventory Management**:
   - Use Odoo's stock management features:
     - Track inventory levels
     - Set reorder points
     - Manage multiple locations
     - Handle returns and adjustments

### Mapping Configuration

The mapping should be stored in a more structured format:

```yaml
odoo:
  test:
    mapping:
      - test_id: "CBC"
        product:
          template_id: 1
          variant_id: 1
          category_id: 1
          pricelist_id: 1
        inventory:
          location_id: 1
          reorder_point: 10
      - test_id: "GLUCOSE"
        product:
          template_id: 2
          variant_id: 2
          category_id: 2
          pricelist_id: 1
        inventory:
          location_id: 1
          reorder_point: 20
```

## Odoo API Integration

### Official Documentation

Odoo provides several ways to integrate with external applications:

1. **XML-RPC API** (Current Implementation):

   - [XML-RPC Documentation](https://www.odoo.com/documentation/16.0/developer/reference/external_api.html)
   - Pros:
     - Simple to implement
     - Works with any programming language
     - Good for basic operations
   - Cons:
     - Limited performance
     - No real-time updates
     - Basic error handling

2. **JSON-RPC API** (Recommended):

   - [JSON-RPC Documentation](https://www.odoo.com/documentation/16.0/developer/reference/external_api.html#json-rpc)
   - Pros:
     - Better performance
     - More modern approach
     - Better error handling
     - Support for batch operations

3. **Odoo External API** (Enterprise):
   - [External API Documentation](https://www.odoo.com/documentation/16.0/developer/reference/external_api.html#external-api)
   - Pros:
     - Real-time updates
     - Better security
     - Advanced features
   - Cons:
     - Enterprise feature
     - More complex setup

### Recommended Changes

Based on Odoo's recommendations, we should:

1. **Switch to JSON-RPC**:

   ```java
   // Example JSON-RPC client
   public class OdooJsonRpcClient {
       private final String serverUrl;
       private final String database;
       private final String username;
       private final String password;
       private final ObjectMapper objectMapper;

       public OdooJsonRpcClient(String serverUrl, String database,
                               String username, String password) {
           this.serverUrl = serverUrl;
           this.database = database;
           this.username = username;
           this.password = password;
           this.objectMapper = new ObjectMapper();
       }

       public <T> T execute(String model, String method,
                          Map<String, Object> params, Class<T> responseType) {
           // Implementation
       }
   }
   ```

2. **Use Batch Operations**:

   ```java
   // Example batch operation
   public List<Integer> createSalesOrders(List<Map<String, Object>> orders) {
       return odooClient.execute("sale.order", "create_multi",
           Map.of("orders", orders), List.class);
   }
   ```

3. **Implement Webhooks**:

   ```java
   @RestController
   @RequestMapping("/api/odoo/webhooks")
   public class OdooWebhookController {
       @PostMapping("/inventory")
       public void handleInventoryUpdate(@RequestBody InventoryUpdate update) {
           // Handle real-time inventory updates
       }
   }
   ```

4. **Use Odoo's Security Features**:
   - API keys for authentication
   - IP whitelisting
   - Role-based access control
   - SSL/TLS encryption

### Performance Considerations

1. **Connection Pooling**:

   - Use connection pooling for better performance
   - Implement retry mechanisms
   - Handle timeouts properly

2. **Caching**:

   - Cache frequently accessed data
   - Implement cache invalidation
   - Use distributed caching for scalability

3. **Batch Processing**:
   - Use batch operations for multiple records
   - Implement bulk updates
   - Handle partial failures

## Sales Order Creation

When a test is ordered in OpenELIS:

1. The system creates a sales order in Odoo with:

   - Order name: `SO-{accessionNumber}`
   - Client reference: `{accessionNumber}`
   - Order date: Sample collection date
   - Order lines: One line per test with product and price

2. Each order line includes:
   - Product ID (mapped from test ID)
   - Price (mapped from test ID)
   - Quantity: 1
   - Name: "Test: {testId}"

## Inventory Updates

When a test is performed:

1. The system creates inventory adjustments in Odoo:
   - Name: `INV-{accessionNumber}`
   - Product ID (mapped from test ID)
   - Quantity: -1 (decrease inventory)
   - Location ID: 1 (default location)
   - State: "draft"

## Error Handling

The integration includes comprehensive error handling:

1. **Connection Errors**:

   - Retries on transient failures
   - Connection pooling for better reliability
   - Timeout configuration

2. **Operation Errors**:

   - Custom exceptions for different error types
   - Detailed error messages
   - Proper logging

3. **Validation**:
   - Required field validation
   - Test-to-product mapping validation
   - Empty test list validation

## Logging

The integration logs important events:

1. **Info Level**:

   - Successful sales order creation
   - Successful inventory updates
   - Connection pool initialization

2. **Warning Level**:

   - Skipped tests (no product mapping)
   - Retry attempts

3. **Error Level**:
   - Connection failures
   - Operation failures
   - Validation errors

## Security

The integration implements several security measures:

1. **Credential Management**:

   - Environment variable support
   - No hardcoded credentials
   - Secure configuration loading

2. **Connection Security**:
   - Connection pooling
   - Timeout configuration
   - Error handling

## Performance

The integration is optimized for performance:

1. **Connection Pooling**:

   - Configurable pool size
   - Round-robin client selection
   - Connection reuse

2. **Timeout Configuration**:
   - Connection timeout: 5 seconds
   - Reply timeout: 30 seconds

## Troubleshooting

Common issues and solutions:

1. **Connection Issues**:

   - Check Odoo server URL
   - Verify credentials
   - Check network connectivity

2. **Mapping Issues**:

   - Verify test-to-product mapping format
   - Check product IDs exist in Odoo
   - Validate price format

3. **Performance Issues**:
   - Adjust connection pool size
   - Check timeout settings
   - Monitor server load

## Development

To extend the integration:

1. **Add New Features**:

   - Extend `OdooClient` for new operations
   - Add new methods to `OdooIntegrationService`
   - Update configuration as needed

2. **Modify Existing Features**:

   - Update mapping format
   - Change order/inventory structure
   - Adjust error handling

3. **Testing**:
   - Unit tests for new features
   - Integration tests with Odoo
   - Performance testing

## Support

For issues and support:

1. **Documentation**:

   - Check this document
   - Review code comments
   - Check Odoo documentation

2. **Logging**:

   - Check application logs
   - Review error messages
   - Monitor performance

3. **Contact**:
   - OpenELIS-Global team
   - Odoo support
   - Development team
