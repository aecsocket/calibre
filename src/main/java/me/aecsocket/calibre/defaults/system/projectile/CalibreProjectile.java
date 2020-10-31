package me.aecsocket.calibre.defaults.system.projectile;

import me.aecsocket.calibre.defaults.service.CalibreDamage;
import me.aecsocket.calibre.defaults.service.CalibrePenetration;
import me.aecsocket.calibre.item.util.damagecause.DamageCause;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.CalibreParticleData;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class CalibreProjectile extends Projectile {
    public static final double MAX_DISTANCE = 1024;

    public static class Data extends ProjectileProviderSystem.Data {
        // TODO Possibly replace all of these with a System, and call #stat on that.
        // Less abstract but way cleaner.
        private CalibreParticleData[] trail;
        private double trailStep;
        private int maxHits;
        private ItemUser damager;
        private double damage;
        private double armorPenetration;
        private DamageCause damageCause;

        public Data(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, CalibreParticleData[] trail, double trailStep, int maxHits, ItemUser damager, double damage, double armorPenetration, DamageCause damageCause) {
            super(location, velocity, bounce, drag, gravity, expansion);
            this.trail = trail;
            this.trailStep = trailStep;
            this.maxHits = maxHits;
            this.damager = damager;
            this.damage = damage;
            this.armorPenetration = armorPenetration;
            this.damageCause = damageCause;
        }

        public Data(ProjectileProviderSystem.Data o) { super(o); }

        public CalibreParticleData[] getTrail() { return trail; }
        public void setTrail(CalibreParticleData[] trail) { this.trail = trail; }

        public double getTrailStep() { return trailStep; }
        public void setTrailStep(double trailStep) { this.trailStep = trailStep; }

        public int getMaxHits() { return maxHits; }
        public void setMaxHits(int maxHits) { this.maxHits = maxHits; }

        public ItemUser getDamager() { return damager; }
        public void setDamager(ItemUser damager) { this.damager = damager; }

        public double getDamage() { return damage; }
        public void setDamage(double damage) { this.damage = damage; }

        public double getArmorPenetration() { return armorPenetration; }
        public void setArmorPenetration(double armorPenetration) { this.armorPenetration = armorPenetration; }

        public DamageCause getDamageCause() { return damageCause; }
        public void setDamageCause(DamageCause damageCause) { this.damageCause = damageCause; }
    }

    private Data data;
    private double damage;

    public CalibreProjectile(Data data) {
        super(
                data.getLocation(),
                data.getVelocity(),
                data.getBounce(),
                data.getDrag(),
                data.getGravity(),
                data.getExpansion()
        );
        this.data = data;
        damage = data.damage;
    }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }

    @Override
    public void tick(TickContext tickContext) {
        if (getTravelled() > MAX_DISTANCE) {
            tickContext.remove();
            return;
        }
        super.tick(tickContext);
    }

    @Override
    protected void step(TickContext tickContext, Vector from, Vector delta, double deltaLength) {
        if (data.trail != null && data.trailStep > 0) {
            double length = delta.length();
            delta.normalize().multiply(data.trailStep);
            Location location = from.toLocation(getLocation().getWorld());
            double distance = 0;
            while (distance < length) {
                ParticleData.spawn(location, data.trail);
                distance += data.trailStep;
                location.add(delta);
            }
        }
        super.step(tickContext, from, delta, deltaLength);
    }

    @Override
    protected void hitEntity(TickContext tickContext, RayTraceResult ray, Entity entity) {
        if (damage > 0)
            Utils.useService(CalibreDamage.Service.class, s -> {
                Utils.useService(CalibrePenetration.Service.class, s2 -> {
                    double mult = s2.armorMultiplier(damage, data.getArmorPenetration(), entity);
                    damage *= mult;
                    getVelocity().multiply(mult);
                });
                s.damage(data.getDamager(), entity, ray.getHitPosition(), damage, data.getDamageCause());
            });
    }

    @Override
    protected void successHit(TickContext tickContext, RayTraceResult ray) {
        if (data.maxHits > 0 && getHits() >= data.maxHits) {
            tickContext.remove();
            return;
        }
        super.successHit(tickContext, ray);
    }
}
