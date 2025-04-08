package tech.sonnk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import tech.sonnk.config.AsyncSyncConfiguration;
import tech.sonnk.config.EmbeddedElasticsearch;
import tech.sonnk.config.EmbeddedKafka;
import tech.sonnk.config.EmbeddedRedis;
import tech.sonnk.config.EmbeddedSQL;
import tech.sonnk.config.JacksonConfiguration;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { ProductApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedRedis
@EmbeddedElasticsearch
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
