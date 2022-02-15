package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.effect.SoundEffect;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

@ConfigSerializable
public record Explosions(
    double exponent,
    double minDamage,
    List<SoundEffect> sounds,
    List<PaperParticleEffect> dynamicParticles,
    List<PaperParticleEffect> staticParticles
) {
    public static final Explosions DEFAULT = new Explosions(
        1, 1, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
    );

    public double damage(double power, double distance) {
        return Math.pow(distance, -exponent) * power;
    }

    public double maxDistance(double power) {
        return Math.pow(minDamage / power, 1 / -exponent);
    }

    public static double distance(LivingEntity target, Location location) {
        return Math.max(0.001, Math.min(
            target.getLocation().distance(location),
            target.getEyeLocation().distance(location)
        ));
    }

    public static Instance instance(CalibrePlugin plugin, double power, int fragments) {
        Explosions options = plugin.explosions();

        List<SoundEffect> sounds = new ArrayList<>();
        for (var sound : options.sounds) {
            sounds.add(SoundEffect.soundEffect(
                    sound.sound(), sound.dropoff() * power, sound.range() * power
            ));
        }

        List<PaperParticleEffect> dynamicParticles = new ArrayList<>();
        for (var particle : options.dynamicParticles) {
            dynamicParticles.add(new PaperParticleEffect(
                    particle.name(), (int) (particle.count() * power), particle.size().multiply(power), particle.speed(), particle.data()
            ));
        }

        List<PaperParticleEffect> staticParticles = new ArrayList<>();
        for (var particle : options.staticParticles) {
            staticParticles.add(new PaperParticleEffect(
                    particle.name(), (int) (particle.count() * power), particle.size(), particle.speed(), particle.data()
            ));
        }

        return new Instance(plugin, power, fragments, sounds, dynamicParticles, staticParticles);
    }

    public record Result(
        Map<LivingEntity, Double> damage
    ) {}

    public record Instance(
        CalibrePlugin plugin,
        double power,
        int fragments,
        List<SoundEffect> sounds,
        List<PaperParticleEffect> dynamicParticles,
        List<PaperParticleEffect> staticParticles
    ) {
        public double damage(double distance) {
            return plugin.explosions().damage(power, distance);
        }

        public double maxDistance() {
            return plugin.explosions().maxDistance(power);
        }

        public Result spawn(Location location, @Nullable LivingEntity source) {
            Map<LivingEntity, Double> allDamage = new HashMap<>();

            Vector3 origin = PaperUtils.toCommons(location);
            Explosions options = plugin.explosions();

            // Effects
            for (var player : location.getWorld().getPlayers()) {
                Effector effector = plugin.effectors().ofPlayer(player);
                sounds.forEach(sound -> effector.play(sound, origin));
                dynamicParticles.forEach(particle -> effector.spawn(particle, origin));
                staticParticles.forEach(particle -> effector.spawn(particle, origin));
            }

            // Blunt damage
            for (var target : location.getNearbyLivingEntities(maxDistance())) {
                Location to = target.getLocation();
                double distance = location.distance(to);
                double damage = damage(distance);

                double hardness = 0;
                PaperRaycast raycast = plugin.raycastBuilder().build(location.getWorld());
                Vector3 pos = origin;
                Vector3 dir = PaperUtils.toCommons(to).subtract(origin).normalize();
                Vector3 epsilon = dir.multiply(0.01);
                Raycast.Result<PaperRaycast.PaperBoundable> res;
                while ((res = raycast.cast(pos, dir, distance, null)).hit() != null) {
                    var hit = res.hit().hit();
                    if (hit.block() != null) {
                        hardness += hit.block().getType().getBlastResistance() * res.hit().penetration();
                    }
                    pos = res.hit().out().add(epsilon);
                }

                target.sendMessage("hardness = " + hardness);

                /*double distance = distance(target, location);
                double damage = damage(distance);

                // Penetration todo
                Penetration.Instance penetration = plugin.penetration().instance();*/

                plugin.damage(target, damage, source);
                allDamage.put(target, damage);
            }

            // Shrapnel

            return new Result(allDamage);
        }
    }
}
