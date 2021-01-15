package me.aecsocket.calibre.wrapper.user;

import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import org.bukkit.entity.LivingEntity;

public interface LivingEntityUser extends EntityUser {
    LivingEntity entity();

    @Override default Vector3D position() { return VectorUtils.toUF(entity().getEyeLocation().toVector()); }

    static LivingEntityUser of(LivingEntity entity) { return () -> entity; }
}
