package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.world.ItemUser;
import org.bukkit.World;

public interface BukkitItemUser extends ItemUser {
    World world();
}
