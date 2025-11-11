# Warehouse Service

Asynchronous warehouse service that consumes order messages from RabbitMQ, sent by the Shopping Cart Service.

## Overview

The Warehouse Service is a **fire-and-forget** message consumer that:
- Listens to the `warehouse-orders-queue` in RabbitMQ
- Uses **manual acknowledgements** to ensure message delivery
- Counts total orders and product quantities (thread-safe)
- Runs multithreaded for high throughput
- Prints statistics on shutdown

## Features

- **Manual Acknowledgements**: ACKs message immediately after recording (before processing)
- **Thread-Safe Counters**: Uses `ConcurrentHashMap` and `AtomicInteger` for concurrent operations
- **Multithreaded Consumer**: Configurable concurrent consumers (default: 5-10)
- **Error Handling**: NACKs and requeues messages on validation errors
- **Graceful Shutdown**: Prints statistics summary when service stops
- **Health Checks**: Actuator endpoints for monitoring

## Architecture

```
Shopping Cart Service → RabbitMQ (Queue) → Warehouse Service
                                              ↓
                                        Statistics Counter
                                              ↓
                                        (On Shutdown)
                                              ↓
                                        Print Total Orders
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `RABBITMQ_HOST` | localhost | RabbitMQ hostname |
| `RABBITMQ_PORT` | 5672 | RabbitMQ AMQP port |
| `RABBITMQ_USER` | guest | RabbitMQ username |
| `RABBITMQ_PASS` | guest | RabbitMQ password |
| `QUEUE_NAME` | warehouse-orders-queue | Queue to consume from |
| `CONSUMER_CONCURRENCY` | 5 | Minimum concurrent consumers |
| `CONSUMER_MAX_CONCURRENCY` | 10 | Maximum concurrent consumers |

### Ports

- **8084**: Application port (health checks only, no REST API)

## Running the Service

### Option 1: Docker Compose (Standalone)

Run warehouse service with its own RabbitMQ instance:

```bash
cd services/warehouse-service
docker-compose up --build
```

### Option 2: With All Services (Recommended)

Run all services together from the root directory:

```bash
cd ../../
docker-compose up --build
```

This starts:
- RabbitMQ
- Shopping Cart Service
- Credit Card Authorizer
- Warehouse Service
- Product Services

### Option 3: Local Development (Maven)

```bash
# Start RabbitMQ first
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.12-management-alpine

# Run warehouse service
cd services/warehouse-service
mvn spring-boot:run
```

## Testing

### Run Unit Tests

```bash
cd services/warehouse-service
mvn test
```

### Integration Test Script

Test warehouse service with real messages:

```bash
chmod +x test_warehouse.sh
./test_warehouse.sh
```

This script:
1. Checks warehouse service health
2. Verifies RabbitMQ connection
3. Sends 5 test orders via shopping cart
4. Verifies messages are consumed

### Run All Services Test

From the root directory:

```bash
chmod +x test-all.sh
./test-all.sh
```

## Viewing Statistics

### During Runtime

View real-time logs:

```bash
docker logs -f warehouse-service
```

You'll see messages like:
```
2025-10-28 12:34:56 - Received order from queue: Order ID = 1001, Customer ID = 100, Cart ID = 1, Items = 2
2025-10-28 12:34:56 - Order 1001 acknowledged successfully
```

### On Shutdown

**To see final statistics, gracefully stop the service:**

```bash
docker stop warehouse-service
```

Or press `CTRL+C` if running locally.

You'll see output like:
```
2025-10-28 12:35:00 - Warehouse service shutting down...
2025-10-28 12:35:00 - =====================================
2025-10-28 12:35:00 -    WAREHOUSE STATISTICS SUMMARY
2025-10-28 12:35:00 - =====================================
2025-10-28 12:35:00 - Total Orders Processed: 42
2025-10-28 12:35:00 - Total Unique Products: 15
2025-10-28 12:35:00 - Total Items Quantity: 128
2025-10-28 12:35:00 - =====================================
```

### Check RabbitMQ Management UI

Open browser to: http://localhost:15672
- Username: `guest`
- Password: `guest`

Navigate to **Queues** → `warehouse-orders-queue` to:
- View message rate
- See consumer count
- Browse messages

## Message Format

The warehouse consumes `OrderMessage` objects:

```json
{
  "order_id": 1001,
  "shopping_cart_id": 1,
  "customer_id": 100,
  "items": [
    {
      "productId": 5,
      "quantity": 2
    },
    {
      "productId": 10,
      "quantity": 3
    }
  ],
  "timestamp": "2025-10-28T12:34:56.789Z"
}
```

## Error Handling

### Validation Errors

If a message fails validation (null order_id, invalid items), the service will:
1. Log the error
2. Send **NACK** (negative acknowledgement)
3. **Requeue** the message for retry
4. Print error details to console for debugging

### Processing Errors

If an unexpected error occurs during processing:
1. Log the error with stack trace
2. Send **NACK** and requeue
3. Continue processing other messages

### Network Errors

If the RabbitMQ channel closes:
- Message will be automatically redelivered by RabbitMQ
- Service will attempt to reconnect

## Performance Tuning

### Adjusting Consumer Concurrency

For higher throughput, increase concurrent consumers:

```bash
# In docker-compose.yml or environment
CONSUMER_CONCURRENCY=10
CONSUMER_MAX_CONCURRENCY=20
```

**Guidelines:**
- Start with 5-10 consumers
- Monitor queue size: `curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue`
- If queue is growing, increase consumers
- If CPU is saturated, decrease consumers

### Monitoring Queue Size

```bash
# Check queue size
curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue | grep messages
```

Goal: Keep queue size close to **0** during steady-state operation.

## API Endpoints

### Health Check

```bash
GET http://localhost:8084/actuator/health
```

Response:
```json
{
  "status": "UP"
}
```

### Info Endpoint

```bash
GET http://localhost:8084/actuator/info
```

## Project Structure

```
warehouse-service/
├── src/
│   ├── main/
│   │   ├── java/com/cs6650/group13/warehouse/
│   │   │   ├── WarehouseServiceApplication.java    # Main app + shutdown hook
│   │   │   ├── config/
│   │   │   │   └── RabbitMQConfig.java             # RabbitMQ configuration
│   │   │   ├── consumer/
│   │   │   │   └── OrderMessageConsumer.java       # Message consumer (manual ACK)
│   │   │   ├── model/
│   │   │   │   ├── OrderMessage.java               # Order message model
│   │   │   │   └── CartItem.java                   # Cart item model
│   │   │   └── service/
│   │   │       └── WarehouseStatistics.java        # Thread-safe statistics
│   │   └── resources/
│   │       └── application.properties               # Configuration
│   └── test/
│       └── java/com/cs6650/group13/warehouse/
│           ├── WarehouseServiceApplicationTests.java
│           ├── service/
│           │   └── WarehouseStatisticsTest.java     # Statistics tests
│           └── consumer/
│               └── OrderMessageConsumerTest.java    # Consumer tests
├── Dockerfile                                       # Multi-stage Docker build
├── docker-compose.yml                               # Standalone deployment
├── pom.xml                                          # Maven dependencies
├── test_warehouse.sh                                # Test script
└── README.md                                        # This file
```

## Troubleshooting

### Warehouse not consuming messages

1. Check if warehouse service is running:
   ```bash
   docker ps | grep warehouse
   ```

2. Check warehouse logs:
   ```bash
   docker logs warehouse-service
   ```

3. Verify RabbitMQ connection:
   ```bash
   curl http://localhost:8084/actuator/health
   ```

4. Check if consumers are connected:
   ```bash
   curl -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue
   ```
   Look for `"consumers": X` where X > 0

### Messages stuck in queue

1. Check warehouse logs for errors
2. Increase consumer concurrency
3. Verify messages are valid JSON
4. Check if warehouse service crashed

### Statistics not showing

Make sure to **gracefully stop** the service:
```bash
# Good (triggers shutdown hook)
docker stop warehouse-service

# Bad (kills immediately, no shutdown hook)
docker kill warehouse-service
```

## Development

### Building

```bash
mvn clean package
```

### Running Tests

```bash
mvn test
```

### Building Docker Image

```bash
docker build -t warehouse-service .
```

## Dependencies

- **Spring Boot 3.3.5**
- **Spring AMQP** (RabbitMQ client)
- **Java 17**
- **Jackson** (JSON serialization)
- **JUnit 5** (testing)
- **Mockito** (mocking)

## License

CS6650 Fall 2025, Group 13

