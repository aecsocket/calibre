package me.aecsocket.calibre.item.util.user;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class LivingEntityItemUser extends EntityItemUser {
    public LivingEntityItemUser(LivingEntity entity) {
        super(entity);
    }

    @Override public LivingEntity getEntity() { return (LivingEntity) super.getEntity(); }
    @Override public Location getLocation() { return getEntity().getEyeLocation(); }
}
