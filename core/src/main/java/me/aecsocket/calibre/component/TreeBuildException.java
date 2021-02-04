package me.aecsocket.calibre.component;

public class TreeBuildException extends RuntimeException {
    public TreeBuildException() {}
    public TreeBuildException(String message) { super(message); }
    public TreeBuildException(String message, Throwable cause) { super(message, cause); }
    public TreeBuildException(Throwable cause) { super(cause); }
    public TreeBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
