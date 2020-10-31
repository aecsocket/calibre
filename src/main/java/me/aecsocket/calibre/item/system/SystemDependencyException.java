package me.aecsocket.calibre.item.system;

public class SystemDependencyException extends SystemInitializationException {
    public SystemDependencyException(Class<? extends CalibreSystem> type) {
        super("Missing system service " + type);
    }
}
