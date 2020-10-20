package me.aecsocket.calibre.item.util.user;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class EntityItemUser implements ItemUser {
    private final Entity entity;

    public EntityItemUser(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() { return entity; }

    @Override public Location getLocation() { return entity.getLocation(); }

    public static EntityItemUser of(Entity entity) {
        if (entity instanceof LivingEntity)
            return new LivingEntityItemUser((LivingEntity) entity);
        else
            return new EntityItemUser(entity);
    }
}
