package me.aecsocket.calibre.util.item;

import me.aecsocket.calibre.proto.Tree;

public class ComponentCreationException extends RuntimeException {
    private final Tree.Component component;

    public ComponentCreationException(String message, Tree.Component component) {
        super(message);
        this.component = component;
    }

    public ComponentCreationException(String message, Throwable cause, Tree.Component component) {
        super(message, cause);
        this.component = component;
    }

    public ComponentCreationException(Throwable cause, Tree.Component component) {
        super(cause);
        this.component = component;
    }

    public Tree.Component component() { return component; }
}
