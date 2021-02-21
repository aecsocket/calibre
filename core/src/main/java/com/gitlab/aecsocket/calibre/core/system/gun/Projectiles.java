package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.projectile.Projectile;

import java.util.HashMap;
import java.util.Map;

public final class Projectiles {
    private Projectiles() {}

    public static final Map<String, Stat<?>> STATS = MapInit.of(new HashMap<String, Stat<?>>())
            .init("projectile_gravity", NumberDescriptorStat.of(Projectile.GRAVITY))
            .get();
}
