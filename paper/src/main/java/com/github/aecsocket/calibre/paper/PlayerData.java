package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.minecommons.core.effect.Effector;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperParticleEffect;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.awt.print.Paper;
import java.util.Locale;

import static net.kyori.adventure.text.Component.*;

public final class PlayerData {
    public static final String
        EXPLOSION_INFO = "explosion.info";

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

    public void tick(TaskContext ctx) {
        Location location = handle.getLocation();
        Vector3 position = PaperUtils.toCommons(location);
        Locale locale = handle.locale();

        if (explosionInfo != null && explosionInfo.location().getWorld().equals(location.getWorld())) {
            effector.spawn(
                plugin.setting(new PaperParticleEffect(
                    Particle.FLAME, 0, Vector3.ZERO, 0, null
                ), (n, d) -> n.get(PaperParticleEffect.class, d), "explosion", "info_particle"),
                PaperUtils.toCommons(explosionInfo.location())
            );

            Explosions.Instance explosion = explosionInfo.explosion();
            double distance = Explosions.distance(handle, explosionInfo.location());
            handle.sendActionBar(plugin.i18n().line(locale, EXPLOSION_INFO,
                c -> c.of("distance", () -> text(String.format(locale, "%.2f", distance))),
                c -> c.of("max_distance", () -> text(String.format(locale, "%.2f", explosion.maxDistance()))),
                c -> c.of("damage", () -> text(String.format(locale, "%.2f", explosion.damage(distance))))));
        }
    }
}
