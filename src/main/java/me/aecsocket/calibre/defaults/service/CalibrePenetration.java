package me.aecsocket.calibre.defaults.service;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

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
            return hardness > 0 ? 1 - Math.min(1, hardness / penetration) : 1;
        }

        @Override public double entityMultiplier(double damage, double penetration, Entity entity) { return penetration; }

        @Override public double armorMultiplier(double damage, double penetration, Entity entity) {
            double armor = entity instanceof LivingEntity ? ((LivingEntity) entity).getAttribute(Attribute.GENERIC_ARMOR).getValue() : 0;
            return armor > 0 ? Utils.clamp01(penetration / armor) : 1;
        }
    }

    private CalibrePenetration() {}
}
