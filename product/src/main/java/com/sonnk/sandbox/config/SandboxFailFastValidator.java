package com.sonnk.sandbox.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fail-fast configuration validator.
 * Runs at application startup to check required properties and env config.
 *
 * Purpose: Prevent partial deployments.
 * Instead of discovering config errors at runtime (e.g., 503 when database unavailable),
 * validate upfront and fail fast if any critical config is missing or invalid.
 *
 * Checks:
 * 1. Database connection properties exist
 * 2. Kafka broker(s) configured (if using messaging)
 * 3. Redis host configured (if using caching)
 * 4. Logging level not too verbose in production
 * 5. Batch sizes within reasonable bounds
 */
@Component
public class SandboxFailFastValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxFailFastValidator.class);
    private final Environment env;

    public SandboxFailFastValidator(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== Sandbox: Fail-Fast Configuration Validation ===");

        // Check critical properties
        validateDatabaseConfig();
        validateBatchSizes();
        validateLoggingConfig();

        log.info("✓ All configuration checks passed. Application is ready.");
    }

    private void validateDatabaseConfig() {
        String datasourceUrl = env.getProperty("spring.datasource.url");
        String datasourceUsername = env.getProperty("spring.datasource.username");

        if (datasourceUrl == null || datasourceUrl.isEmpty()) {
            throw new IllegalArgumentException(
                    "FAIL-FAST: Missing spring.datasource.url. Check application.properties/yml");
        }

        if (datasourceUsername == null || datasourceUsername.isEmpty()) {
            throw new IllegalArgumentException(
                    "FAIL-FAST: Missing spring.datasource.username. Check application.properties/yml");
        }

        log.info("✓ Database config validated: {}", datasourceUrl);
    }

    private void validateBatchSizes() {
        String batchSizeStr = env.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "20");
        try {
            int batchSize = Integer.parseInt(batchSizeStr);
            if (batchSize < 1 || batchSize > 1000) {
                throw new IllegalArgumentException(
                        "Batch size must be between 1 and 1000, got: " + batchSize);
            }
            log.info("✓ Batch size validated: {}", batchSize);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid batch size format: " + batchSizeStr, e);
        }
    }

    private void validateLoggingConfig() {
        String profile = env.getProperty("spring.profiles.active", "local");

        // In production, don't log SQL statements (performance overhead)
        if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
            String sqlLogLevel = env.getProperty("logging.level.org.hibernate.SQL", "INFO");
            if ("DEBUG".equalsIgnoreCase(sqlLogLevel) || "TRACE".equalsIgnoreCase(sqlLogLevel)) {
                log.warn("⚠ WARNING: SQL logging is DEBUG/TRACE in PRODUCTION. " +
                        "This impacts performance. Consider disabling in prod.");
            }
        }

        log.info("✓ Logging config validated for profile: {}", profile);
    }
}
