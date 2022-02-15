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

import java.util.*;

public record Explosion(
    CalibrePlugin plugin,
    double power, // 1 power -> 1 damage @ 1m
    int fragments,
    double maxDistance,
    List<SoundEffect> sounds,
    List<PaperParticleEffect> dynamicParticles,
    List<PaperParticleEffect> staticParticles
) {
    public static final double MAX_DISTANCE_FACTOR = 10;

    @ConfigSerializable
    public record Options(
        double exponent,
        double minDamage,
        List<SoundEffect> sounds,
        List<PaperParticleEffect> dynamicParticles,
        List<PaperParticleEffect> staticParticles
    ) {
        public static final Options EMPTY = new Options(1, 1, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public static double damage(double exp, double pwr, double dst) {
        return Math.pow(dst, -exp) * pwr;
    }

    public static double maxDistance(double exp, double pwr, double thresh) {
        return Math.pow(thresh / pwr, 1 / -exp);
    }

    public double damage(double distance) {
        return damage(plugin.explosionOptions().exponent, power, distance);
    }

    public double maxDistance() {
        return maxDistance(plugin.explosionOptions().exponent, power, plugin.explosionOptions().minDamage);
    }

    public static Explosion explosion(CalibrePlugin plugin, double power, int fragments) {
        Options options = plugin.explosionOptions();

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

        return new Explosion(plugin, power, fragments, maxDistance(options.exponent, power, options.minDamage), sounds, dynamicParticles, staticParticles);
    }

    public record Result(
        Map<LivingEntity, Double> damage
    ) {}

    public Result spawn(Location location, @Nullable LivingEntity source) {
        Map<LivingEntity, Double> allDamage = new HashMap<>();

        Vector3 origin = PaperUtils.toCommons(location);
        Options options = plugin.explosionOptions();

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
            double damage = damage(options.exponent, power, distance);
            plugin.damage(target, damage, source);
            allDamage.put(target, damage);
        }

        // Shrapnel

        return new Result(allDamage);
    }

    public static double distance(LivingEntity target, Location location) {
        return Math.max(0.001, Math.min(
            // TODO don't use sqrt here
            target.getLocation().distance(location),
            target.getEyeLocation().distance(location)
        ));
    }
}
