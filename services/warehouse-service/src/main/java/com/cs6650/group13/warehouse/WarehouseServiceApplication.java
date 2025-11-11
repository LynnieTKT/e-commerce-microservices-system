package com.cs6650.group13.warehouse;

import com.cs6650.group13.warehouse.service.WarehouseStatistics;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WarehouseServiceApplication {

	private static final Logger logger = LoggerFactory.getLogger(WarehouseServiceApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(WarehouseServiceApplication.class, args);
	}

	/**
	 * Register shutdown hook to print statistics when application stops
	 * This will be called when:
	 * - CTRL+C (SIGINT)
	 * - docker stop (SIGTERM)
	 * - Application shutdown
	 */
	@Bean
	public ShutdownHook shutdownHook(WarehouseStatistics statistics) {
		return new ShutdownHook(statistics);
	}

	/**
	 * Shutdown hook to print warehouse statistics
	 */
	static class ShutdownHook {
		private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
		private final WarehouseStatistics statistics;

		public ShutdownHook(WarehouseStatistics statistics) {
			this.statistics = statistics;
			// Register JVM shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(this::onShutdown));
			logger.info("Shutdown hook registered");
		}

		@PreDestroy
		public void onShutdown() {
			logger.info("Warehouse service shutting down...");
			statistics.printStatistics();
		}
	}
}


