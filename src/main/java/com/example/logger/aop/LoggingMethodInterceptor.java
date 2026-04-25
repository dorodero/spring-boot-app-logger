package com.example.logger.aop;

import com.example.logger.AppLogger;
import com.example.logger.config.AppLoggerProperties;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.concurrent.ConcurrentHashMap;

public class LoggingMethodInterceptor implements MethodInterceptor {

    private final ConcurrentHashMap<Class<?>, AppLogger> loggerCache = new ConcurrentHashMap<>();
    private final AppLoggerProperties properties;

    public LoggingMethodInterceptor(AppLoggerProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (!properties.getAop().isEnabled()) {
            return invocation.proceed();
        }

        AppLogger logger = getLogger(invocation.getThis().getClass());
        String methodInfo = getMethodInfo(invocation);
        long startTime = System.currentTimeMillis();

        logMethodStart(logger, invocation, methodInfo);

        try {
            Object result = invocation.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodEnd(logger, methodInfo, result, executionTime);
            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodError(logger, methodInfo, throwable, executionTime);
            throw throwable;
        }
    }

    private void logMethodStart(AppLogger logger, MethodInvocation invocation, String methodInfo) {
        if (!properties.getAop().isLogArgs()) {
            logger.info("START: {}", methodInfo);
            return;
        }

        Object[] args = invocation.getArguments();
        if (args == null || args.length == 0) {
            logger.info("START: {}", methodInfo);
            return;
        }

        String argsInfo = formatArguments(
                invocation.getMethod().getParameters(),
                invocation.getMethod().getParameterAnnotations(),
                args);
        logger.info("START: {} | {}", methodInfo, argsInfo);
    }

    private void logMethodEnd(AppLogger logger, String methodInfo, Object result, long executionTime) {
        StringBuilder msg = new StringBuilder("END: ").append(methodInfo);
        if (properties.getAop().isLogExecutionTime()) {
            msg.append(" | ").append(executionTime).append("ms");
        }
        if (properties.getAop().isLogResult() && result != null) {
            msg.append(" | result: ").append(formatResult(result));
        }
        logger.info(msg.toString());
    }

    private void logMethodError(AppLogger logger, String methodInfo, Throwable throwable, long executionTime) {
        StringBuilder msg = new StringBuilder("ERROR: ").append(methodInfo);
        if (properties.getAop().isLogExecutionTime()) {
            msg.append(" | ").append(executionTime).append("ms");
        }
        msg.append(" | exception: ").append(throwable.getClass().getSimpleName());
        logger.error(msg.toString(), throwable);
    }

    private String formatArguments(Parameter[] parameters, Annotation[][] paramAnnotations, Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            String paramType = getParameterType(paramAnnotations[i]);
            String paramName = parameters[i].getName();
            if (paramType != null) {
                sb.append(paramType).append("(").append(paramName).append(")");
            } else {
                sb.append(paramName);
            }
            sb.append("=").append(formatValue(args[i]));
        }
        return sb.toString();
    }

    private String getParameterType(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestBody) return "RequestBody";
            if (annotation instanceof PathVariable) return "PathVariable";
            if (annotation instanceof RequestParam) return "RequestParam";
        }
        return null;
    }

    private String formatValue(Object value) {
        if (value == null) return "null";
        String s = value.toString();
        return s.length() > 100 ? s.substring(0, 100) + "...(truncated)" : s;
    }

    private String formatResult(Object result) {
        if (result == null) return "null";
        String s = result.toString();
        return s.length() > 200 ? result.getClass().getSimpleName() + "(size=" + s.length() + ")" : s;
    }

    private AppLogger getLogger(Class<?> targetClass) {
        return loggerCache.computeIfAbsent(targetClass, AppLogger::getLogger);
    }

    private String getMethodInfo(MethodInvocation invocation) {
        String className = invocation.getThis().getClass().getSimpleName();
        String methodName = invocation.getMethod().getName();
        return className + "." + methodName;
    }
}
