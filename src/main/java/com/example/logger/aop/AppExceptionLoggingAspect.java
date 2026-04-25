package com.example.logger.aop;

import com.example.logger.AppLogger;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class AppExceptionLoggingAspect {

    private final ConcurrentHashMap<Class<?>, AppLogger> loggerCache = new ConcurrentHashMap<>();
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
        AppLogger logger = getLogger(joinPoint);
        logger.msg(appEx.getAppMsg(), appEx);
    }

    private AppLogger getLogger(JoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return loggerCache.computeIfAbsent(targetClass, AppLogger::getLogger);
    }

    private AppLoggerProperties createDefaultProperties() {
        AppLoggerProperties props = new AppLoggerProperties();
        props.getExceptionLogging().setLogInAop(true);
        return props;
    }
}
