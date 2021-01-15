package me.aecsocket.calibre.util;

public class ComponentCreationException extends RuntimeException {
    private final String tree;

    public ComponentCreationException(String tree) {
        this.tree = tree;
    }

    public ComponentCreationException(String message, String tree) {
        super(message);
        this.tree = tree;
    }

    public ComponentCreationException(String message, Throwable cause, String tree) {
        super(message, cause);
        this.tree = tree;
    }

    public ComponentCreationException(Throwable cause, String tree) {
        super(cause);
        this.tree = tree;
    }

    public String tree() { return tree; }
}
