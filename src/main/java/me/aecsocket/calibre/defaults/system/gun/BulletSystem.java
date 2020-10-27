package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.CalibreDamage;
import me.aecsocket.calibre.defaults.service.CalibrePenetration;
import me.aecsocket.calibre.defaults.system.projectile.CalibreProjectile;
import me.aecsocket.calibre.defaults.system.projectile.ProjectileProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;

import java.util.Collection;
import java.util.Collections;

public class BulletSystem extends BaseSystem implements ProjectileProviderSystem {
    public static class Projectile extends CalibreProjectile {
        public static final double DAMAGE_THRESHOLD = 0.01;

        private FireableSystem.ProjectileData data;
        private double damage;

        public Projectile(FireableSystem.ProjectileData data) {
            super(data);
            this.data = data;
            damage = data.getDamage();
        }

        @Override public FireableSystem.ProjectileData getData() { return data; }

        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }

        public double computeDamage() { return computeDamage(damage, getTravelled(), data.getDropoff(), data.getRange()); }

        @Override
        protected void hitBlock(TickContext tickContext, RayTraceResult ray, Block block) {
            Utils.useService(CalibrePenetration.Service.class, s -> {
                double mult = s.blockMultiplier(damage, data.getBlockPenetration(), block);
                damage *= mult;
                getVelocity().multiply(mult);
                if (damage < DAMAGE_THRESHOLD)
                    tickContext.remove();
            });
        }

        @Override
        protected void hitEntity(TickContext tickContext, RayTraceResult ray, Entity entity) {
            ((PlayerItemUser) data.getDamager()).getEntity().sendMessage("damage " + computeDamage() + " travelled " + getTravelled());
            if (damage > 0)
                Utils.useService(CalibreDamage.Service.class, s ->
                        s.damage(data.getDamager(), entity, ray.getHitPosition(), computeDamage(), data.getDamageCause(), data.getArmorPenetration()));
            Utils.useService(CalibrePenetration.Service.class, s -> {
                double mult = s.entityMultiplier(damage, data.getEntityPenetration(), entity);
                damage *= mult;
                getVelocity().multiply(mult);
                if (damage < DAMAGE_THRESHOLD)
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

    public BulletSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
        ProjectileProviderSystem.super.initialize(parent, tree);
    }

    @Override
    public me.aecsocket.unifiedframework.util.Projectile createProjectile(ProjectileProviderSystem.Data data) {
        if (data instanceof FireableSystem.ProjectileData)
            return new Projectile((FireableSystem.ProjectileData) data);
        if (data instanceof CalibreProjectile.Data)
            return new CalibreProjectile((CalibreProjectile.Data) data);
        return ProjectileProviderSystem.super.createProjectile(data);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public BulletSystem clone() { return (BulletSystem) super.clone(); }
    @Override public BulletSystem copy() { return clone(); }
}
