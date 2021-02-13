package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.world.user.SenderUser;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public interface EntityUser extends ItemUser, SenderUser, BukkitItemUser {
    Entity entity();

    @Override
    default World world() { return entity().getWorld(); }

    @Override
    default void sendMessage(Component component) {
        CalibrePlugin.instance().audiences()
                .sender(entity())
                .sendMessage(component);
    }

    @Override
    default void sendInfo(Component component) {
        CalibrePlugin.instance().audiences()
                .sender(entity())
                .sendActionBar(component);
    }

    @Override
    default String locale() { return CalibrePlugin.instance().getDefaultLocale(); }

    @Override default Vector3D position() { return VectorUtils.toUF(entity().getLocation().toVector()); }
    @Override default Vector3D direction() { return VectorUtils.toUF(entity().getLocation().getDirection()); }

    static EntityUser of(Entity entity) { return () -> entity; }

    static EntityUser autoOf(Entity entity) {
        return entity instanceof Player
                ? PlayerUser.of((Player) entity)
                : entity instanceof LivingEntity
                ? LivingEntityUser.of((LivingEntity) entity)
                : of(entity);
    }
}
