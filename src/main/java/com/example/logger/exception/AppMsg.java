package com.example.logger.exception;

import com.example.logger.type.MessageType;
import lombok.Getter;

import java.util.Objects;

/**
 * アプリケーションメッセージ
 *
 * エラーコード、メッセージ、タイプを保持するクラス
 */
@Getter
public final class AppMsg {
    private final MessageType type;
    private final String code;
    private final String message;

    /**
     * コンストラクタ
     *
     * @param type メッセージタイプ
     * @param code メッセージコード
     * @param message メッセージ内容
     */
    public AppMsg(MessageType type, String code, String message) {
        this.type = type;
        this.code = code;
        this.message = message;
    }

    /**
     * Warningタイプかどうか
     */
    public boolean isWarning() {
        return type == MessageType.Warning;
    }

    /**
     * Errorタイプかどうか
     */
    public boolean isError() {
        return type == MessageType.Error;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", type, code, message);
    }
}
