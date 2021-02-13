package me.aecsocket.calibre.system;

import com.google.protobuf.Message;

public class InvalidMessageTypeException extends RuntimeException {
    public InvalidMessageTypeException(Class<? extends Message> expected) {
        super("Expected message of type [" + expected.getName() + "]");
    }
}
