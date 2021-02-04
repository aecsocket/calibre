package me.aecsocket.calibre.system.gun;

import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.projectile.Projectile;

import java.util.HashMap;
import java.util.Map;

public final class Projectiles {
    private Projectiles() {}

    public static final Map<String, Stat<?>> STATS = MapInit.of(new HashMap<String, Stat<?>>())
            .init("projectile_gravity", NumberDescriptorStat.of(Projectile.GRAVITY))
            .get();
}
