package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibrePenetration;
import me.aecsocket.calibre.defaults.system.projectile.CalibreProjectile;
import me.aecsocket.calibre.defaults.system.projectile.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.TextUtils;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Collection;
import java.util.Collections;

public class BulletSystem extends BaseSystem implements ProjectileProviderSystem {
    public static class Projectile extends CalibreProjectile {
        public static final double DAMAGE_THRESHOLD = 0.01;

        private GunSystem.ProjectileData data;

        public Projectile(GunSystem.ProjectileData data) {
            super(data);
            this.data = data;
        }

        @Override public GunSystem.ProjectileData getData() { return data; }

        public double computeDamage() { return computeDamage(getDamage(), getTravelled(), data.getDropoff(), data.getRange()); }

        @Override
        protected void hitBlock(TickContext tickContext, RayTraceResult ray, Block block) {
            Utils.useService(CalibrePenetration.Service.class, s -> {
                double mult = s.blockMultiplier(getDamage(), data.getBlockPenetration(), block);
                setDamage(getDamage() * mult);
                getVelocity().multiply(mult);
                if (getDamage() < DAMAGE_THRESHOLD)
                    tickContext.remove();
            });
            if (data.getBreakableBlocks() != null && data.getBreakableBlocks().contains(block.getType())) {
                if (data.getDamager() instanceof PlayerItemUser) {
                    if (!new BlockBreakEvent(block, ((PlayerItemUser) data.getDamager()).getEntity()).callEvent())
                        return;
                }
                block.breakNaturally(new ItemStack(Material.AIR), true);
            }
        }

        @Override
        protected void hitEntity(TickContext tickContext, RayTraceResult ray, Entity entity) {
            double oldDamage = getDamage();
            setDamage(computeDamage());
            super.hitEntity(tickContext, ray, entity);
            setDamage(oldDamage);

            Utils.useService(CalibrePenetration.Service.class, s -> {
                double mult = s.entityMultiplier(getDamage(), data.getEntityPenetration(), entity);
                setDamage(getDamage() * mult);
                getVelocity().multiply(mult);
                if (getDamage() < DAMAGE_THRESHOLD)
                    tickContext.remove();
            });
        }

        @Override
        protected void successHit(TickContext tickContext, RayTraceResult ray) {
            if (getData().getMaxHits() > 0 && getHits() >= getData().getMaxHits()) {
                tickContext.remove();
                return;
            }
            if (getBounce() > 0)
                Utils.reflect(getVelocity(), ray.getHitBlockFace()).multiply(getBounce());
        }

        public static double computeDamage(double damage, double travelled, double dropoff, double range) {
            double percent = Utils.clamp01((travelled - dropoff) / (range - dropoff));
            return damage * (1 - percent);
        }
    }

    public static final String ID = "bullet";

    @LoadTimeOnly private String prefix;
    @LoadTimeOnly private String icon;

    public BulletSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
        ProjectileProviderSystem.super.initialize(parent, tree);

        prefix = prefix == null ? "" : TextUtils.translateColor(prefix);
        icon = icon == null ? "" : TextUtils.translateColor(icon);
    }

    @Override
    public me.aecsocket.unifiedframework.util.Projectile createProjectile(ProjectileProviderSystem.Data data) {
        if (data instanceof GunSystem.ProjectileData)
            return new Projectile((GunSystem.ProjectileData) data);
        if (data instanceof CalibreProjectile.Data)
            return new CalibreProjectile((CalibreProjectile.Data) data);
        return ProjectileProviderSystem.super.createProjectile(data);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public BulletSystem clone() { return (BulletSystem) super.clone(); }
    @Override public BulletSystem copy() { return clone(); }
}
