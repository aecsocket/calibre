package me.aecsocket.calibre.item.component.descriptor;

import me.aecsocket.unifiedframework.item.ItemCreationException;

public class ComponentCreationException extends ItemCreationException {
    public ComponentCreationException() {}
    public ComponentCreationException(String message) { super(message); }
    public ComponentCreationException(String message, Throwable cause) { super(message, cause); }
    public ComponentCreationException(Throwable cause) { super(cause); }
    public ComponentCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
