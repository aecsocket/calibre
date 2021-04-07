package com.gitlab.aecsocket.calibre.paper.util;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import com.gitlab.aecsocket.unifiedframework.paper.util.RayTraceUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitCollidable;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitProjectile;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitRayTrace;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Explosion {
    public static class Shrapnel extends BukkitProjectile {
        private double damage;
        private final ParticleData[] trail;

        public Shrapnel(World world, Vector3D position, Vector3D velocity, double damage, ParticleData[] trail) {
            super(world, position, velocity);
            this.damage = damage;
            this.trail = trail;
        }

        public double damage() { return damage; }
        public ParticleData[] trail() { return trail; }

        @Override
        protected void step(TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
            super.step(tickContext, ray, from, delta, deltaLength);
            Random rng = ThreadLocalRandom.current();
            /*if (rng.nextDouble() < (travelled() / 20)) {
                tickContext.remove();
                return;
            }*/
            Vector step = VectorUtils.toBukkit(delta.normalize().multiply(1));
            Location current = VectorUtils.toBukkit(from).toLocation(world);
            for (double d = 0; d < deltaLength; d += 1) {
                ParticleData.spawn(current, trail);
                current.add(step);
            }
        }

        @Override
        protected void collide(TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided) {
            super.collide(tickContext, ray, collided);
            if (collided.isEntity()) {
                Entity entity = collided.entity();
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    living.damage(damage, source);
                    living.setNoDamageTicks(0);
                }
            } else {
                //Block block = collided.block();
                //block.setType(Material.AIR);
            }
            tickContext.remove();
        }
    }

    private static final Vector3D[] computedAngles = new Vector3D[512];

    public static void computeAngles() {
        double x = 0;

        Random rng = ThreadLocalRandom.current();
        int i = 0;
        while (i < computedAngles.length) {
            Vector3D current = new Vector3D(rng.nextDouble() - 0.5, rng.nextDouble() - 0.5, rng.nextDouble() - 0.5).normalize();
            for (int j = 0; i == 0 || j < i; j++) {
                Vector3D test = computedAngles[j];
                if (i == 0 || current.angle(test) >= x) {
                    computedAngles[i] = current;
                    ++i;
                    break;
                }
            }
        }
    }

    private final CalibrePlugin plugin;
    private double power;
    private double damage;
    private double dropoff;
    private double range;

    public Explosion(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public CalibrePlugin plugin() { return plugin; }

    public double power() { return power; }
    public Explosion power(double power) { this.power = power; return this; }

    public double damage() { return damage; }
    public Explosion damage(double damage) { this.damage = damage; return this; }

    public double dropoff() { return dropoff; }
    public Explosion dropoff(double dropoff) { this.dropoff = dropoff; return this; }

    public double range() { return range; }
    public Explosion range(double range) { this.range = range; return this; }

    public void spawn(Location from, Entity source) {
        SoundData.play(() -> from, plugin.setting(n -> n.get(SoundData[].class), "explosion", "sound"));
        Random rng = ThreadLocalRandom.current();
        ParticleData[] particles = plugin.setting(n -> n.get(ParticleData[].class), "explosion", "particle");
        for (int i = 0; i < power; i++) {
            ParticleData.spawn(from, data -> {
                if (data.count() == 0)
                    return data.size(new Vector3D(rng.nextDouble() - 0.5, rng.nextDouble() - 0.5, rng.nextDouble() - 0.5).normalize());
                return data;
            }, particles);
        }

        from.getNearbyLivingEntities(range).forEach(entity -> {
            Location toFeet = entity.getLocation();
            double toFeetD = from.distance(toFeet);
            Location toEyes = entity.getEyeLocation();
            double toEyesD = from.distance(toEyes);
            Location to;
            double distance;
            if (toFeetD < toEyesD) {
                to = toFeet;
                distance = toFeetD;
            } else {
                to = toEyes;
                distance = toEyesD;
            }
            Vector delta = to.clone().subtract(from).toVector();

            double multiplier = 1 - Utils.clamp01((distance-dropoff) / (range-dropoff));
            entity.damage(damage * multiplier, entity == source ? null : source);
        });
    }

    /*public void spawn(Location location) {
        if (computedAngles[0] == null)
            computeAngles();

        SoundData.play(() -> location, plugin.setting(n -> n.get(SoundData[].class), "explosion", "sound"));

        SchedulerLoop loop = plugin.schedulerLoop();
        Vector3D position = VectorUtils.toUF(location.toVector());
        int i = 0;
        for (Vector3D angle : computedAngles) {
            loop.register(new Shrapnel(location.getWorld(), position, angle.multiply(50), 5,
                    plugin.setting(n -> n.get(ParticleData[].class), "explosion", "particle")));
            ++i;
        }
    }*/
}
