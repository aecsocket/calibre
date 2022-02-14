package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.SoundEffect;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final Explosion.Options explosionOptions = new Explosion.Options(List.of(
        SoundEffect.soundEffect(Sound.sound(
            Key.key("desolated", "generic.explosion"), Sound.Source.MASTER, 1f, 1f
        ), 0.12, 0.16),
        SoundEffect.soundEffect(Sound.sound(
            Key.key("desolated", "generic.explosion_far"), Sound.Source.MASTER, 0.5f, 1f
        ), 0.16, 0.32)
    ), List.of(
        new PaperParticleEffect(Particle.FLAME, 6, Vector3.ZERO, 3, null)
    ), List.of(
        new PaperParticleEffect(Particle.LAVA, 2, Vector3.vec3(2), 0, null),
        new PaperParticleEffect(Particle.CAMPFIRE_COSY_SMOKE, 1, Vector3.vec3(2), 0.01, null)
    ));

    public PaperEffectors effectors() { return effectors; }
    public Explosion.Options explosionOptions() { return explosionOptions; }

    public void damage(LivingEntity entity, double damage, @Nullable LivingEntity source) {
        if (entity == source)
            entity.damage(damage);
        else
            entity.damage(damage, source);
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }
}
