# CS6650 Group 13 - Testing Clients

This directory contains two separate testing clients for the CS6650 e-commerce system.

## Directory Structure

```
client/
├── loadTester/          # Load testing client for shopping cart service
├── productTester/       # Product service testing client for load balancer demo
└── README.md           # This file
```

## Load Tester

Tests the shopping cart service with various load patterns and monitors RabbitMQ queue metrics.

**Key Features:**
- Shopping cart workflow testing
- RabbitMQ monitoring
- Mass checkout simulation
- Real-time metrics

**Quick Start:**
```bash
cd loadTester
mvn clean package
java -jar target/load-test-client-1.0.0.jar -t 10 -n 200000 -m MASS_CHECKOUT --config config/local.properties
```

[See loadTester/README.md for detailed documentation](loadTester/README.md)

## Product Tester

Tests product service endpoints to demonstrate AWS Application Load Balancer behavior with good and bad instances.

**Key Features:**
- Test individual or multiple services
- Load balancer testing support
- Success rate analysis
- Status code breakdown

**Quick Start:**
```bash
cd productTester
mvn clean package
java -jar target/product-tester-1.0.0.jar -t 10 -n 1000 --config config/local.properties
```

[See productTester/README.md for detailed documentation](productTester/README.md)

## Building Both Projects

From the `client` directory:

```bash
# Build load tester
cd loadTester && mvn clean package && cd ..

# Build product tester
cd productTester && mvn clean package && cd ..
```

## Common Requirements

- Java 17 or higher
- Maven 3.6+
- Running services (local or AWS)

## Configuration

Both testers support:
- **Local mode**: Test against localhost services
- **AWS mode**: Test against AWS-deployed services
- **Command-line overrides**: Customize via CLI arguments

## Testing Workflow

### Local Testing

1. Start all services using docker-compose:
   ```bash
   cd ../
   docker-compose up -d
   ```

2. Start product services:
   ```bash
   cd services/product-service && mvn spring-boot:run &
   cd ../product-service-bad && mvn spring-boot:run &
   ```

3. Run load tester:
   ```bash
   cd client/loadTester
   ./run-load-test.sh 10 10000 MASS_CHECKOUT
   ```

4. Run product tester:
   ```bash
   cd ../productTester
   ./run-product-test.sh 10 1000
   ```

### AWS Testing

1. Update configuration files with your AWS endpoints:
   - `loadTester/config/aws.properties`
   - `productTester/config/aws.properties`

2. Run tests:
   ```bash
   # Load tester
   cd loadTester
   java -jar target/load-test-client-1.0.0.jar --config config/aws.properties
   
   # Product tester
   cd ../productTester
   java -jar target/product-tester-1.0.0.jar --config config/aws.properties
   ```

## Project Separation

These two testers are completely independent:
- **Separate dependencies**: Each has its own `pom.xml`
- **Separate configurations**: Different config files and parameters
- **Separate packages**: Different Java package structures
- **Separate JARs**: Build and run independently

This separation allows you to:
- Deploy them independently
- Update one without affecting the other
- Run them on different machines
- Use different Java versions if needed

## Support

For issues or questions:
1. Check individual README files in each tester directory
2. Review configuration examples
3. Ensure all prerequisites are met
4. Verify services are running before testing

## License

CS6650 Group 13 Project
