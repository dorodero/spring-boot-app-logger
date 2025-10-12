package com.example.logger.exception;

import com.example.logger.type.MessageType;

public class AppException extends RuntimeException {
    private final AppMsg appMsg;

    public AppException(AppMsg appMsg) {
        super(String.format("[%s] %s", appMsg.getCode(), appMsg.getMessage()));
        this.appMsg = appMsg;
    }

    public AppException(AppMsg appMsg, Throwable cause) {
        super(String.format("[%s] %s", appMsg.getCode(), appMsg.getMessage()), cause);
        this.appMsg = appMsg;
    }

    public AppMsg getAppMsg() {
        return appMsg;
    }

    public boolean isWarning() {
        return appMsg.isWarning();
    }

    public boolean isError() {
        return appMsg.isError();
    }
}
