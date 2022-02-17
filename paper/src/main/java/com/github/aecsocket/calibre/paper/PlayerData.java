package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

import static net.kyori.adventure.text.Component.*;
import static com.github.aecsocket.calibre.paper.CalibrePlugin.format2Raw;

public final class PlayerData {
    public static final String
        EXPLOSION_INFO_RESULT = "explosion.info.result",
        EXPLOSION_INFO_IN_RANGE = "explosion.info.in_range",
        EXPLOSION_INFO_OUT_RANGE = "explosion.info.out_range",
        EXPLOSION_INFO_PENETRATE = "explosion.info.penetrate",
        EXPLOSION_INFO_NO_PENETRATE = "explosion.info.no_penetrate",
        EXPLOSION_INFO_DAMAGE = "explosion.info.damage",
        EXPLOSION_INFO_NO_DAMAGE = "explosion.info.no_damage";
    private static final PaperParticleEffect EXPLOSION_INFO_EFFECT = new PaperParticleEffect(
        Particle.FLAME, 0, Vector3.ZERO, 0, null
    );
    private static final double LINE_STEP = 0.5;

    private final CalibrePlugin plugin;
    private final Player handle;
    private final Effector effector;

    CalibreCommand.@Nullable ExplosionInfo explosionInfo;

    PlayerData(CalibrePlugin plugin, Player handle) {
        this.plugin = plugin;
        this.handle = handle;
        effector = plugin.effectors().ofPlayer(handle);
    }

    public CalibrePlugin plugin() { return plugin; }
    public Player handle() { return handle; }

    public void load() {
        if (explosionInfo != null) {
            explosionInfo.explosion().update();
        }
    }

    public void tick(TaskContext ctx) {
        Location location = handle.getLocation();
        Vector3 position = PaperUtils.toCommons(location);
        Locale locale = handle.locale();

        if (explosionInfo != null && explosionInfo.location().getWorld().equals(location.getWorld())) {
            Explosions.Instance explosion = explosionInfo.explosion();
            Explosions.Instance.DamageComponent damage = explosion.computeDamage(explosionInfo.location(), handle);

            Location from = explosionInfo.location();
            Location to = damage.to();
            double distance = from.distance(to);
            Vector3 step = PaperUtils.toCommons(to).subtract(PaperUtils.toCommons(from)).divide(distance);
            Vector3 fromPos = PaperUtils.toCommons(from);
            for (double d = 0; d < distance; d += LINE_STEP) {
                effector.spawn(EXPLOSION_INFO_EFFECT, fromPos.add(step.multiply(d)));
            }

            handle.sendActionBar(plugin.i18n().line(locale, EXPLOSION_INFO_RESULT,
                c -> c.of("distance", () -> c.line(damage.distance() <= explosion.maxDistance() ? EXPLOSION_INFO_IN_RANGE : EXPLOSION_INFO_OUT_RANGE,
                    d -> d.of("max_distance", () -> text(format2Raw(locale, explosion.maxDistance()))),
                    d -> d.of("distance", () -> text(format2Raw(locale, damage.distance())))
                )),
                c -> c.of("penetration", () -> c.line(explosion.penetration() > damage.hardness() ? EXPLOSION_INFO_PENETRATE : EXPLOSION_INFO_NO_PENETRATE,
                    d -> d.of("hardness", () -> text(format2Raw(locale, damage.hardness()))),
                    d -> d.of("penetration", () -> text(format2Raw(locale, explosion.penetration())))
                )),
                c -> c.of("damage", () -> c.line(damage.damage() > plugin.explosions().config().minDamage() ? EXPLOSION_INFO_DAMAGE : EXPLOSION_INFO_NO_DAMAGE,
                    d -> d.of("damage", () -> text(format2Raw(locale, damage.damage())))))));
        }
    }
}
