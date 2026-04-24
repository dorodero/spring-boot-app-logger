package com.example.logger.aop;

import com.example.logger.AppLogger;
import com.example.logger.config.AppLoggerProperties;
import com.example.logger.exception.AppException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
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
     * ポイントカット定義: Controller
     */
    @Pointcut("execution(* *..*Controller.*(..))")
    public void controllerMethods() {}

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
     * メソッド実行全体をラップ（実行時間計測）
     */
    @Around("controllerMethods()")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.getAop().isEnabled()) {
            return joinPoint.proceed();
        }

        AppLogger logger = getLogger(joinPoint);
        String methodInfo = getMethodInfo(joinPoint);
        long startTime = System.currentTimeMillis();

        // START ログ
        logMethodStart(logger, joinPoint, methodInfo);

        try {
            // メソッド実行
            Object result = joinPoint.proceed();

            // END ログ（正常終了）
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodEnd(logger, joinPoint, methodInfo, result, executionTime);

            return result;

        } catch (Throwable throwable) {
            // ERROR ログ（異常終了）
            long executionTime = System.currentTimeMillis() - startTime;
            logMethodError(logger, joinPoint, methodInfo, throwable, executionTime);
            throw throwable;
        }
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
     * メソッド開始ログ
     */
    private void logMethodStart(AppLogger logger, JoinPoint joinPoint, String methodInfo) {
        if (!properties.getAop().isLogArgs()) {
            logger.info("START: {}", methodInfo);
            return;
        }

        // 引数情報を取得
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            logger.info("START: {}", methodInfo);
            return;
        }

        // Spring MVCアノテーションを考慮した引数ログ
        String argsInfo = formatArguments(joinPoint, args);
        logger.info("START: {} | {}", methodInfo, argsInfo);
    }

    /**
     * メソッド終了ログ（正常）
     */
    private void logMethodEnd(AppLogger logger, JoinPoint joinPoint,
                              String methodInfo, Object result, long executionTime) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("END: ").append(methodInfo);

        if (properties.getAop().isLogExecutionTime()) {
            logMessage.append(" | ").append(executionTime).append("ms");
        }

        if (properties.getAop().isLogResult() && result != null) {
            logMessage.append(" | result: ").append(formatResult(result));
        }

        logger.info(logMessage.toString());
    }

    /**
     * メソッド終了ログ（異常）
     */
    private void logMethodError(AppLogger logger, JoinPoint joinPoint,
                                String methodInfo, Throwable throwable, long executionTime) {

        StringBuilder logMessage = new StringBuilder();
        logMessage.append("ERROR: ").append(methodInfo);

        if (properties.getAop().isLogExecutionTime()) {
            logMessage.append(" | ").append(executionTime).append("ms");
        }

        logMessage.append(" | exception: ").append(throwable.getClass().getSimpleName());

        logger.error(throwable, logMessage.toString());
    }

    /**
     * 引数をフォーマット（Spring MVCアノテーション考慮）
     */
    private String formatArguments(JoinPoint joinPoint, Object[] args) {
        if (!(joinPoint.getSignature() instanceof MethodSignature)) {
            return formatSimpleArgs(args);
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = signature.getParameterNames();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }

            // アノテーションから情報取得
            String paramType = getParameterType(parameterAnnotations[i]);
            String paramName = parameterNames != null && parameterNames.length > i
                    ? parameterNames[i] : "arg" + i;

            if (paramType != null) {
                sb.append(paramType).append("(").append(paramName).append(")");
            } else {
                sb.append(paramName);
            }

            sb.append("=").append(formatValue(args[i]));
        }

        return sb.toString();
    }

    /**
     * パラメータタイプを取得（@RequestBody, @PathVariable等）
     */
    private String getParameterType(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof RequestBody) {
                return "RequestBody";
            } else if (annotation instanceof PathVariable) {
                return "PathVariable";
            } else if (annotation instanceof RequestParam) {
                return "RequestParam";
            }
        }
        return null;
    }

    /**
     * シンプルな引数フォーマット
     */
    private String formatSimpleArgs(Object[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatValue(args[i]));
        }
        return sb.toString();
    }

    /**
     * 値をフォーマット（機密情報マスク対応）
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        String valueStr = value.toString();

        // 機密情報のマスク（例: パスワード）
        if (valueStr.length() > 100) {
            return valueStr.substring(0, 100) + "...(truncated)";
        }

        return valueStr;
    }

    /**
     * 結果をフォーマット
     */
    private String formatResult(Object result) {
        if (result == null) {
            return "null";
        }

        String resultStr = result.toString();

        if (resultStr.length() > 200) {
            return result.getClass().getSimpleName() + "(size=" + resultStr.length() + ")";
        }

        return resultStr;
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