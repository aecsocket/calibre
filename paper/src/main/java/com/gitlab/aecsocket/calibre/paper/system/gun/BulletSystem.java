package com.gitlab.aecsocket.calibre.paper.system.gun;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.core.system.builtin.ProjectileSystem;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.unifiedframework.paper.loop.SchedulerLoop;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.Vector2DDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector2DDescriptor;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitRayTrace;
import com.gitlab.aecsocket.unifiedframework.core.util.projectile.RayTrace;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@ConfigSerializable
public class BulletSystem extends AbstractSystem implements ProjectileSystem, PaperSystem {
    public static class Projectile extends PaperProjectiles.CalibreProjectile {
        protected final double originalDamage;
        protected double damage;
        protected double armorPenetration;
        protected double blockPenetration;
        protected double entityPenetration;

        protected double ricochetChance;
        protected double ricochetAngle;

        protected double dropoff;
        protected double range;

        public Projectile(CalibrePlugin plugin, World world, Vector3D position, Vector3D velocity, StatMap stats) {
            super(world, position, velocity, plugin, stats);

            originalDamage = stats.<NumberDescriptor.Double>val("damage").apply();
            damage = originalDamage;
            armorPenetration = stats.<NumberDescriptor.Double>val("armor_penetration").apply();
            blockPenetration = stats.<NumberDescriptor.Double>val("block_penetration").apply();
            entityPenetration = stats.<NumberDescriptor.Double>val("entity_penetration").apply();

            ricochetChance = stats.<NumberDescriptor.Double>val("ricochet_chance").apply();
            ricochetAngle = stats.<NumberDescriptor.Double>val("ricochet_angle").apply() + 90;

            Vector2D rangeStat = stats.<Vector2DDescriptor>val("range").apply(new Vector2D());
            dropoff = rangeStat.x();
            range = rangeStat.y();
        }

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

        public double dropoff() { return dropoff; }
        public void dropoff(double dropoff) { this.dropoff = dropoff; }

        public double range() { return range; }
        public void range(double range) { this.range = range; }

        @Override
        protected void step(TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
            super.step(tickContext, ray, from, delta, deltaLength);
            if (travelled() >= range)
                tickContext.remove();
        }

        @Override
        public void bounce(RayTrace<?> ray) {
            super.bounce(ray);
            damage *= bounce;
        }

        protected boolean ricochets(BukkitRayTrace ray) {
            double angle = velocity.angle(ray.collisionNormal());
            if (Math.toDegrees(angle) > ricochetAngle)
                return false;

            double chance = ricochetChance * Utils.square(damage / originalDamage);
            return ThreadLocalRandom.current().nextDouble() < chance;
        }

        protected HitResult collideBlock(TickContext tickContext, BukkitRayTrace ray, Location location, Block block) {
            super.collideBlock(tickContext, ray, location, block);
            double hardness = plugin.hardness(block);
            // if this block is too hard
            if (hardness > blockPenetration) {
                if (ricochets(ray))
                    // ricochets/bounces
                    return HitResult.BOUNCE;
                else
                    // or gets stopped
                    return HitResult.STOP;
            } else {
                // penetrates
                damage -= originalDamage * Utils.clamp01(hardness / Math.max(1, blockPenetration));
                return HitResult.CONTINUE;
            }
        }

        protected HitResult collideEntity(TickContext tickContext, BukkitRayTrace ray, Location location, Entity entity) {
            super.collideEntity(tickContext, ray, location, entity);
            if (entity instanceof Hanging) {
                if (new HangingBreakByEntityEvent((Hanging) entity, source, HangingBreakEvent.RemoveCause.ENTITY).callEvent())
                    entity.remove();
            }

            calculateDamage(entity);
            if (entity instanceof LivingEntity) {
                damage((LivingEntity) entity);
            }

            damage *= entityPenetration;
            // TODO
            return HitResult.STOP;
        }

        protected void calculateDamage(Entity entity) {
            double armor = 0;
            if (entity instanceof Attributable) {
                AttributeInstance attr = ((Attributable) entity).getAttribute(Attribute.GENERIC_ARMOR);
                if (attr != null)
                    armor = attr.getValue();
            }

            damage *= Math.min(1, armorPenetration / Math.max(1, armor));
        }

        protected void damage(LivingEntity entity) {
            double applied = damage * (1 - Utils.clamp01((travelled()-dropoff) / (range-dropoff)));
            plugin.locationalDamageManager().damage(applied, entity, source, position);
        }
    }

    public static final String ID = "bullet";
    public static final Map<String, Stat<?>> DEFAULT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(PaperProjectiles.CALIBRE_STATS)
            .init("damage", NumberDescriptorStat.of(0d))
            .init("armor_penetration", NumberDescriptorStat.of(0d))
            .init("block_penetration", NumberDescriptorStat.of(0d))
            .init("entity_penetration", NumberDescriptorStat.of(0d))

            .init("ricochet_chance", NumberDescriptorStat.of(0d))
            .init("ricochet_angle", NumberDescriptorStat.of(0d))
            .init("ricochet_multiplier", NumberDescriptorStat.of(0d))

            .init("raycast_distance", NumberDescriptorStat.of(0d))
            .init("range", new Vector2DDescriptorStat(new Vector2D()))
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public BulletSystem(CalibrePlugin plugin) {
        super(0);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public BulletSystem() {
        super(0);
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

    @Override public CalibrePlugin calibre() { return plugin; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> defaultStats() { return DEFAULT_STATS; }

    @Override
    public void createProjectile(ItemUser user, Vector3D position, Vector3D velocity) {
        if (!(user instanceof BukkitItemUser))
            return;
        BukkitItemUser bukkitUser = (BukkitItemUser) user;

        Projectile projectile = new Projectile(plugin, bukkitUser.world(), position, velocity, tree().stats());
        PaperProjectiles.applyTo(projectile, tree().stats());
        projectile.source(user instanceof EntityUser ? ((EntityUser) user).entity() : null);

        // raycast the first specified amount metres
        SchedulerLoop loop = plugin.schedulerLoop();
        long period = loop.period();

        double raycastDistance = tree().<NumberDescriptor.Double>stat("raycast_distance").apply();
        long end = System.currentTimeMillis() + 1000;
        while (projectile.travelled() < raycastDistance && System.currentTimeMillis() >= end) {
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
