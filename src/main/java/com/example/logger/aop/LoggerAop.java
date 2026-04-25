package com.example.logger.aop;

import com.example.logger.AppLogger;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 高度なログ出力AOP
 *
 * アノテーションベースのログ制御、パフォーマンス計測、
 * リクエストパラメータの詳細ログなどをサポート
 */
@Aspect
@Component
public class LoggerAop {

    private final ConcurrentHashMap<Class<?>, AppLogger> loggerCache = new ConcurrentHashMap<>();
    private final AppLoggerProperties properties;

    public LoggerAop() {
        this.properties = createDefaultProperties();
    }

    public LoggerAop(AppLoggerProperties properties) {
        this.properties = properties != null ? properties : createDefaultProperties();
    }

    /**
     * ポイントカット定義: Service
     */
    @Pointcut("execution(* *..*Service.*(..))")
    public void serviceMethods() {}

    /**
     * ポイントカット定義: Repository
     */
    @Pointcut("execution(* *..*Repository.*(..))")
    public void repositoryMethods() {}

    /**
     * AppException発生時のログ出力
     *
     * ログ出力後、例外は自動的に再スローされ、
     * アプリケーションの@ExceptionHandlerで処理されます。
     */
    @AfterThrowing(
            pointcut = "execution(* *..*.*(..))",
            throwing = "ex"
    )
    public void logAppException(JoinPoint joinPoint, Exception ex) {
        // 設定で無効化されている場合はスキップ
        if (!properties.getExceptionLogging().isLogInAop()) {
            return;
        }

        // AppExceptionのみ処理
        if (!(ex instanceof AppException)) {
            return;
        }

        AppException appEx = (AppException) ex;
        AppLogger logger = getLogger(joinPoint);

        // AppMsgの内容を自動的に適切なログレベルで出力
        logger.msg(appEx.getAppMsg(), appEx);

        // 例外は自動的に再スロー（明示的なthrow不要）
    }

    /**
     * Service層のログ（デバッグレベル）
     */
    @Around("serviceMethods()")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.getAop().isEnabled()) {
            return joinPoint.proceed();
        }

        AppLogger logger = getLogger(joinPoint);

        if (!logger.isDebugEnabled()) {
            return joinPoint.proceed();
        }

        String methodInfo = getMethodInfo(joinPoint);
        long startTime = System.currentTimeMillis();

        logger.debug("START: {}", methodInfo);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("END: {} | {}ms", methodInfo, executionTime);
            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("ERROR: {} | {}ms | {}",
                    methodInfo, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * 対象クラスのLoggerを取得（キャッシュ利用）
     */
    private AppLogger getLogger(JoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return loggerCache.computeIfAbsent(targetClass, AppLogger::getLogger);
    }

    /**
     * メソッド情報を取得
     */
    private String getMethodInfo(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        return className + "." + methodName;
    }

    /**
     * デフォルトプロパティを生成
     */
    private AppLoggerProperties createDefaultProperties() {
        AppLoggerProperties props = new AppLoggerProperties();
        props.getAop().setEnabled(true);
        props.getAop().setLogArgs(true);
        props.getAop().setLogResult(false);
        props.getAop().setLogExecutionTime(true);
        props.getExceptionLogging().setLogInAop(true);
        return props;
    }
}