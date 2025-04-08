package tech.sonnk.config;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class MariadbTestContainer implements SqlTestContainer {

    private static final Logger LOG = LoggerFactory.getLogger(MariadbTestContainer.class);

    private MariaDBContainer<?> mariaDBContainer;

    @Override
    public void destroy() {
        if (null != mariaDBContainer && mariaDBContainer.isRunning()) {
            mariaDBContainer.stop();
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (null == mariaDBContainer) {
            mariaDBContainer = new MariaDBContainer<>("mariadb:11.7.2")
                .withDatabaseName("ProductApp")
                .withTmpFs(Collections.singletonMap("/testtmpfs", "rw"))
                .withLogConsumer(new Slf4jLogConsumer(LOG))
                .withReuse(true);
        }
        if (!mariaDBContainer.isRunning()) {
            mariaDBContainer.start();
        }
    }

    @Override
    public JdbcDatabaseContainer<?> getTestContainer() {
        return mariaDBContainer;
    }
}
