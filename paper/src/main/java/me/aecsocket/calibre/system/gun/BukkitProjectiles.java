package me.aecsocket.calibre.system.gun;

import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.projectile.BukkitProjectile;
import me.aecsocket.unifiedframework.util.projectile.Projectile;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BukkitProjectiles {
    private BukkitProjectiles() {}

    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("projectile_bounce", NumberDescriptorStat.of(0d))
            .init("projectile_drag", NumberDescriptorStat.of(0d))
            .init("projectile_expansion", NumberDescriptorStat.of(0d))
            .init("projectile_gravity", NumberDescriptorStat.of(Projectile.GRAVITY))
            .get();

    public static void applyTo(BukkitProjectile projectile, StatMap stats) {
        projectile
                .bounce(stats.<NumberDescriptor.Double>val("projectile_bounce").apply())
                .drag(stats.<NumberDescriptor.Double>val("projectile_drag").apply())
                .expansion(stats.<NumberDescriptor.Double>val("projectile_expansion").apply())
                .gravity(stats.<NumberDescriptor.Double>val("projectile_gravity").apply());
    }
}
