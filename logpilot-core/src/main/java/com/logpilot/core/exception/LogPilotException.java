package com.logpilot.core.exception;

public abstract class LogPilotException extends RuntimeException {

    private final String errorCode;

    public LogPilotException(String message) {
        super(message);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public LogPilotException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public LogPilotException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LogPilotException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
