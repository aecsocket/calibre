package me.aecsocket.calibre.item.blueprint;

import me.aecsocket.unifiedframework.item.ItemCreationException;

public class BlueprintCreationException extends ItemCreationException {
    public BlueprintCreationException() {}
    public BlueprintCreationException(String message) { super(message); }
    public BlueprintCreationException(String message, Throwable cause) { super(message, cause); }
    public BlueprintCreationException(Throwable cause) { super(cause); }
    public BlueprintCreationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) { super(message, cause, enableSuppression, writableStackTrace); }
}
