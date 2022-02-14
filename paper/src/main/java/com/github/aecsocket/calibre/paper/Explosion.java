package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.effect.SoundEffect;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

public record Explosion(
    CalibrePlugin plugin,
    Options options,
    double power, // 1 power -> 1 damage @ 1m
    int fragments,
    List<SoundEffect> sounds,
    List<PaperParticleEffect> dynamicParticles,
    List<PaperParticleEffect> staticParticles
) {
    public static final double MAX_DISTANCE_FACTOR = 10;

    @ConfigSerializable
    public record Options(
        List<SoundEffect> sounds,
        List<PaperParticleEffect> dynamicParticles,
        List<PaperParticleEffect> staticParticles
    ) {}

    public static Explosion explosion(CalibrePlugin plugin, Options options, double power, int fragments) {
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

        return new Explosion(plugin, options, power, fragments, sounds, dynamicParticles, staticParticles);
    }

    public double maxDistance() {
        return power * MAX_DISTANCE_FACTOR;
    }

    public double damage(double distance) {
        return power * (1 / distance);
    }

    public void spawn(Location location, @Nullable LivingEntity source) {
        Vector3 origin = PaperUtils.toCommons(location);

        // Effects
        for (var player : location.getWorld().getPlayers()) {
            Effector effector = plugin.effectors().ofPlayer(player);
            sounds.forEach(sound -> effector.play(sound, origin));
            dynamicParticles.forEach(particle -> effector.spawn(particle, origin));
            staticParticles.forEach(particle -> effector.spawn(particle, origin));
        }

        // Blunt damage
        for (var target : location.getNearbyLivingEntities(maxDistance())) {
            double distance = distance(target, location);
            plugin.damage(target, damage(distance), source);
        }

        // Shrapnel
    }

    public static double distance(LivingEntity target, Location location) {
        return Math.max(0.001, Math.min(
                // TODO don't use sqrt here
                target.getLocation().distance(location),
                target.getEyeLocation().distance(location)
        ));
    }
}
