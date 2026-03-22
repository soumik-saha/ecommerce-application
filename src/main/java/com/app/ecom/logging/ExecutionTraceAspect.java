package com.app.ecom.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTraceAspect {

    @Around("execution(public * com.app.ecom.controller..*(..)) || " +
            "execution(public * com.app.ecom.service..*(..)) || " +
            "execution(public * com.app.ecom.exception..*(..)) || " +
            "execution(public * com.app.ecom.security.Jwt*.*(..)) || " +
            "execution(public * com.app.ecom.security.CustomUserDetailsService.*(..))")
    public Object traceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long start = System.currentTimeMillis();
        log.debug("<<{}>> starts <<{}>> starts", className, methodName);

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - start;
            log.debug("<<{}>> ends <<{}>> ends ({} ms)", className, methodName, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error("<<{}>> failed <<{}>> failed ({} ms): {}", className, methodName, durationMs, ex.getMessage());
            throw ex;
        }
    }
}

