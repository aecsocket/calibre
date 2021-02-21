package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;

public interface ProjectileSystem extends CalibreSystem {
    void createProjectile(ItemUser user, Vector3D position, Vector3D velocity);
}
