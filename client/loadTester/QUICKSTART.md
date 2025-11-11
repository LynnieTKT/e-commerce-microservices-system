# Quick Start Guide - Load Testing

## 1. Build the Client

```bash
cd client
mvn clean package
```

## 2. Start All Services

```bash
cd ..
docker-compose up -d
```

Wait 15 seconds for services to start.

## 3. Verify Services

```bash
# Check all services
docker ps

# Should show: rabbitmq, credit-card-authorizer, shopping-cart-service, warehouse-service

# Quick health check
curl http://localhost:8083/actuator/health  # Shopping cart
curl http://localhost:8084/actuator/health  # Warehouse
curl http://localhost:15672                 # RabbitMQ UI
```

## 4. Run a Small Test First

```bash
cd client
java -jar target/load-test-client-1.0.0.jar --threads 5 --requests 100
```

**Expected:**
- Test completes in ~1 second
- ~90 successes, ~10 failures
- Queue size increases then decreases to 0

## 5. Run Full 200K Test

```bash
java -jar target/load-test-client-1.0.0.jar --threads 10 --requests 200000
```

## 6. Monitor RabbitMQ

Open: http://localhost:15672 (guest/guest)

Navigate to: Queues → warehouse-orders-queue

Watch:
- Queue depth (should stay low)
- Publish rate
- Consume rate

## 7. Find Optimal Configuration

### Test Different Thread Counts

```bash
# 5 threads
java -jar target/load-test-client-1.0.0.jar -t 5 -n 50000

# 10 threads
java -jar target/load-test-client-1.0.0.jar -t 10 -n 50000

# 20 threads
java -jar target/load-test-client-1.0.0.jar -t 20 -n 50000

# 50 threads
java -jar target/load-test-client-1.0.0.jar -t 50 -n 50000
```

### Adjust Warehouse Consumers

Edit `../docker-compose.yml`:

```yaml
warehouse-service:
  environment:
    CONSUMER_CONCURRENCY: 20      # Change this value
    CONSUMER_MAX_CONCURRENCY: 40  # And this
```

Restart:
```bash
docker-compose restart warehouse-service
```

### Goal

Find the configuration where:
- Queue stays below 1,000 messages
- Queue plateaus (doesn't keep growing)
- Production rate ≈ Consumption rate

## 8. Clean Up Between Tests

```bash
# Quick restart
docker-compose restart shopping-cart-service warehouse-service

# Or full restart
docker-compose down
docker-compose up -d
```

## Common Commands

### Quick Tests

```bash
# Use the convenience script
chmod +x run-test.sh

# 10 threads, 10K requests
./run-test.sh 10 10000

# 20 threads, 50K requests
./run-test.sh 20 50000

# 50 threads, 200K requests, full workflow mode
./run-test.sh 50 200000 FULL_WORKFLOW
```

### Manual Tests

```bash
# Mass checkout (default, fastest)
java -jar target/load-test-client-1.0.0.jar -t 20 -n 100000 -m MASS_CHECKOUT

# Full workflow (slower, tests complete flow)
java -jar target/load-test-client-1.0.0.jar -t 10 -n 10000 -m FULL_WORKFLOW

# With throttling (10ms delay per request)
java -jar target/load-test-client-1.0.0.jar -t 10 -n 10000 -d 10

# Using config file
java -jar target/load-test-client-1.0.0.jar -c config/local.properties
```

### Monitoring

```bash
# Watch Docker resources
docker stats

# View service logs
docker logs -f shopping-cart-service
docker logs -f warehouse-service

# Check RabbitMQ queue
curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue | jq .
```

## Expected Results

**Small Test (100 requests):**
- Time: ~1 second
- Throughput: ~100 RPS
- Success rate: ~90%
- Queue max: < 50 messages

**Full Test (200K requests, 10 threads):**
- Time: ~120-150 seconds
- Throughput: ~1,300-1,700 RPS
- Success rate: ~90%
- Queue max: < 1,000 messages (with proper consumer count)

## Troubleshooting

**Problem:** Connection refused to localhost:8083

**Solution:**
```bash
docker ps | grep shopping-cart-service
docker logs shopping-cart-service
docker-compose restart shopping-cart-service
```

**Problem:** Queue keeps growing, never shrinks

**Solution:**
```bash
docker logs warehouse-service
# Check for "Received order from queue" messages
# If missing, restart warehouse:
docker-compose restart warehouse-service
```

**Problem:** Very low throughput (< 100 RPS)

**Solution:**
- Check CPU: `docker stats`
- Reduce thread count
- Check for errors in logs

## Next Steps

1. ✅ Run small test to verify setup
2. ✅ Run full 200K test
3. ✅ Experiment with thread counts (5, 10, 20, 50)
4. ✅ Adjust warehouse consumers to balance queue
5. ✅ Document optimal configuration
6. 📝 Save results for comparison

## Tips

- Start small (100 requests) to verify
- Monitor RabbitMQ UI during tests
- Try both test modes (MASS_CHECKOUT and FULL_WORKFLOW)
- Save output: `java -jar ... | tee results.txt`
- Check detailed logs: `tail -f load-test.log`

For detailed documentation, see [README.md](README.md)

