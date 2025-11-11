# Load Testing Client for Shopping Cart Service

A comprehensive multi-threaded load testing client for testing the Shopping Cart Service with RabbitMQ queue monitoring.

## Features

- **Two Test Modes**:
  - **Full Workflow**: Creates cart → Adds item → Checks out (tests entire flow)
  - **Mass Checkout**: Pre-creates carts with items, then performs mass checkout (focuses on RabbitMQ throughput)

- **Real-Time Monitoring**:
  - Requests per second (RPS)
  - Success/failure counts
  - RabbitMQ queue depth
  - RabbitMQ publish/consume/ack rates
  - Progress percentage

- **Configurable Parameters**:
  - Number of threads
  - Total requests (default: 200,000)
  - Delay between requests (for throttling)
  - Test mode
  - Endpoints (local/AWS)

- **Test Data**:
  - 10% requests use invalid credit cards (guaranteed failure)
  - 90% requests use valid credit cards (~90% approval from CCA)
  - 1 item per cart
  - 10 different product IDs

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Running services:
  - Shopping Cart Service (default: localhost:8083)
  - RabbitMQ with Management Plugin (default: localhost:15672)
  - Credit Card Authorizer (as dependency of Shopping Cart)
  - Warehouse Service (to consume messages)

## Building the Client

Navigate to the client directory and build:

```bash
cd client
mvn clean package
```

This creates an executable JAR: `target/load-test-client-1.0.0.jar`

## Configuration

### Option 1: Using Configuration Files

Edit `config/local.properties` for local testing:

```properties
shopping.cart.url=http://localhost:8083
rabbitmq.mgmt.url=http://localhost:15672
rabbitmq.user=guest
rabbitmq.pass=guest
test.threads=10
test.total.requests=200000
test.delay.ms=0
test.mode=MASS_CHECKOUT
```

### Option 2: Using Command Line Arguments

Override settings with command line flags (see Usage section).

## Running Tests Locally

### Step 1: Start All Services

From the project root directory:

```bash
cd ..
docker-compose up -d
```

Verify all services are running:

```bash
docker ps
```

You should see:
- `rabbitmq`
- `credit-card-authorizer`
- `shopping-cart-service`
- `warehouse-service`

### Step 2: Verify Services are Healthy

Check Shopping Cart Service:
```bash
curl http://localhost:8083/actuator/health
```

Check Warehouse Service:
```bash
curl http://localhost:8084/actuator/health
```

Check RabbitMQ Management UI:
```bash
open http://localhost:15672
# Login: guest / guest
```

### Step 3: Run the Load Test

Navigate to the client directory:

```bash
cd client
```

#### Test 1: Small Test (Verify Setup)

Run a small test first to verify everything works:

```bash
java -jar target/load-test-client-1.0.0.jar \
  --threads 5 \
  --requests 100 \
  --mode MASS_CHECKOUT
```

Expected output:
- Real-time metrics updating every second
- Queue size increases then decreases
- Success rate around 80-90%
- Some "Payment Declined" and "Invalid Card Format" failures

#### Test 2: Full 200K Test

Once verified, run the full test:

```bash
java -jar target/load-test-client-1.0.0.jar \
  --threads 10 \
  --requests 200000 \
  --mode MASS_CHECKOUT
```

#### Test 3: Full Workflow Test

Test the complete flow (slower, tests entire system):

```bash
java -jar target/load-test-client-1.0.0.jar \
  --threads 10 \
  --requests 10000 \
  --mode FULL_WORKFLOW
```

### Step 4: Monitor RabbitMQ

Open the RabbitMQ Management UI:
```bash
open http://localhost:15672
```

Navigate to **Queues** → **warehouse-orders-queue** to see:
- Queue depth graph
- Message rates (in/out)
- Consumer count

### Step 5: Find Optimal Configuration

#### Test Different Thread Counts

```bash
# Test with 5 threads
java -jar target/load-test-client-1.0.0.jar --threads 5 --requests 50000

# Test with 10 threads
java -jar target/load-test-client-1.0.0.jar --threads 10 --requests 50000

# Test with 20 threads
java -jar target/load-test-client-1.0.0.jar --threads 20 --requests 50000

# Test with 50 threads
java -jar target/load-test-client-1.0.0.jar --threads 50 --requests 50000
```

#### Adjust Warehouse Consumers

Edit `docker-compose.yml` to change warehouse consumer count:

```yaml
warehouse-service:
  environment:
    CONSUMER_CONCURRENCY: 10      # Change this
    CONSUMER_MAX_CONCURRENCY: 20  # And this
```

Restart warehouse service:
```bash
docker-compose restart warehouse-service
```

#### Goal: Balance Production and Consumption

Watch the RabbitMQ queue size in real-time. Ideal configuration:
- Queue size stays below 1,000 messages
- Queue size plateaus (doesn't keep growing): `/¯¯¯\`
- Production rate ≈ Consumption rate

#### Example Test Matrix

| Client Threads | Warehouse Consumers | Queue Behavior | Notes |
|----------------|---------------------|----------------|-------|
| 10 | 5 | Growing /\ | Underprovisioned |
| 10 | 10 | Plateau /¯\ | Good balance |
| 20 | 10 | Growing /\ | Underprovisioned |
| 20 | 20 | Plateau /¯\ | Good balance |
| 50 | 20 | Growing /\ | Underprovisioned |
| 50 | 50 | Plateau /¯\ | Good balance |

### Step 6: Clean Up Between Tests

The Shopping Cart Service stores carts in memory. To clean up:

#### Option 1: Restart Services

```bash
docker-compose restart shopping-cart-service warehouse-service
```

#### Option 2: Full Restart

```bash
docker-compose down
docker-compose up -d
```

Wait for services to be ready (~15 seconds).

## Command Line Options

```
Usage: load-test-client [OPTIONS]

Options:
  -c, --config <file>     Configuration file path
  -t, --threads <num>     Number of threads (default: 10)
  -n, --requests <num>    Total requests (default: 200000)
  -d, --delay <ms>        Delay between requests in ms (default: 0)
  -m, --mode <mode>       Test mode: FULL_WORKFLOW or MASS_CHECKOUT (default: MASS_CHECKOUT)
  -u, --url <url>         Shopping cart service URL (default: http://localhost:8083)
  -h, --help              Show help
```

### Examples

**Using configuration file:**
```bash
java -jar target/load-test-client-1.0.0.jar --config config/local.properties
```

**Override specific settings:**
```bash
java -jar target/load-test-client-1.0.0.jar \
  --config config/local.properties \
  --threads 20 \
  --requests 100000
```

**Quick test:**
```bash
java -jar target/load-test-client-1.0.0.jar -t 5 -n 1000
```

**With throttling (10ms delay between requests):**
```bash
java -jar target/load-test-client-1.0.0.jar -t 10 -n 10000 -d 10
```

## Understanding the Output

### Real-Time Metrics

```
[  5.23%] Requests:  10,450 / 200,000 | RPS:  156 | Success:   9,405 | Failed:  1,045 | Queue:     342 msgs | Pub:  158.2/s | Consume:  155.7/s | Ack:  155.7/s
```

- **[5.23%]**: Progress percentage
- **Requests**: Total requests sent / Target requests
- **RPS**: Requests per second (last second)
- **Success**: Total successful checkouts
- **Failed**: Total failed checkouts
- **Queue**: Current messages in RabbitMQ queue
- **Pub**: RabbitMQ publish rate (messages/sec)
- **Consume**: RabbitMQ consume rate (messages/sec)
- **Ack**: RabbitMQ acknowledgement rate (messages/sec)

### Final Summary

```
================================================================================
LOAD TEST SUMMARY
================================================================================
Total Requests:        200,000
Successful:            179,856 (89.93%)
Failed:                20,144 (10.07%)

Total Time:            127.45 seconds
Average Throughput:    1,569.15 requests/sec

Response Time (avg):   6.37 ms
Response Time (min):   2 ms
Response Time (max):   523 ms

Failure Breakdown:
  Invalid Card Format            : 20,000
  Payment Declined (CCA)         : 144
================================================================================
```

### Expected Failure Rate

- **~10% total failures** (intentional test design)
  - ~10% from invalid card format (every 10th request)
  - ~1% from CCA payment decline (10% of valid cards)

## Troubleshooting

### Problem: "Connection refused" to Shopping Cart Service

**Solution:**
```bash
# Check if service is running
docker ps | grep shopping-cart-service

# Check service health
curl http://localhost:8083/actuator/health

# Check logs
docker logs shopping-cart-service

# Restart if needed
docker-compose restart shopping-cart-service
```

### Problem: "Connection refused" to RabbitMQ Management

**Solution:**
```bash
# Check if RabbitMQ is running
docker ps | grep rabbitmq

# Check if management plugin is enabled
curl http://localhost:15672

# Restart if needed
docker-compose restart rabbitmq
```

### Problem: Queue keeps growing (never decreases)

**Solution:**
```bash
# Check warehouse service is running and consuming
docker logs warehouse-service

# Look for "Received order from queue" messages
# If no messages, warehouse might be down or misconfigured

# Restart warehouse
docker-compose restart warehouse-service
```

### Problem: Very low throughput (< 100 RPS)

**Possible causes:**
1. **Network latency**: Running on slow network
2. **CPU contention**: Too many threads, not enough CPU
3. **Service overload**: Services can't keep up

**Solutions:**
- Reduce thread count
- Check CPU usage: `docker stats`
- Check service logs for errors

### Problem: "OutOfMemoryError" in Shopping Cart Service

**Cause:** Too many carts in memory (200K carts = ~200MB+)

**Solution:**
```bash
# Restart shopping cart service
docker-compose restart shopping-cart-service

# Or allocate more memory in docker-compose.yml:
# environment:
#   JAVA_OPTS: "-Xmx1g"
```

## Tips for Optimal Performance

### 1. Start Small, Scale Up
- Begin with 1,000 requests to verify setup
- Then 10,000 to get baseline metrics
- Finally 200,000 for full test

### 2. Monitor System Resources
```bash
# Watch Docker container resources
docker stats

# Watch system resources
top  # or htop
```

### 3. Tune RabbitMQ Consumers
- Start with `CONSUMER_CONCURRENCY=5`
- If queue grows, increase to 10, 20, etc.
- Watch RabbitMQ Management UI for optimal count

### 4. Find Thread Sweet Spot
- More threads ≠ better performance
- Too many threads cause CPU thrashing
- Typical optimal range: 10-50 threads

### 5. Use MASS_CHECKOUT Mode
- Faster for RabbitMQ testing
- Pre-creates all carts first
- Then focuses on checkout throughput

### 6. Save Your Results
```bash
# Redirect output to file
java -jar target/load-test-client-1.0.0.jar -t 20 -n 200000 | tee results-20threads.txt

# Check detailed logs
tail -f load-test.log
```

## Performance Benchmarks

Typical results on local machine (M1 Mac, 8 cores):

| Threads | Requests | Time | Throughput | Queue Max | Consumers |
|---------|----------|------|------------|-----------|-----------|
| 10 | 200,000 | ~130s | ~1,540 RPS | 850 | 10 |
| 20 | 200,000 | ~95s | ~2,100 RPS | 1,200 | 20 |
| 50 | 200,000 | ~75s | ~2,660 RPS | 2,500 | 50 |

Your results will vary based on hardware and network.

## Project Structure

```
client/
├── config/
│   ├── local.properties          # Local test configuration
│   └── aws.properties             # AWS test configuration (template)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/cs6650/group13/client/
│       │       ├── LoadTestClient.java       # Main client
│       │       ├── api/
│       │       │   └── ShoppingCartClient.java  # API client
│       │       ├── config/
│       │       │   └── LoadTestConfig.java   # Configuration
│       │       ├── metrics/
│       │       │   └── TestMetrics.java      # Metrics tracking
│       │       └── monitor/
│       │           └── RabbitMQMonitor.java  # RabbitMQ monitoring
│       └── resources/
│           └── logback.xml                   # Logging configuration
├── pom.xml                                   # Maven configuration
└── README.md                                 # This file
```

## Next Steps

1. **Run local tests** following this guide
2. **Find optimal configuration** (threads vs consumers)
3. **Document your findings** (queue behavior, throughput)
4. **Deploy to AWS** (when ready)
5. **Re-run tests on AWS** with higher scale

## Support

For issues or questions:
1. Check the Troubleshooting section
2. Review Docker logs: `docker-compose logs`
3. Check RabbitMQ Management UI
4. Review `load-test.log` file

## License

CS6650 Fall 2025, Group 13

