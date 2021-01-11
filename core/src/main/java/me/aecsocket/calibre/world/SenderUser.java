package me.aecsocket.calibre.world;

import net.kyori.adventure.text.Component;

public interface SenderUser extends ItemUser {
    void sendMessage(Component component);
    void sendInfo(Component component);
}
