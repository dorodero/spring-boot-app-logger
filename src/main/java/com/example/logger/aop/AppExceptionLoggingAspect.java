package com.example.logger.aop;

import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import com.example.logger.exception.AppMsg;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class AppExceptionLoggingAspect {

    private final ConcurrentHashMap<Class<?>, Logger> loggerCache = new ConcurrentHashMap<>();
    private final AppLoggerProperties properties;

    public AppExceptionLoggingAspect() {
        this.properties = createDefaultProperties();
    }

    public AppExceptionLoggingAspect(AppLoggerProperties properties) {
        this.properties = properties != null ? properties : createDefaultProperties();
    }

    @AfterThrowing(
            pointcut = "execution(* *..*.*(..))",
            throwing = "ex"
    )
    public void logAppException(JoinPoint joinPoint, Exception ex) {
        if (!properties.getExceptionLogging().isLogInAop()) {
            return;
        }
        if (!(ex instanceof AppException appEx)) {
            return;
        }
        Logger logger = getLogger(joinPoint);
        AppMsg msg = appEx.getAppMsg();
        if (msg.isWarning()) {
            logger.warn("[{}] {}", msg.getCode(), msg.getMessage(), appEx);
        } else {
            logger.error("[{}] {}", msg.getCode(), msg.getMessage(), appEx);
        }
    }

    private Logger getLogger(JoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return loggerCache.computeIfAbsent(targetClass, LoggerFactory::getLogger);
    }

    private AppLoggerProperties createDefaultProperties() {
        AppLoggerProperties props = new AppLoggerProperties();
        props.getExceptionLogging().setLogInAop(true);
        return props;
    }
}
