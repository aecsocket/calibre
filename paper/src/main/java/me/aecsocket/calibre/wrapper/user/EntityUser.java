package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.world.user.SenderUser;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public interface EntityUser extends ItemUser, SenderUser, BukkitItemUser {
    Entity entity();

    @Override
    default World world() { return entity().getWorld(); }

    @Override
    default void sendMessage(Component component) {
        CalibrePlugin.getInstance().audiences()
                .sender(entity())
                .sendMessage(component);
    }

    @Override
    default void sendInfo(Component component) {
        CalibrePlugin.getInstance().audiences()
                .sender(entity())
                .sendActionBar(component);
    }

    @Override
    default String locale() { return CalibrePlugin.getInstance().getDefaultLocale(); }

    @Override default Vector3D position() { return VectorUtils.toUF(entity().getLocation().toVector()); }
    @Override default Vector3D direction() { return VectorUtils.toUF(entity().getLocation().getDirection()); }

    static EntityUser of(Entity entity) { return () -> entity; }
}
