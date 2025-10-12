package com.example.logger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AppLoggerの設定プロパティ
 *
 * application.ymlで設定可能:
 * <pre>
 * app:
 *   logger:
 *     aop:
 *       enabled: true
 *       pointcut: "execution(* com.myapp.controller.*.*(..))"
 *       log-args: true
 *       log-result: false
 *     message:
 *       enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "app.logger")
public class AppLoggerProperties {
    /**
     * AOP設定
     */
    private Aop aop = new Aop();

    /**
     * AppException発生時のロギング設定
     */
    private ExceptionLogging exceptionLogging = new ExceptionLogging();

    /**
     * メッセージ設定
     */
    private Message message = new Message();

    public Aop getAop() {
        return aop;
    }

    public void setAop(Aop aop) {
        this.aop = aop;
    }

    public ExceptionLogging getExceptionLogging() {
        return exceptionLogging;
    }

    public void setExceptionLogging(ExceptionLogging exceptionLogging) {
        this.exceptionLogging = exceptionLogging;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    /**
     * AOP設定
     */
    public static class Aop {
        /**
         * AOPログを有効にするか
         */
        private boolean enabled = true;

        /**
         * カスタムポイントカット式
         */
        private String pointcut = "execution(* *..*Controller.*(..))";

        /**
         * メソッド引数をログ出力するか
         */
        private boolean logArgs = true;

        /**
         * メソッドの戻り値をログ出力するか
         */
        private boolean logResult = false;

        /**
         * 実行時間をログ出力するか
         */
        private boolean logExecutionTime = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPointcut() {
            return pointcut;
        }

        public void setPointcut(String pointcut) {
            this.pointcut = pointcut;
        }

        public boolean isLogArgs() {
            return logArgs;
        }

        public void setLogArgs(boolean logArgs) {
            this.logArgs = logArgs;
        }

        public boolean isLogResult() {
            return logResult;
        }

        public void setLogResult(boolean logResult) {
            this.logResult = logResult;
        }

        public boolean isLogExecutionTime() {
            return logExecutionTime;
        }

        public void setLogExecutionTime(boolean logExecutionTime) {
            this.logExecutionTime = logExecutionTime;
        }
    }

    /**
     * メッセージ設定
     */
    public static class Message {
        /**
         * メッセージサービスを有効にするか
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ExceptionLogging {
        private boolean logInAop = true;

        public boolean isLogInAop() {
            return logInAop;
        }

        public void setLogInAop(boolean logInAop) {
            this.logInAop = logInAop;
        }
    }
}
