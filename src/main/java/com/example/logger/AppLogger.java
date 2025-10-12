package com.example.logger;

import com.example.logger.exception.AppMsg;
import com.example.logger.type.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * アプリケーション用ロガー
 * SLF4Jをラップし、独自機能を追加
 */
public class AppLogger {
    private final Logger logger;

    private AppLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * ロガーを取得（クラス指定）
     */
    public static AppLogger getLogger(Class<?> clazz) {
        return new AppLogger(LoggerFactory.getLogger(clazz));
    }

    /**
     * ロガーを取得（名前指定）
     */
    public static AppLogger getLogger(String name) {
        return new AppLogger(LoggerFactory.getLogger(name));
    }

    // ============================================
    // 基本ログメソッド（SLF4Jのパラメータ化ログに対応）
    // ============================================

    /**
     * トレースログを出力
     * @param format
     * @param args
     */
    public void trace(String format, Object... args) {
        logger.trace(format, args);
    }

    /**
     * デバッグログを出力
     * @param format
     * @param args
     */
    public void debug(String format, Object... args) {
        logger.debug(format, args);
    }

    /**
     * インフォメーションログを出力
     * @param format
     * @param args
     */
    public void info(String format, Object... args) {
        logger.info(format, args);
    }

    /**
     * Warningログを出力
     * @param format
     * @param args
     */
    public void warn(String format, Object... args) {
        logger.warn(format, args);
    }

    /**
     * エラーログを出力
     * @param format
     * @param args
     */
    public void error(String format, Object... args) {
        logger.error(format, args);
    }

    /**
     * エラーログ（例外付き）
     */
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    /**
     * エラーログ（パラメータ + 例外）
     */
    public void error(Throwable t, String format, Object... args) {
        // SLF4Jでは最後の引数がThrowableの場合、スタックトレースを出力
        Object[] argsWithThrowable = new Object[args.length + 1];
        System.arraycopy(args, 0, argsWithThrowable, 0, args.length);
        argsWithThrowable[args.length] = t;
        logger.error(format, argsWithThrowable);
    }

    // ============================================
    // AppMsg用のログメソッド
    // ============================================

    /**
     * アプリケーションメッセージをログ出力
     */
    public void msg(AppMsg msg) {
        String logMessage = "[{}] {}";
        // 例外処理専用なので、Warning と Error のみで問題ない
        if (msg.isWarning()) {
            logger.warn(logMessage, msg.getCode(), msg.getMessage());
        } else {
            logger.error(logMessage, msg.getCode(), msg.getMessage());
        }
    }

    /**
     * アプリケーションメッセージをログ出力（例外付き）
     */
    public void msg(AppMsg msg, Throwable t) {
        String logMessage = "[{}] {}";

        // 例外処理専用なので、Warning と Error のみで問題ない
        if (msg.isWarning()) {
            logger.warn(logMessage, msg.getCode(), msg.getMessage(), t);
        } else {
            logger.error(logMessage, msg.getCode(), msg.getMessage(), t);
        }
    }

    // ============================================
    // ログレベル確認メソッド（パフォーマンス最適化用）
    // ============================================

    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }
}
