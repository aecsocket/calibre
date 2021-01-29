package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.unifiedframework.util.VectorUtils;
import org.bukkit.Location;
import org.bukkit.World;

public interface BukkitItemUser extends ItemUser {
    World world();

    default Location location() { return VectorUtils.toBukkit(position()).toLocation(world()); }
}
