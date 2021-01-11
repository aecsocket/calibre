package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.world.SenderUser;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;

public interface EntityUser extends ItemUser, SenderUser {
    Entity entity();

    @Override
    default void sendMessage(Component component) {
        CalibrePlugin.getInstance().getAudiences()
                .sender(entity())
                .sendMessage(component);
    }

    @Override
    default void sendInfo(Component component) {
        CalibrePlugin.getInstance().getAudiences()
                .sender(entity())
                .sendActionBar(component);
    }

    @Override default Vector3D position() { return VectorUtils.toUF(entity().getLocation().toVector()); }
    @Override default Vector3D direction() { return VectorUtils.toUF(entity().getLocation().getDirection()); }
}
