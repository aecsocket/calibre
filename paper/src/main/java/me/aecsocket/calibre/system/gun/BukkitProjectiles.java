package me.aecsocket.calibre.system.gun;

import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.descriptor.DoubleDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.descriptor.DoubleDescriptor;
import me.aecsocket.unifiedframework.util.projectile.BukkitProjectile;
import me.aecsocket.unifiedframework.util.projectile.Projectile;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BukkitProjectiles {
    private BukkitProjectiles() {}

    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("projectile_bounce", new DoubleDescriptorStat(0).min(0d).max(1d).hide())
            .init("projectile_drag", new DoubleDescriptorStat(0).min(0d).max(1d).hide())
            .init("projectile_expansion", new DoubleDescriptorStat(0).min(0d).hide())
            .init("projectile_gravity", new DoubleDescriptorStat(Projectile.GRAVITY).hide())
            .get();

    public static void applyTo(BukkitProjectile projectile, StatMap stats) {
        projectile
                .bounce(stats.<DoubleDescriptor>val("projectile_bounce").apply(0d))
                .drag(stats.<DoubleDescriptor>val("projectile_drag").apply(0d))
                .expansion(stats.<DoubleDescriptor>val("projectile_expansion").apply(0d))
                .gravity(stats.<DoubleDescriptor>val("projectile_gravity").apply(0d));
    }
}
