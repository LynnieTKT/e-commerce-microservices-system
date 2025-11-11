# Product Service Tester

A comprehensive testing client for the Product Service to demonstrate AWS Application Load Balancer behavior with "good" and "bad" service instances.

## Overview

This tester sends HTTP POST requests to the Product Service `/product` endpoint to:
- Test service availability and response rates
- Demonstrate load balancer routing behavior
- Compare performance between "good" (100% success) and "bad" (50% error rate) instances
- Verify that the load balancer correctly routes less traffic to failing instances

## Features

- **Multi-threaded testing** - Concurrent request execution
- **Flexible targeting** - Test individual services or through load balancer
- **Comprehensive metrics** - Success rates, response times, status code breakdown
- **Command-line configurable** - Override settings via CLI arguments
- **Local and AWS support** - Test locally or against AWS deployments

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Running Product Service instances (local or AWS)

## Quick Start

### 1. Build the Project

```bash
cd productTester
mvn clean package
```

### 2. Run Local Test

```bash
# Using the run script (easiest)
./run-product-test.sh 10 1000 config/local.properties

# Or directly with java
java -jar target/product-tester-1.0.0.jar -c config/local.properties -t 10 -n 1000
```

### 3. Run AWS Test

```bash
# Test through load balancer
java -jar target/product-tester-1.0.0.jar -c config/aws.properties -t 20 -n 10000
```

## Configuration Files

### Local Configuration (`config/local.properties`)

```properties
# Good product service (100% success rate)
product.service.good.url=http://localhost:8080/api

# Bad product service (50% error rate)
product.service.bad.url=http://localhost:8081/api

# Test Parameters
test.threads=10
test.total.requests=1000
test.delay.ms=0

# Target: GOOD, BAD, BOTH
test.target=BOTH
```

### AWS Configuration (`config/aws.properties`)

```properties
# AWS Application Load Balancer URL
product.service.load.balancer.url=http://YOUR-ALB-URL/api

# Direct instance URLs (optional)
product.service.good.url=http://YOUR-GOOD-INSTANCE-IP:8080/api
product.service.bad.url=http://YOUR-BAD-INSTANCE-IP:8081/api

# Test Parameters
test.threads=20
test.total.requests=10000
test.delay.ms=0

# Target: LOAD_BALANCER
test.target=LOAD_BALANCER
```

## Command Line Arguments

| Flag | Long Form | Description | Default |
|------|-----------|-------------|---------|
| `-c` | `--config` | Configuration file path | None (uses defaults) |
| `-t` | `--threads` | Number of concurrent threads | 10 |
| `-n` | `--requests` | Total number of requests | 1000 |
| `-d` | `--delay` | Delay between requests (ms) | 0 |
| N/A | `--target` | Test target (GOOD/BAD/BOTH/LOAD_BALANCER) | BOTH |
| `-h` | `--help` | Show help message | N/A |

## Usage Examples

### Example 1: Test Both Local Services

```bash
java -jar target/product-tester-1.0.0.jar -t 10 -n 1000
```

This will split the load 50/50 between good and bad services.

### Example 2: Test Only Good Service

```bash
java -jar target/product-tester-1.0.0.jar -t 10 -n 1000 --target GOOD
```

### Example 3: Test Only Bad Service

```bash
java -jar target/product-tester-1.0.0.jar -t 10 -n 1000 --target BAD
```

### Example 4: Test Through Load Balancer (AWS)

```bash
java -jar target/product-tester-1.0.0.jar -c config/aws.properties -t 20 -n 10000
```

### Example 5: High-Load Test

```bash
java -jar target/product-tester-1.0.0.jar -t 50 -n 50000 -c config/local.properties
```

## Test Output

The test displays a final summary with:

```
================================================================================
PRODUCT TEST SUMMARY
================================================================================
Total Requests:        10,000
Successful (201):      7,500 (75.00%)
Failed:                2,500 (25.00%)

Total Time:            45.23 seconds
Average Throughput:    221.09 requests/sec

Response Time (avg):   45.23 ms
Response Time (min):   12 ms
Response Time (max):   234 ms

Status Code Breakdown:
  201 Created (Success)      :   7,500 (75.00%)
  503 Service Unavailable    :   2,500 (25.00%)
================================================================================
```

## Understanding the Results

### Testing BOTH Services Locally

When `test.target=BOTH`, the tester splits load between good and bad services:
- **Good service**: Returns 201 (100% success)
- **Bad service**: Returns 201 or 503 (50% each)
- **Expected overall success rate**: ~75% (50% from good + 25% from bad)

### Testing Load Balancer (AWS)

When `test.target=LOAD_BALANCER`, all requests go through the AWS ALB:
- The load balancer should route less traffic to the bad instance
- You can verify this by checking AWS CloudWatch metrics
- Expected success rate should be > 75% if load balancer is working correctly

## Starting Product Services Locally

### Good Product Service (Port 8080)

```bash
cd ../../services/product-service
mvn spring-boot:run
```

### Bad Product Service (Port 8081)

```bash
cd ../../services/product-service-bad
mvn spring-boot:run
```

## Troubleshooting

### Connection Refused

**Problem**: `Connection refused` error
**Solution**: Ensure product services are running on the configured ports

```bash
# Check if services are running
curl http://localhost:8080/api/health
curl http://localhost:8081/api/health
```

### Build Failures

**Problem**: Maven build fails
**Solution**: Ensure Java 17+ is installed

```bash
java -version
mvn -version
```

### AWS Load Balancer Testing

**Problem**: Load balancer URL not working
**Solution**: 
1. Update `config/aws.properties` with your actual ALB URL
2. Ensure ALB is configured to route `/api/product` requests
3. Verify security groups allow traffic on port 80/443

## Project Structure

```
productTester/
├── config/
│   ├── local.properties      # Local testing configuration
│   └── aws.properties         # AWS testing configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── com/cs6650/group13/producttester/
│       │       ├── ProductTester.java       # Main test client
│       │       ├── api/
│       │       │   └── ProductClient.java   # HTTP client
│       │       ├── config/
│       │       │   └── ProductTesterConfig.java
│       │       ├── metrics/
│       │       │   └── ProductTestMetrics.java
│       │       └── model/
│       │           └── Product.java
│       └── resources/
│           └── logback.xml
├── pom.xml
├── run-product-test.sh        # Quick start script
└── README.md                  # This file
```

## Dependencies

- Apache HttpClient 5.2.1 - HTTP communication
- Gson 2.10.1 - JSON processing
- Commons CLI 1.5.0 - Command line parsing
- SLF4J + Logback - Logging
- JUnit 5.10.0 - Testing framework

## License

CS6650 Group 13 Project

