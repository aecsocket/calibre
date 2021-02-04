package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.PaperSystem;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.system.builtin.ProjectileSystem;
import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.calibre.wrapper.user.BukkitItemUser;
import me.aecsocket.calibre.wrapper.user.EntityUser;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.impl.data.ParticleDataStat;
import me.aecsocket.unifiedframework.stat.impl.data.SoundDataStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.NumberDescriptorStat;
import me.aecsocket.unifiedframework.stat.impl.descriptor.Vector2DDescriptorStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.VectorUtils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import me.aecsocket.unifiedframework.util.data.SoundData;
import me.aecsocket.unifiedframework.util.descriptor.NumberDescriptor;
import me.aecsocket.unifiedframework.util.descriptor.Vector2DDescriptor;
import me.aecsocket.unifiedframework.util.projectile.BukkitCollidable;
import me.aecsocket.unifiedframework.util.projectile.BukkitProjectile;
import me.aecsocket.unifiedframework.util.projectile.BukkitRayTrace;
import me.aecsocket.unifiedframework.util.vector.Vector2D;
import me.aecsocket.unifiedframework.util.vector.Vector3D;
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
public class BulletSystem extends AbstractSystem implements ProjectileSystem, PaperSystem {
    public static class Projectile extends BukkitProjectile {
        public enum HitResult {
            STOP,
            RICOCHET,
            CONTINUE
        }

        private final CalibrePlugin plugin;
        private final StatMap stats;
        private ParticleData[] trailParticle;
        private double trailDistance;

        private ParticleData[] hitParticle;
        private SoundData[] hitSound;

        private ParticleData[] hitBlockParticle;
        private SoundData[] hitBlockSound;

        private ParticleData[] hitEntityParticle;
        private SoundData[] hitEntitySound;

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

            trailParticle = stats.val("trail_particle");
            trailDistance = stats.<NumberDescriptor.Double>val("trail_distance").apply();

            hitParticle = stats.val("hit_particle");
            hitSound = stats.val("hit_sound");

            hitBlockParticle = stats.val("hit_block_particle");
            hitBlockSound = stats.val("hit_block_sound");

            hitEntityParticle = stats.val("hit_entity_particle");
            hitEntitySound = stats.val("hit_entity_sound");

            originalDamage = stats.<NumberDescriptor.Double>val("damage").apply();
            damage = originalDamage;
            armorPenetration = stats.<NumberDescriptor.Double>val("armor_penetration").apply();
            blockPenetration = stats.<NumberDescriptor.Double>val("block_penetration").apply();
            entityPenetration = stats.<NumberDescriptor.Double>val("entity_penetration").apply();

            ricochetChance = stats.<NumberDescriptor.Double>val("ricochet_chance").apply();
            ricochetAngle = stats.<NumberDescriptor.Double>val("ricochet_angle").apply() + 90;
            ricochetMultiplier = stats.<NumberDescriptor.Double>val("ricochet_multiplier").apply();

            Vector2D rangeStat = stats.<Vector2DDescriptor>val("range").apply(new Vector2D());
            dropoff = rangeStat.x();
            range = rangeStat.y();
        }

        public CalibrePlugin plugin() { return plugin; }
        public StatMap stats() { return stats; }

        public ParticleData[] trailParticle() { return trailParticle; }
        public double trailDistance() { return trailDistance; }

        public ParticleData[] hitParticle() { return hitParticle; }
        public SoundData[] hitSound() { return hitSound; }

        public ParticleData[] hitBlockParticle() { return hitBlockParticle; }
        public SoundData[] hitBlockSound() { return hitBlockSound; }

        public ParticleData[] hitEntityParticle() { return hitEntityParticle; }
        public SoundData[] hitEntitySound() { return hitEntitySound; }

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

        public double ricochetMultiplier() { return ricochetMultiplier; }
        public void ricochetMultiplier(double ricochetSpeed) { this.ricochetMultiplier = ricochetSpeed; }


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
                ParticleData.spawn(current, trailParticle);
                current.add(step);
            }
        }

        @Override
        protected void collide(TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided) {
            Location location = VectorUtils.toBukkit(ray.position()).toLocation(world);

            ParticleData.spawn(location, hitParticle);
            SoundData.play(() -> location, hitSound);

            HitResult result;
            if (collided.isBlock())
                result = collideBlock(tickContext, ray, location, collided.block());
            else
                result = collideEntity(tickContext, ray, location, collided.entity());

            if (damage <= 0 || result == HitResult.STOP) {
                tickContext.remove();
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
            ParticleData.spawn(location, block.getBlockData(), hitBlockParticle);
            SoundData.play(() -> location, hitBlockSound);
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
                ParticleData.spawn(location, hitEntityParticle);
                SoundData.play(() -> location, hitEntitySound);
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
            .init("damage", NumberDescriptorStat.of(0d))
            .init("armor_penetration", NumberDescriptorStat.of(0d))
            .init("block_penetration", NumberDescriptorStat.of(0d))
            .init("entity_penetration", NumberDescriptorStat.of(0d))

            .init("ricochet_chance", NumberDescriptorStat.of(0d))
            .init("ricochet_angle", NumberDescriptorStat.of(0d))
            .init("ricochet_multiplier", NumberDescriptorStat.of(0d))

            .init("raycast_distance", NumberDescriptorStat.of(0d))
            .init("range", new Vector2DDescriptorStat(new Vector2D()))

            .init("trail_particle", new ParticleDataStat())
            .init("trail_distance", NumberDescriptorStat.of(1d))

            .init("hit_particle", new ParticleDataStat())
            .init("hit_sound", new SoundDataStat())

            .init("hit_block_particle", new ParticleDataStat())
            .init("hit_block_sound", new SoundDataStat())

            .init("hit_entity_particle", new ParticleDataStat())
            .init("hit_entity_sound", new SoundDataStat())
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public BulletSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public BulletSystem() {
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public BulletSystem(BulletSystem o) {
        super(o);
        plugin = o.plugin;
    }

    @Override public CalibrePlugin plugin() { return plugin; }

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

        // raycast the first specified amount metres
        SchedulerLoop loop = plugin.schedulerLoop();
        long period = loop.period();

        double raycastDistance = tree().<NumberDescriptor.Double>stat("raycast_distance").apply();
        while (projectile.travelled() < raycastDistance) {
            TickContext context = loop.context(projectile, period);
            projectile.tick(context);
            if (context.removed())
                return;
        }
        loop.register(projectile);
    }

    @Override public BulletSystem copy() { return new BulletSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
