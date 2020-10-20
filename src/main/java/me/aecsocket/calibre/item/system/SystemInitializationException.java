package me.aecsocket.calibre.item.system;

public class SystemInitializationException extends RuntimeException {
    public SystemInitializationException() {}
    public SystemInitializationException(String message) { super(message); }
    public SystemInitializationException(String message, Throwable cause) { super(message, cause); }
    public SystemInitializationException(Throwable cause) { super(cause); }
    public SystemInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
