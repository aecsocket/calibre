package me.aecsocket.calibre.defaults.service.system;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.bukkit.damage.CalibreDamageService;
import me.aecsocket.calibre.defaults.service.bukkit.raytrace.CalibreRaytraceService;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import me.aecsocket.unifiedframework.util.data.ParticleData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * A system which can create a projectile with specific conditions.
 */
public interface ProjectileProviderSystem {
    /**
     * Creates a projectile according to a data class.
     * @param data The event.
     * @return The projectile.
     */
    me.aecsocket.unifiedframework.util.Projectile create(Data data);

    /**
     * Represents projectile creation data.
     */
    class Data {
        private final Location location;
        private final Vector velocity;
        private final double bounce;
        private final double drag;
        private final double gravity;
        private final double expansion;

        private final ParticleData[] trail;
        private final double trailStepSize;
        private final ItemUser shooter;
        private final double damage;
        private final ItemStack item;

        public Data(Location location, Vector velocity, double bounce, double drag, double gravity, double expansion, ParticleData[] trail, double trailStepSize, ItemUser shooter, double damage, ItemStack item) {
            this.location = location;
            this.velocity = velocity;
            this.bounce = bounce;
            this.drag = drag;
            this.gravity = gravity;
            this.expansion = expansion;
            this.trail = trail;
            this.trailStepSize = trailStepSize;
            this.shooter = shooter;
            this.damage = damage;
            this.item = item;
        }

        public Location getLocation() { return location; }
        public Vector getVelocity() { return velocity; }
        public double getBounce() { return bounce; }
        public double getDrag() { return drag; }
        public double getGravity() { return gravity; }
        public double getExpansion() { return expansion; }

        public ParticleData[] getTrail() { return trail; }
        public double getTrailStepSize() { return trailStepSize; }
        public ItemUser getShooter() { return shooter; }
        public double getDamage() { return damage; }
        public ItemStack getItem() { return item; }

    }

    class Projectile extends me.aecsocket.unifiedframework.util.Projectile {
        private final CalibrePlugin plugin;
        private Data data;

        public Projectile(CalibrePlugin plugin, Data data) {
            super(data.location, data.velocity, data.bounce, data.drag, data.gravity, data.expansion);
            this.plugin = plugin;
            this.data = data;
        }

        public CalibrePlugin getPlugin() { return plugin; }

        public Data getData() { return data; }
        public void setData(Data data) { this.data = data; }

        @Override
        public void tick(TickContext tickContext) {
            super.tick(tickContext);
            if (getTravelled() > plugin.setting("projectile.max_distance", double.class, 2048d))
                tickContext.remove();
        }

        @Override
        protected RayTraceResult rayTrace(double distance) {
            RayTraceResult[] ray = {null};
            Utils.useService(CalibreRaytraceService.class, provider -> ray[0] =
                    provider.rayTrace(getLocation(), getVelocity(), distance, getExpansion()));
            return ray[0];
        }

        @Override
        protected void step(TickContext tickContext, Vector from, Vector delta) {
            ParticleData[] trail = data.getTrail();
            if (trail == null) return;

            double stepSize = data.trailStepSize;
            World world = getLocation().getWorld();
            Vector step = delta.clone().normalize().multiply(stepSize);
            for (int i = 0; i < delta.length() / stepSize; i++)
                ParticleData.spawn(from.add(step).toLocation(world), trail);
        }

        @Override
        protected void hitEntity(TickContext tickContext, RayTraceResult ray, Entity entity) {
            Utils.useService(CalibreDamageService.class, provider ->
                    provider.damage(data.shooter, entity, data.damage, ray.getHitPosition(), data.item));
        }
    }
}
