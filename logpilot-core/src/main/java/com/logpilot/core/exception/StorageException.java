package com.logpilot.core.exception;

public class StorageException extends LogPilotException {

    public StorageException(String message) {
        super("STORAGE_ERROR", message);
    }

    public StorageException(String message, Throwable cause) {
        super("STORAGE_ERROR", message, cause);
    }
}
