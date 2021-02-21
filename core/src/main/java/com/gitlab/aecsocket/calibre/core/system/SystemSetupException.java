package com.gitlab.aecsocket.calibre.core.system;

public class SystemSetupException extends RuntimeException {
    public SystemSetupException() {}
    public SystemSetupException(String message) { super(message); }
    public SystemSetupException(String message, Throwable cause) { super(message, cause); }
    public SystemSetupException(Throwable cause) { super(cause); }
    public SystemSetupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
