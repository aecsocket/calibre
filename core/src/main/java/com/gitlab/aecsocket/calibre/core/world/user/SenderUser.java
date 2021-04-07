package com.gitlab.aecsocket.calibre.core.world.user;

import net.kyori.adventure.text.Component;

public interface SenderUser extends ItemUser {
    void sendMessage(Component component);
    void sendInfo(Component component);
    void showTitle(Component title, Component subtitle, long fadeIn, long stay, long fadeOut);
}
