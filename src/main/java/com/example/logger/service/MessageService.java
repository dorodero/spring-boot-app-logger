package com.example.logger.service;

import com.example.logger.exception.AppMsg;
import com.example.logger.type.MessageType;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * メッセージ取得サービス
 *
 * MessageSourceを使用してメッセージを取得し、AppMsgを生成します。
 */
@Service
public class MessageService {
    private final MessageSource messageSource;

    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * メッセージを取得してAppMsgを生成
     *
     * @param type メッセージタイプ（Warning, Error等）
     * @param code メッセージコード
     * @return AppMsg
     */
    public AppMsg getMsg(MessageType type, String code) {
        String message = getMessage(code);
        return new AppMsg(type, code, message);
    }

    /**
     * メッセージを取得してAppMsgを生成（パラメータ付き）
     *
     * @param type メッセージタイプ
     * @param code メッセージコード
     * @param args メッセージパラメータ
     * @return AppMsg
     */
    public AppMsg getMsg(MessageType type, String code, Object... args) {
        String message = getMessage(code, args);
        return new AppMsg(type, code, message);
    }

    /**
     * メッセージを取得してAppMsgを生成（ロケール指定）
     *
     * @param type メッセージタイプ
     * @param code メッセージコード
     * @param locale ロケール
     * @param args メッセージパラメータ
     * @return AppMsg
     */
    public AppMsg getMsg(MessageType type, String code, Locale locale, Object... args) {
        String message = getMessage(code, args, locale);
        return new AppMsg(type, code, message);
    }

    /**
     * Warningタイプのメッセージを取得
     *
     * @param code メッセージコード
     * @param args メッセージパラメータ
     * @return AppMsg
     */
    public AppMsg getWarning(String code, Object... args) {
        return getMsg(MessageType.Warning, code, args);
    }

    /**
     * Errorタイプのメッセージを取得
     *
     * @param code メッセージコード
     * @param args メッセージパラメータ
     * @return AppMsg
     */
    public AppMsg getError(String code, Object... args) {
        return getMsg(MessageType.Error, code, args);
    }

    // ============================================
    // 内部ヘルパーメソッド
    // ============================================

    /**
     * メッセージを取得（現在のロケール）
     */
    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * メッセージを取得（パラメータ付き、現在のロケール）
     */
    private String getMessage(String code, Object[] args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * メッセージを取得（ロケール指定）
     */
    private String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }
}
