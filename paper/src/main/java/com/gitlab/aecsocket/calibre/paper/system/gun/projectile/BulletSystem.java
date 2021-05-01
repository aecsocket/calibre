package com.gitlab.aecsocket.calibre.paper.system.gun.projectile;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.Vector2DDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.Vector2DDescriptor;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitRayTrace;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@ConfigSerializable
public final class BulletSystem extends AbstractSystem implements PaperSystem {
    public class ProjectileData {
        public final double originalDamage;
        public double armorPenetration;
        public double blockPenetration;
        public double entityPenetration;

        public double ricochetChance;
        public double ricochetAngle;

        public double dropoff;
        public double range;

        public double damage;

        public ProjectileData(ComponentTree tree) {
            originalDamage = tree.<NumberDescriptor.Double>stat("damage").apply();
            armorPenetration = tree.<NumberDescriptor.Double>stat("armor_penetration").apply();
            blockPenetration = tree.<NumberDescriptor.Double>stat("block_penetration").apply();
            entityPenetration = tree.<NumberDescriptor.Double>stat("entity_penetration").apply();

            ricochetChance = tree.<NumberDescriptor.Double>stat("ricochet_chance").apply();
            ricochetAngle = tree.<NumberDescriptor.Double>stat("ricochet_angle").apply();

            Vector2D rangeStat = tree.<Vector2DDescriptor>stat("range").apply(new Vector2D());
            dropoff = rangeStat.x();
            range = rangeStat.y();

            damage = originalDamage;
        }

        public BulletSystem system() { return BulletSystem.this; }

        public boolean ricochets(PaperProjectileSystem.PaperProjectile projectile, BukkitRayTrace ray) {
            Vector3D flat;
            Vector3D velocity = projectile.velocity();
            switch (ray.face()) {
                case EAST:
                case WEST:
                    flat = velocity.x(0);
                    break;
                case NORTH:
                case SOUTH:
                    flat = velocity.z(0);
                    break;
                case UP:
                case DOWN:
                    flat = velocity.y(0);
                    break;
                default:
                    return false;
            }
            double angle = Math.toDegrees(velocity.angle(flat));
            if (angle > ricochetAngle)
                return false;

            double chance = ricochetChance * Utils.square(damage / originalDamage);
            return ThreadLocalRandom.current().nextDouble() < chance;
        }

        public void damage(PaperProjectileSystem.PaperProjectile projectile, LivingEntity entity) {
            double applied = damage * (1 - Utils.clamp01((projectile.travelled()-dropoff) / (range-dropoff)));
            damage = plugin.locationalDamageManager().damage(applied, entity, projectile.source(), projectile.position(), armorPenetration);
        }
    }

    private static final Map<PaperProjectileSystem.PaperProjectile, ProjectileData> data = new HashMap<>();

    public static final String ID = "bullet";
    public static final int LISTENER_PRIORITY = 0;
    public static final Map<String, Stat<?>> STAT_TYPES = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(PaperProjectileSystem.PAPER_STATS)
            .init("damage", NumberDescriptorStat.of(0d))
            .init("armor_penetration", NumberDescriptorStat.of(0d))
            .init("block_penetration", NumberDescriptorStat.of(0d))
            .init("entity_penetration", NumberDescriptorStat.of(0d))

            .init("ricochet_chance", NumberDescriptorStat.of(0d))
            .init("ricochet_angle", NumberDescriptorStat.of(0d))
            .init("ricochet_multiplier", NumberDescriptorStat.of(0d))

            .init("range", new Vector2DDescriptorStat(new Vector2D()))
            .get();

    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public BulletSystem(CalibrePlugin plugin) {
        super(LISTENER_PRIORITY);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    public BulletSystem() {
        super(LISTENER_PRIORITY);
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
    @Override public Map<String, Stat<?>> statTypes() { return STAT_TYPES; }

    public Map<PaperProjectileSystem.PaperProjectile, ProjectileData> data() { return data; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        require(PaperProjectileSystem.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(PaperProjectileSystem.Events.Create.class, this::onEvent, priority);
        events.registerListener(PaperProjectileSystem.Events.Step.class, this::onEvent, priority);
        events.registerListener(PaperProjectileSystem.Events.Collide.class, this::onEvent, priority);
        events.registerListener(PaperProjectileSystem.Events.Bounce.class, this::onEvent, priority);
        events.registerListener(PaperProjectileSystem.Events.Remove.class, this::onEvent, priority);
    }

    protected void onEvent(PaperProjectileSystem.Events.Create event) {
        if (!event.local())
            return;
        data.put(event.projectile(), new ProjectileData(event.projectile().fullTree));
    }

    protected void onEvent(PaperProjectileSystem.Events.Step event) {
        if (!event.local())
            return;
        PaperProjectileSystem.PaperProjectile projectile = event.projectile();
        ProjectileData data = BulletSystem.data.get(projectile);
        if (projectile.travelled() > data.range) {
            event.taskContext().cancel();
        }
    }

    protected void onEvent(PaperProjectileSystem.Events.Remove event) {
        if (!event.local())
            return;
        data.remove(event.projectile());
    }

    protected void onEvent(PaperProjectileSystem.Events.Collide event) {
        if (!event.local())
            return;
        if (!event.local())
            return;
        PaperProjectileSystem.PaperProjectile projectile = event.projectile();
        ProjectileData data = BulletSystem.data.get(projectile);
        if (event.collided().isBlock()) {
            Block block = event.collided().block();
            double hardness = plugin.hardness(block);
            if (hardness < 0) {
                // pass through the block
                event.result(PaperProjectileSystem.HitResult.CANCEL);
                return;
            }

            // if this block is too hard
            if (hardness > data.blockPenetration) {
                if (data.ricochets(projectile, event.ray())) {
                    // ricochets/bounces
                    event.result(PaperProjectileSystem.HitResult.BOUNCE);
                } else {
                    // or gets stopped
                    event.result(PaperProjectileSystem.HitResult.STOP);
                }
            } else {
                // penetrates
                data.damage -= data.originalDamage * Utils.clamp01(hardness / Math.max(1, data.blockPenetration));
                event.result(PaperProjectileSystem.HitResult.CONTINUE);
            }
        } else {
            Entity entity = event.collided().entity();
            if (entity instanceof Hanging) {
                if (new HangingBreakByEntityEvent((Hanging) entity, projectile.source(), HangingBreakEvent.RemoveCause.ENTITY).callEvent()) {
                    entity.remove();
                }
            }

            if (entity instanceof LivingEntity) {
                data.damage(projectile, (LivingEntity) entity);
            }

            data.damage *= data.entityPenetration;
            // TODO penetration
            event.result(PaperProjectileSystem.HitResult.STOP);
        }
    }

    protected void onEvent(PaperProjectileSystem.Events.Bounce event) {
        if (!event.local())
            return;
        PaperProjectileSystem.PaperProjectile projectile = event.projectile();
        ProjectileData data = BulletSystem.data.get(projectile);
        data.damage *= projectile.bounce();
    }

    @Override public BulletSystem copy() { return new BulletSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }
}
