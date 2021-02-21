package com.gitlab.aecsocket.calibre.paper.system.gun;

import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.gun.Projectiles;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.ParticleDataStat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitCollidable;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitProjectile;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitRayTrace;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PaperProjectiles {
    private PaperProjectiles() {}

    public static class CalibreProjectile extends BukkitProjectile {
        public enum HitResult {
            STOP,
            BOUNCE,
            CONTINUE
        }

        protected final CalibrePlugin plugin;
        protected final StatMap stats;

        protected ParticleData[] trailParticle;
        protected double trailDistance;

        protected ParticleData[] hitParticle;
        protected SoundData[] hitSound;
        protected SoundData[] hitSoundSource;

        protected ParticleData[] hitBlockParticle;
        protected SoundData[] hitBlockSound;
        protected SoundData[] hitBlockSoundSource;

        protected ParticleData[] hitEntityParticle;
        protected SoundData[] hitEntitySound;
        protected SoundData[] hitEntitySoundSource;

        public CalibreProjectile(World world, Vector3D position, Vector3D velocity, CalibrePlugin plugin, StatMap stats) {
            super(world, position, velocity);
            this.plugin = plugin;
            this.stats = stats;

            trailParticle = stats.val("trail_particle");
            trailDistance = stats.<NumberDescriptor.Double>val("trail_distance").apply();

            hitParticle = stats.val("hit_particle");
            hitSound = stats.val("hit_sound");
            hitSoundSource = stats.val("hit_sound_source");

            hitBlockParticle = stats.val("hit_block_particle");
            hitBlockSound = stats.val("hit_block_sound");
            hitBlockSoundSource = stats.val("hit_block_sound_source");

            hitEntityParticle = stats.val("hit_entity_particle");
            hitEntitySound = stats.val("hit_entity_sound");
            hitEntitySoundSource = stats.val("hit_entity_sound_source");
        }

        public CalibrePlugin plugin() { return plugin; }
        public StatMap stats() { return stats; }

        public ParticleData[] trailParticle() { return trailParticle; }
        public CalibreProjectile trailParticle(ParticleData[] trailParticle) { this.trailParticle = trailParticle; return this; }

        public double trailDistance() { return trailDistance; }
        public CalibreProjectile trailDistance(double trailDistance) { this.trailDistance = trailDistance; return this; }

        public ParticleData[] hitParticle() { return hitParticle; }
        public CalibreProjectile hitParticle(ParticleData[] hitParticle) { this.hitParticle = hitParticle; return this; }

        public SoundData[] hitSound() { return hitSound; }
        public CalibreProjectile hitSound(SoundData[] hitSound) { this.hitSound = hitSound; return this; }

        public SoundData[] hitSoundSource() { return hitSoundSource; }
        public CalibreProjectile hitSoundSource(SoundData[] hitSoundSource) { this.hitSoundSource = hitSoundSource; return this; }

        public ParticleData[] hitBlockParticle() { return hitBlockParticle; }
        public CalibreProjectile hitBlockParticle(ParticleData[] hitBlockParticle) { this.hitBlockParticle = hitBlockParticle; return this; }

        public SoundData[] hitBlockSound() { return hitBlockSound; }
        public CalibreProjectile hitBlockSound(SoundData[] hitBlockSound) { this.hitBlockSound = hitBlockSound; return this; }

        public SoundData[] hitBlockSoundSource() { return hitBlockSoundSource; }
        public CalibreProjectile hitBlockSoundSource(SoundData[] hitBlockSoundSource) { this.hitBlockSoundSource = hitBlockSoundSource; return this; }

        public ParticleData[] hitEntityParticle() { return hitEntityParticle; }
        public CalibreProjectile hitEntityParticle(ParticleData[] hitEntityParticle) { this.hitEntityParticle = hitEntityParticle; return this; }

        public SoundData[] hitEntitySound() { return hitEntitySound; }
        public CalibreProjectile hitEntitySound(SoundData[] hitEntitySound) { this.hitEntitySound = hitEntitySound; return this; }

        public SoundData[] hitEntitySoundSource() { return hitEntitySoundSource; }
        public CalibreProjectile hitEntitySoundSource(SoundData[] hitEntitySoundSource) { this.hitEntitySoundSource = hitEntitySoundSource; return this; }

        @Override
        protected void step(TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
            super.step(tickContext, ray, from, delta, deltaLength);
            Vector step = VectorUtils.toBukkit(delta.normalize().multiply(trailDistance));
            Location current = VectorUtils.toBukkit(from).toLocation(world);
            double trailMult = tickContext.delta() / 1000d;
            for (double d = 0; d < deltaLength; d += trailDistance) {
                ParticleData.spawn(current, data -> {
                    if (data.count() == 0)
                        return data.size(velocity.multiply(trailMult));
                    return data;
                }, trailParticle);
                current.add(step);
            }
        }

        @Override
        protected void collide(TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided) {
            Location location = VectorUtils.toBukkit(ray.position()).toLocation(world);

            ParticleData.spawn(location, hitParticle);
            SoundData.play(() -> location, hitSound);
            if (source instanceof Player)
                SoundData.play((Player) source, () -> source.getLocation(), hitSoundSource);

            HitResult result;
            if (collided.isBlock())
                result = collideBlock(tickContext, ray, location, collided.block());
            else
                result = collideEntity(tickContext, ray, location, collided.entity());

            if (result == HitResult.BOUNCE && bounce > 0) {
                bounce(ray);
            } else if (result != HitResult.CONTINUE) {
                tickContext.remove();
            }
        }

        protected HitResult collideBlock(TickContext tickContext, BukkitRayTrace ray, Location location, Block block) {
            ParticleData.spawn(location, block.getBlockData(), hitBlockParticle);
            SoundData.play(() -> location, hitBlockSound);
            if (source instanceof Player)
                SoundData.play((Player) source, () -> source.getLocation(), hitBlockSoundSource);
            if (bounce > 0)
                return HitResult.BOUNCE;
            return HitResult.CONTINUE;
        }

        protected HitResult collideEntity(TickContext tickContext, BukkitRayTrace ray, Location location, Entity entity) {
            ParticleData.spawn(location, hitEntityParticle);
            SoundData.play(() -> location, hitEntitySound);
            if (source instanceof Player)
                SoundData.play((Player) source, () -> source.getLocation(), hitEntitySoundSource);
            return HitResult.STOP;
        }
    }

    public static final Map<String, Stat<?>> BUKKIT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(Projectiles.STATS)
            .init("projectile_bounce", NumberDescriptorStat.of(0d))
            .init("projectile_drag", NumberDescriptorStat.of(0d))
            .init("projectile_expansion", NumberDescriptorStat.of(0d))
            .get();

    public static final Map<String, Stat<?>> CALIBRE_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(BUKKIT_STATS)
            .init("trail_particle", new ParticleDataStat())
            .init("trail_distance", NumberDescriptorStat.of(1d))

            .init("hit_particle", new ParticleDataStat())
            .init("hit_sound", new SoundDataStat())
            .init("hit_sound_source", new SoundDataStat())

            .init("hit_block_particle", new ParticleDataStat())
            .init("hit_block_sound", new SoundDataStat())
            .init("hit_block_sound_source", new SoundDataStat())

            .init("hit_entity_particle", new ParticleDataStat())
            .init("hit_entity_sound", new SoundDataStat())
            .init("hit_entity_sound_source", new SoundDataStat())
            .get();

    public static void applyTo(BukkitProjectile projectile, StatMap stats) {
        projectile
                .bounce(stats.<NumberDescriptor.Double>val("projectile_bounce").apply())
                .drag(stats.<NumberDescriptor.Double>val("projectile_drag").apply())
                .expansion(stats.<NumberDescriptor.Double>val("projectile_expansion").apply())
                .gravity(stats.<NumberDescriptor.Double>val("projectile_gravity").apply());
    }
}
