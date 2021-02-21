package com.gitlab.aecsocket.calibre.paper.wrapper.user;

import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import org.bukkit.Location;
import org.bukkit.World;

public interface BukkitItemUser extends ItemUser {
    World world();

    default Location location() { return VectorUtils.toBukkit(position()).toLocation(world()); }
}
