package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import net.kyori.adventure.text.Component;

import java.util.Locale;

public interface ItemUser {
    Vector3D position();
    Vector3D direction();
    Locale locale();

    default void debug(String msg) {
        if (this instanceof SenderUser)
            ((SenderUser) this).sendMessage(Component.text(msg));
    }
}
