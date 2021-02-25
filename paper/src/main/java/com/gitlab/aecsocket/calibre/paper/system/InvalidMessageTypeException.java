package com.gitlab.aecsocket.calibre.paper.system;

import com.google.protobuf.Message;

public class InvalidMessageTypeException extends RuntimeException {
    public InvalidMessageTypeException(Class<? extends Message> expected, String received) {
        super("Expected message of type [" + expected.getName() + "], received [" + received + "]");
    }
}
