# Wallet & Settlement Service

A microservice for managing customer wallet balances and performing daily reconciliation with external transaction reports.

## Features

- Customer wallet management (top-up, consume, balance inquiry)
- Transaction ledger with RabbitMQ messaging
- Daily reconciliation with external transaction reports
- CSV export of reconciliation reports
- Docker containerization
- Comprehensive testing

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Running with Docker

```bash
# Build the application
mvn clean package

# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f wallet-app
```

The application will be available at:
- API: http://localhost:8080
- RabbitMQ Management: http://localhost:15672 (guest/guest)
- Swagger UI: http://localhost:8080/swagger-ui.html

### Local Development

```bash
# Start dependencies
docker-compose up postgres rabbitmq -d

# Run application
mvn spring-boot:run
```

## API Endpoints

### Wallet Operations

#### Top Up Wallet
```
POST /api/v1/wallets/{customerId}/topup
Content-Type: application/json

{
  "amount": 100.00,
  "description": "Account top up"
}
```

#### Consume from Wallet
```
POST /api/v1/wallets/{customerId}/consume
Content-Type: application/json

{
  "amount": 25.50,
  "description": "KYC verification service"
}
```

#### Get Wallet Balance
```
GET /api/v1/wallets/{customerId}/balance
```

### Reconciliation Operations

#### Process Reconciliation
```
POST /api/v1/reconciliation/process
Content-Type: multipart/form-data

date: 2024-01-15
file: [CSV file with external_transaction_id,amount,customer_id,type,transaction_date columns]
```

#### Get Reconciliation Report
```
GET /api/v1/reconciliation/report?date=2024-01-15
```

#### Export Reconciliation CSV
```
GET /api/v1/reconciliation/export?date=2024-01-15
```

## Test Data

### Sample API Requests

#### 1. Create and Top Up Wallet
```bash
curl -X POST http://localhost:8080/api/v1/wallets/CUSTOMER001/topup \
  -H "Content-Type: application/json" \
  -d '{"amount": 500.00, "description": "Initial funding"}'
```

#### 2. Check Balance
```bash
curl -X GET http://localhost:8080/api/v1/wallets/CUSTOMER001/balance
```

#### 3. Consume from Wallet
```bash
curl -X POST http://localhost:8080/api/v1/wallets/CUSTOMER001/consume \
  -H "Content-Type: application/json" \
  -d '{"amount": 75.00, "description": "Credit score check"}'
```

#### 4. Multiple Transactions for Testing
```bash
# Customer A
curl -X POST http://localhost:8080/api/v1/wallets/CUST_A/topup -H "Content-Type: application/json" -d '{"amount": 1000.00, "description": "Initial deposit"}'
curl -X POST http://localhost:8080/api/v1/wallets/CUST_A/consume -H "Content-Type: application/json" -d '{"amount": 150.00, "description": "KYC verification"}'
curl -X POST http://localhost:8080/api/v1/wallets/CUST_A/consume -H "Content-Type: application/json" -d '{"amount": 200.00, "description": "Credit scoring"}'

# Customer B
curl -X POST http://localhost:8080/api/v1/wallets/CUST_B/topup -H "Content-Type: application/json" -d '{"amount": 750.00, "description": "Account funding"}'
curl -X POST http://localhost:8080/api/v1/wallets/CUST_B/consume -H "Content-Type: application/json" -d '{"amount": 85.00, "description": "Document verification"}'

# Customer C
curl -X POST http://localhost:8080/api/v1/wallets/CUST_C/topup -H "Content-Type: application/json" -d '{"amount": 300.00, "description": "Top up"}'
curl -X POST http://localhost:8080/api/v1/wallets/CUST_C/consume -H "Content-Type: application/json" -d '{"amount": 120.00, "description": "CRB check"}'
```

### Assumption for External CSV Report Structure and Reconciliation Strategy:

Our reconciliation module assumes that the internal and external transaction IDs may not directly match. Instead, reconciliation is performed by comparing a composite key derived from several fields: `customer_id`, `type`, `transaction_date`, and `amount`.

The external CSV report should adhere to the following structure:

```csv
external_transaction_id,amount,customer_id,type,transaction_date
EXT-TXN-123,100.00,CUST_A,TOPUP,2024-01-15
EXT-TXN-124,50.00,CUST_A,CONSUME,2024-01-15
EXT-TXN-125,75.00,CUST_B,TOPUP,2024-01-15
```

*   `external_transaction_id`: The unique transaction ID from the external system (will not be used for matching but for record-keeping).
*   `amount`: The transaction amount.
*   `customer_id`: The ID of the customer associated with the transaction.
*   `type`: The type of transaction (e.g., `TOPUP`, `CONSUME`).
*   `transaction_date`: The date of the transaction in `YYYY-MM-DD` format.

### Sample External Reconciliation File (external_report.csv)

Example `external_report.csv`:
```csv
external_transaction_id,amount,customer_id,type,transaction_date
EXT-1,100.00,CUST_A,TOPUP,2024-01-15
EXT-2,50.00,CUST_A,CONSUME,2024-01-15
EXT-3,700.00,CUST_B,TOPUP,2024-01-15
EXT-4,999.99,CUST_D,TOPUP,2024-01-15
EXT-5,150.00,CUST_A,TOPUP,2024-01-14
```
*   `EXT-1`: Should match an internal `CUST_A` TOPUP of `100.00` on `2024-01-15`.
*   `EXT-2`: Should match an internal `CUST_A` CONSUME of `50.00` on `2024-01-15`.
*   `EXT-3`: If an internal `CUST_B` TOPUP of `750.00` existed, this would be an `AMOUNT_MISMATCH`.
*   `EXT-4`: This is a `MISSING_INTERNAL` transaction if `CUST_D` has no internal transactions on `2024-01-15`.
*   `EXT-5`: This transaction is for a different date and will not be considered for reconciliation on `2024-01-15`.

#### Upload Reconciliation File
```bash
curl -X POST http://localhost:8080/api/v1/reconciliation/process \
  -F "date=2024-01-15" \
  -F "file=@external_report.csv"
```

#### Get Reconciliation Report
```bash
curl -X GET "http://localhost:8080/api/v1/reconciliation/report?date=2024-01-15"
```

#### Export Reconciliation CSV
```bash
curl -X GET "http://localhost:8080/api/v1/reconciliation/export?date=2024-01-15" -o reconciliation_report_2024-01-15.csv
```

### Expected Response Formats

#### Wallet Balance Response
```json
{
  "customerId": "CUSTOMER001",
  "balance": 425.00
}
```

#### Transaction Response
```json
{
  "transactionId": "TXN-550e8400-e29b-41d4-a716-446655440000",
  "type": "CONSUME",
  "amount": 75.00,
  "description": "Credit score check",
  "status": "COMPLETED"
}
```

#### Reconciliation Report Response
```json
{
  "date": "2024-01-15",
  "summary": {
    "totalTransactions": 5,
    "matched": 3,
    "mismatched": 2,
    "totalAmount": 1000.00,
    "matchedAmount": 850.00,
    "discrepancyAmount": 150.00
  },
  "discrepancies": [
    {
      "reconciliationId": "UUID-OF-DISCREPANCY-1",
      "reconciliationDate": "2024-01-15",
      "internalTransactionId": null,
      "externalTransactionId": "EXT-4",
      "internalAmount": null,
      "externalAmount": 999.99,
      "discrepancyAmount": null,
      "discrepancyReason": "External transaction not found in internal records",
      "status": "MISSING_INTERNAL"
    },
    {
      "reconciliationId": "UUID-OF-DISCREPANCY-2",
      "reconciliationDate": "2024-01-15",
      "internalTransactionId": "INTERNAL-TXN-ID-FOR-CUST-B",
      "externalTransactionId": "EXT-3",
      "internalAmount": 750.00,
      "externalAmount": 700.00,
      "discrepancyAmount": 50.00,
      "discrepancyReason": "Amount mismatch: Internal=750.00, External=700.00",
      "status": "AMOUNT_MISMATCH"
    }
  ]
}
```

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn test -Dtest=*IntegrationTest

# Run all tests with coverage
mvn clean test jacoco:report
```

## Database Schema

### Wallet Table
- `id` (Primary Key)
- `customer_id` (Unique)
- `balance` (Decimal 19,2)
- `version` (Optimistic Locking)
- `created_at`, `updated_at`

### Transaction Table
- `id` (Primary Key)
- `transaction_id` (Unique)
- `wallet_id` (Foreign Key)
- `type` (TOPUP/CONSUME)
- `amount` (Decimal 19,2)
- `description`, `status`, `created_at`

### Reconciliation Record Table
- `id` (Primary Key)
- `reconciliation_date`
- `reconciliation_id`
- `internal_transaction_id`
- `external_transaction_id`
- `internal_amount`, `external_amount`
- `discrepancy_amount`
- `discrepancy_reason`
- `status` (MATCHED/MISSING_INTERNAL/MISSING_EXTERNAL/AMOUNT_MISMATCH)

## Architecture

- **Clean Architecture**: Controllers → Services → Repositories
- **Event-Driven**: RabbitMQ for transaction events
- **Database**: PostgreSQL with optimistic locking
- **Containerization**: Docker & Docker Compose
- **Testing**: Unit tests with Mockito, Integration tests with TestContainers

## Configuration

Key application properties:
- `server.port`: Application port (default: 8080)
- `spring.datasource.*`: Database configuration
- `spring.rabbitmq.*`: RabbitMQ configuration
- `wallet.queues.*`: Queue names

## Monitoring

- Health check: `/actuator/health`
- Metrics: `/actuator/metrics`
- RabbitMQ Management UI: http://localhost:15672

## Assumptions & Limitations

1. Transaction IDs are system-generated UUIDs
2. All amounts are positive (validation enforced)
3. External reconciliation files must be in CSV format
4. Reconciliation is processed synchronously
5. No authentication/authorization implemented
6. No rate limiting implemented
7. File upload size limited to 10MB
8. **Reconciliation Matching Strategy**: Internal and external transaction IDs may not directly match. Reconciliation is performed by comparing a composite key derived from `customer_id`, `type`, `transaction_date`, and `amount`.