package me.aecsocket.calibre.system.builtin;

import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.unifiedframework.util.vector.Vector3D;

public interface ProjectileSystem extends CalibreSystem {
    void createProjectile(ItemUser user, Vector3D position, Vector3D velocity);
}
