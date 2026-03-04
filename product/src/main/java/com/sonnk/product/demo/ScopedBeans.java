package com.sonnk.product.demo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Các bean demo scope để quan sát IoC + scope khác nhau.
 */
public class ScopedBeans {

    @Component
    public static class SingletonBean {
        private static final Logger log = LoggerFactory.getLogger(SingletonBean.class);

        private final long createdAt = System.nanoTime();

        @PostConstruct
        void init() {
            log.info("[DEMO-SCOPE] SingletonBean createdAt={}, hash={}", createdAt, System.identityHashCode(this));
        }

        public String info() {
            return "singleton@" + System.identityHashCode(this) + " createdAt=" + createdAt;
        }
    }

    @Component
    @Scope("prototype")
    public static class PrototypeBean {
        private static final Logger log = LoggerFactory.getLogger(PrototypeBean.class);

        private final long createdAt = System.nanoTime();

        @PostConstruct
        void init() {
            log.info("[DEMO-SCOPE] PrototypeBean createdAt={}, hash={}", createdAt, System.identityHashCode(this));
        }

        public String info() {
            return "prototype@" + System.identityHashCode(this) + " createdAt=" + createdAt;
        }
    }

    @Component
    @Scope(WebApplicationContext.SCOPE_REQUEST)
    public static class RequestBean {
        private static final Logger log = LoggerFactory.getLogger(RequestBean.class);

        private final long createdAt = System.nanoTime();

        @PostConstruct
        void init() {
            log.info("[DEMO-SCOPE] RequestBean createdAt={}, hash={}", createdAt, System.identityHashCode(this));
        }

        public String info() {
            return "request@" + System.identityHashCode(this) + " createdAt=" + createdAt;
        }
    }
}

