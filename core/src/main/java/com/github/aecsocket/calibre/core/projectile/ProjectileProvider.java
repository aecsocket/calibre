package com.github.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;

public interface ProjectileProvider extends System.Instance {
    void launchProjectile(ItemUser user, Vector3 origin, Vector3 velocity);
}
