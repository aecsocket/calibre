package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.world.ItemUser;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.calibre.wrapper.user.EntityUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.data.ParticleDataStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.DoubleDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.descriptor.DoubleDescriptor;
import me.aecsocket.unifiedframework.util.projectile.BukkitCollidable;
import me.aecsocket.unifiedframework.util.projectile.BukkitProjectile;
import me.aecsocket.unifiedframework.util.projectile.BukkitRayTrace;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@ConfigSerializable
public class BulletSystem extends AbstractSystem implements ProjectileSystem {
    public static class Projectile extends BukkitProjectile {
        public enum HitResult {
            STOP,
            RICOCHET,
            CONTINUE
        }

        private final CalibrePlugin plugin;
        private final StatMap stats;
        private ParticleData[] trail;
        private double trailDistance;
        private ParticleData[] hitBlock;
        private ParticleData[] hitEntity;

        protected final double originalDamage;
        protected double damage;
        protected double armorPenetration;
        protected double blockPenetration;
        protected double entityPenetration;

        protected double ricochetChance;
        protected double ricochetAngle;
        protected double ricochetMultiplier;

        protected double dropoff;
        protected double range;

        public Projectile(CalibrePlugin plugin, World world, Vector3D position, Vector3D velocity, StatMap stats) {
            super(world, position, velocity);
            this.plugin = plugin;
            this.stats = stats;

            trail = stats.val("trail_particle");
            trailDistance = stats.<DoubleDescriptor>val("trail_distance").apply(0d);
            hitBlock = stats.val("hit_block_particle");
            hitEntity = stats.val("hit_entity_particle");

            originalDamage = stats.<DoubleDescriptor>val("damage").apply(0d);
            damage = originalDamage;
            armorPenetration = stats.<DoubleDescriptor>val("armor_penetration").apply(0d);
            blockPenetration = stats.<DoubleDescriptor>val("block_penetration").apply(0d);

            ricochetChance = stats.<DoubleDescriptor>val("ricochet_chance").apply(0d);
            ricochetAngle = stats.<DoubleDescriptor>val("ricochet_angle").apply(0d);
            ricochetMultiplier = stats.<DoubleDescriptor>val("ricochet_multiplier").apply(0d);

            entityPenetration = stats.<DoubleDescriptor>val("entity_penetration").apply(0d);
            dropoff = stats.<DoubleDescriptor>val("dropoff").apply(0d);
            range = stats.<DoubleDescriptor>val("range").apply(0d);
        }

        public CalibrePlugin plugin() { return plugin; }
        public StatMap stats() { return stats; }

        public ParticleData[] trail() { return trail; }
        public double trailDistance() { return trailDistance; }
        public ParticleData[] hitBlock() { return hitBlock; }
        public ParticleData[] hitEntity() { return hitEntity; }
        public double originalDamage() { return originalDamage; }

        public double damage() { return damage; }
        public void damage(double damage) { this.damage = damage; }

        public double collideEntity() { return damage; }
        public void collideEntity(double damage) { this.damage = damage; }

        public double armorPenetration() { return armorPenetration; }
        public void armorPenetration(double armorPenetration) { this.armorPenetration = armorPenetration; }

        public double blockPenetration() { return blockPenetration; }
        public void blockPenetration(double blockPenetration) { this.blockPenetration = blockPenetration; }

        public double entityPenetration() { return entityPenetration; }
        public void entityPenetration(double entityPenetration) { this.entityPenetration = entityPenetration; }

        public double ricochetChance() { return ricochetChance; }
        public void ricochetChance(double ricochetChance) { this.ricochetChance = ricochetChance; }

        public double ricochetAngle() { return ricochetAngle; }
        public void ricochetAngle(double ricochetAngle) { this.ricochetAngle = ricochetAngle; }

        public double ricochetSpeed() { return ricochetMultiplier; }
        public void ricochetSpeed(double ricochetSpeed) { this.ricochetMultiplier = ricochetSpeed; }

        public double dropoff() { return dropoff; }
        public void dropoff(double dropoff) { this.dropoff = dropoff; }

        public double range() { return range; }
        public void range(double range) { this.range = range; }

        @Override
        protected void step(TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
            super.step(tickContext, ray, from, delta, deltaLength);
            Vector step = VectorUtils.toBukkit(delta.normalize().multiply(trailDistance));
            Location current = VectorUtils.toBukkit(from).toLocation(world);
            for (double d = 0; d < deltaLength; d += trailDistance) {
                ParticleData.spawn(current, trail);
                current.add(step);
            }
        }

        @Override
        protected void collide(TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided) {
            Location location = VectorUtils.toBukkit(ray.position()).toLocation(world);

            HitResult result;
            if (collided.isBlock())
                result = collideBlock(tickContext, ray, location, collided.block());
            else
                result = collideEntity(tickContext, ray, location, collided.entity());

            if (damage <= 0 || result == HitResult.STOP) {
                tickContext.cancel();
                return;
            }

            if (result == HitResult.RICOCHET) {
                velocity = velocity.deflect(ray.collisionNormal()).multiply(ricochetMultiplier);
                damage *= ricochetMultiplier;
            }
        }

        protected boolean ricochets(BukkitRayTrace ray) {
            double angle = velocity.angle(ray.collisionNormal());
            if (Math.toDegrees(angle) > ricochetAngle)
                return false;

            double chance = ricochetChance * Utils.square(damage / originalDamage);
            return ThreadLocalRandom.current().nextDouble() < chance;
        }

        protected HitResult collideBlock(TickContext tickContext, BukkitRayTrace ray, Location location, Block block) {
            ParticleData.spawn(location, block.getBlockData(), hitBlock);
            if (bounce > 0)
                return HitResult.CONTINUE;

            double hardness = plugin.hardness(block);
            // if this block is too hard
            if (hardness > blockPenetration) {
                if (ricochets(ray))
                    // ricochets
                    return HitResult.RICOCHET;
                else
                    // or gets stopped
                    return HitResult.STOP;
            } else
                // penetrates
                damage -= originalDamage * Utils.clamp01(hardness / Math.max(1e-6, blockPenetration));
            return HitResult.CONTINUE;
        }

        protected HitResult collideEntity(TickContext tickContext, BukkitRayTrace ray, Location location, Entity entity) {
            if (entity instanceof LivingEntity) {
                ParticleData.spawn(location, hitEntity);
                damage((LivingEntity) entity);
            } else if (entity instanceof Hanging) {
                if (new HangingBreakByEntityEvent((Hanging) entity, source, HangingBreakEvent.RemoveCause.ENTITY).callEvent())
                    entity.remove();
            }
            // todo
            return HitResult.STOP;
        }

        protected void damage(LivingEntity entity) {
            double entityDamage = damage * (1 - Utils.clamp01((travelled()-dropoff) / (range-dropoff)));
            entity.damage(entityDamage, source);
            entity.setNoDamageTicks(0);
            entity.setVelocity(new Vector());

            if (bounce > 0)
                return;
            damage *= entityPenetration;
        }
    }

    public static final String ID = "bullet";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(BukkitProjectiles.STATS)
            .init("damage", new DoubleDescriptorStat(0).min(0d).max(20d).format("0.0"))
            .init("armor_penetration", new DoubleDescriptorStat(0).min(0d).format("0.0"))
            .init("block_penetration", new DoubleDescriptorStat(1).min(1d).format("0.0"))
            .init("entity_penetration", new DoubleDescriptorStat(0).min(0d).max(1d).format("0.00"))

            .init("ricochet_chance", new DoubleDescriptorStat(0).min(0d).max(1d).format("0.00").hide())
            .init("ricochet_angle", new DoubleDescriptorStat(90).min(90d).max(180d).format("0").hide())
            .init("ricochet_multiplier", new DoubleDescriptorStat(1).min(0d).max(1d).format("0.00").hide())

            .init("dropoff", new DoubleDescriptorStat(0).min(0d).format("0"))
            .init("range", new DoubleDescriptorStat(0).min(0d).format("0"))

            .init("trail_particle", new ParticleDataStat())
            .init("trail_distance", new DoubleDescriptorStat(1d).hide())
            .init("hit_block_particle", new ParticleDataStat())
            .init("hit_entity_particle", new ParticleDataStat())
            .get();

    @FromParent(fromDefaulted = true)
    private transient CalibrePlugin plugin;

    public BulletSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public BulletSystem() {}

    public BulletSystem(BulletSystem o, CalibrePlugin plugin) {
        super(o);
        this.plugin = plugin;
    }

    public BulletSystem(BulletSystem o) {
        this(o, o.plugin);
    }

    public CalibrePlugin plugin() { return plugin; }
    @Override public Component localize(String locale, String key, Object... args) { return plugin.gen(locale, key, args); }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {}

    @Override
    public void createProjectile(ItemUser user, Vector3D position, Vector3D velocity) {
        if (!(user instanceof BukkitItemUser))
            return;
        BukkitItemUser bukkitUser = (BukkitItemUser) user;

        Projectile projectile = new Projectile(plugin, bukkitUser.world(), position, velocity, tree().stats());
        BukkitProjectiles.applyTo(projectile, tree().stats());
        projectile.source(user instanceof EntityUser ? ((EntityUser) user).entity() : null);

        plugin.getSchedulerLoop().tick(projectile);
        plugin.getSchedulerLoop().nextTick(projectile);
    }

    @Override public BulletSystem copy() { return new BulletSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
