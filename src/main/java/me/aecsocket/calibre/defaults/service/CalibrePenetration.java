package me.aecsocket.calibre.defaults.service;

import me.aecsocket.calibre.CalibrePlugin;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public final class CalibrePenetration {
    public interface Service {
        double blockMultiplier(double damage, double penetration, Block block);
        double entityMultiplier(double damage, double penetration, Entity entity);
        double armorMultiplier(double damage, double penetration, Entity entity);
    }

    public static class Provider implements Service {
        private final CalibrePlugin plugin;

        public Provider(CalibrePlugin plugin) {
            this.plugin = plugin;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        @Override
        public double blockMultiplier(double damage, double penetration, Block block) {
            float hardness = plugin.setting("service.penetration.hardness." + block.getType().name(), float.class, block.getType().getBlastResistance());
            return hardness > 0 ? Math.min(1, penetration / hardness) : 1;
        }

        @Override public double entityMultiplier(double damage, double penetration, Entity entity) { return penetration; }

        @Override public double armorMultiplier(double damage, double penetration, Entity entity) { return penetration; }
    }

    private CalibrePenetration() {}
}
