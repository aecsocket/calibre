package me.aecsocket.calibre.wrapper.user;

import org.bukkit.entity.Player;

public interface PlayerUser extends LivingEntityUser {
    Player entity();

    default String locale() { return entity().getLocale(); }
    default boolean sneaking() { return entity().isSneaking(); }

    static PlayerUser of(Player player) { return () -> player; }
}
