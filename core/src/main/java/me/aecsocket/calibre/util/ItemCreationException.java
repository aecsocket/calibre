package me.aecsocket.calibre.util;

public class ItemCreationException extends RuntimeException {
    public ItemCreationException() {}
    public ItemCreationException(String message) { super(message); }
    public ItemCreationException(String message, Throwable cause) { super(message, cause); }
    public ItemCreationException(Throwable cause) { super(cause); }
    public ItemCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
