package com.sonnk.product.demo;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DemoLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(DemoLoggingAspect.class);

    @Around("@annotation(com.sonnk.product.demo.DemoLogged)")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[DEMO-AOP] {}.{} took {} ms",
                    pjp.getSignature().getDeclaringTypeName(),
                    pjp.getSignature().getName(),
                    elapsed);
        }
    }
}

