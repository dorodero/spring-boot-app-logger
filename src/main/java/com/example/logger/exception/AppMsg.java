package com.example.logger.exception;

import com.example.logger.type.MessageType;

import java.util.Objects;

public final class AppMsg {
    private final MessageType type;
    private final String code;
    private final String message;

    public AppMsg(MessageType type, String code, String message) {
        this.type = type;
        this.code = code;
        this.message = message;
    }

    public MessageType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isWarning() {
        return type == MessageType.Warning;
    }

    public boolean isError() {
        return type == MessageType.Error;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", type, code, message);
    }
}
