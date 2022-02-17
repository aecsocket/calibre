package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.effect.SoundEffect;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.*;

public final class Explosions {
    @ConfigSerializable
    public record Config(
        double exponent,
        double minDamage,
        double penetration,
        List<SoundEffect> sounds,
        List<PaperParticleEffect> dynamicParticles,
        List<PaperParticleEffect> staticParticles
    ) {
        public static final Config DEFAULT = new Config(
            1, 1, 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
        );
    }

    private final CalibrePlugin plugin;
    private Config config;

    public Explosions(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    public Config config() { return config; }

    public void load() {
        config = plugin.setting(Config.DEFAULT, (n, d) -> n.get(Config.class, d), "explosions");
    }

    public double damage(double power, double distance) {
        return Math.pow(distance, -config.exponent) * power;
    }

    public double maxDistance(double power) {
        return Math.pow(config.minDamage / power, 1 / -config.exponent);
    }

    public double penetration(double power) {
        return power * config.penetration;
    }

    public Instance instance(double power, int fragments) {
        return new Instance(power, fragments);
    }

    public class Instance {
        private final double power;
        private final int fragments;
        private double maxDistance;
        private double penetration;
        private List<SoundEffect> sounds = new ArrayList<>();
        private List<PaperParticleEffect> dynamicParticles = new ArrayList<>();
        private List<PaperParticleEffect> staticParticles = new ArrayList<>();

        public Instance(double power, int fragments) {
            this.power = power;
            this.fragments = fragments;
            update();
        }

        public void update() {
            maxDistance = Explosions.this.maxDistance(power);
            penetration = Explosions.this.penetration(power);

            sounds.clear();
            for (var sound : config.sounds) {
                sounds.add(SoundEffect.soundEffect(
                        sound.sound(), sound.dropoff() * power, sound.range() * power
                ));
            }

            dynamicParticles.clear();
            for (var particle : config.dynamicParticles) {
                dynamicParticles.add(new PaperParticleEffect(
                        particle.name(), (int) (particle.count() * power), particle.size().multiply(power), particle.speed(), particle.data()
                ));
            }

            staticParticles.clear();
            for (var particle : config.staticParticles) {
                staticParticles.add(new PaperParticleEffect(
                        particle.name(), (int) (particle.count() * power), particle.size(), particle.speed(), particle.data()
                ));
            }
        }

        public double power() { return power; }
        public int fragments() { return fragments; }
        public double maxDistance() { return maxDistance; }
        public double penetration() { return penetration; }

        public double computeDamage(double distance) {
            return plugin.explosions().damage(power, distance);
        }

        public record DamageComponent(Location to, double distance, double damage, double hardness) {}

        private DamageComponent damageComponent(PaperRaycast raycast, LivingEntity target, Vector3 from, Location to) {
            Vector3 fTo = PaperUtils.toCommons(to);
            double distance = from.distance(fTo);
            double damage = computeDamage(distance);
            double hardness = plugin.penetration().hardness(raycast, from, fTo, distance, target);
            return new DamageComponent(
                to,
                distance,
                damage * plugin.penetration().penetration(hardness, penetration),
                hardness
            );
        }

        private DamageComponent computeDamage(PaperRaycast raycast, Vector3 origin, LivingEntity target) {
            /*
            Imagine an explosion at (0, 0, 0).
            An entity stands at (1, 0, 0).
            The ray's direction will be (1, 0, 0).
            The ray will NOT intersect a square entity hitbox,
            since it lies on the same plane as one of the sides.
            https://tavianator.com/2015/ray_box_nan.html
            Therefore we move the target location up a bit above the hitbox floor.
             */
            DamageComponent realPosHardness = damageComponent(raycast, target, origin, target.getLocation().add(0, 0.01, 0));
            DamageComponent eyesDamage = damageComponent(raycast, target, origin, target.getEyeLocation());
            return realPosHardness.damage > eyesDamage.damage ? realPosHardness : eyesDamage;
        }

        public DamageComponent computeDamage(Location location, LivingEntity target) {
            return computeDamage(plugin.raycastBuilder().build(location.getWorld()), PaperUtils.toCommons(location), target);
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
            PaperRaycast raycast = plugin.raycastBuilder().build(location.getWorld());
            for (var target : PaperUtils.nearbyEntitiesSorted(location, Vector3.vec3(maxDistance), e -> e instanceof LivingEntity f ? f : null)) {
                double damage = computeDamage(raycast, origin, target).damage;
                if (damage > config.minDamage) {
                    plugin.damage(target, damage, source);
                }
            }

            // Shrapnel
        }
    }
}
