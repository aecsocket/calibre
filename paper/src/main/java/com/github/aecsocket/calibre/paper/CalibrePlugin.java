package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.calibre.core.Projectile;
import com.github.aecsocket.minecommons.core.Ticks;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.Task;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.vector.cartesian.Ray3;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.core.vector.polar.Coord3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.github.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.github.aecsocket.sokol.paper.PaperBlueprint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    private static final Map<Locale, DecimalFormat> DECIMAL_FORMATS = new HashMap<>();

    private final PaperScheduler scheduler = new PaperScheduler(this);
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final Map<Player, PlayerData> playerData = new HashMap<>();
    private final Explosions explosions = new Explosions(this);
    private final Penetration penetration = new Penetration(this);
    private PaperRaycast.Builder raycastBuilder;

    public PaperScheduler scheduler() { return scheduler; }
    public PaperEffectors effectors() { return effectors; }
    public Explosions explosions() { return explosions; }
    public Penetration penetration() { return penetration; }
    public PaperRaycast.Builder raycastBuilder() { return raycastBuilder; }

    public PlayerData playerData(Player player) {
        return playerData.computeIfAbsent(player, k -> new PlayerData(this, k));
    }

    @Override
    public void onEnable() {
        super.onEnable();

        scheduler.run(Task.repeating(ctx -> {
            var iter = playerData.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                if (entry.getKey().isValid()) {
                    ctx.run(Task.single(entry.getValue()::tick));
                } else
                    iter.remove();
            }
        }, Ticks.MSPT));

        Bukkit.getPluginManager().registerEvents(new Listener() {
            /*
                5.56x45mm Bullet:
                  v = 900m/s
                  m = 4g = 0.004kg
                  diameter = 5.70mm = 0.0057m
                  radius = 0.00285m
                  cross section = πr^2 = 2.5517586328783095e-05
                    = 0.000025517m^2
                  ρ = 1.225kg/m^3

                  Cd = 0.06 (https://sites.google.com/site/technicalarchery/technical-discussions-1/drag-coefficients-of-bullets-arrows-and-spears)

                  F = ((1.225 x Cd x 2.552e-05) / 2) * 900^2
                   -> 0.30386664

                  a = F / m = 0.30386664 / 0.004
                   -> 75.96666m/s^2
                   -> 3.798m/t^2
                 */

            @EventHandler
            void onEvent(PlayerInteractEvent event) {
                if (event.getAction() == Action.LEFT_CLICK_AIR) {
                    Player player = event.getPlayer();
                    World world = player.getWorld();
                    Location eye = player.getEyeLocation();
                    Vector3 pos = PaperUtils.toCommons(eye);
                    Vector3 dir = PaperUtils.toCommons(eye.getDirection());
                    Projectile<PaperRaycast.PaperBoundable> projectile = new Projectile<>(
                        raycastBuilder.build(world), pos, dir.multiply(30),
                        0, 0.06, 2.552e-05, 0.004
                    ) {
                        @Override
                        protected double mediumDensity(Vector3 pos) {
                            return pos.y() > 90 ? 100000000d : 1.225;
                        }

                        @Override
                        protected Predicate<PaperRaycast.PaperBoundable> test() {
                            return b -> b.entity() == null || b.entity() != player;
                        }

                        @Override
                        protected void step(TaskContext ctx, Vector3 origin, Vector3 direction, double speed, Raycast.Result<PaperRaycast.PaperBoundable> ray) {
                            player.spawnParticle(Particle.END_ROD, PaperUtils.toPaper(position, world), 0);
                        }

                        @Override
                        protected void hit(TaskContext ctx, Vector3 origin, Vector3 direction, double speed, Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit) {
                            double dot = direction.dot(hit.normal());
                            if (Math.abs(dot) < 0.2) {
                                deflect(direction, hit.normal(), 0.5);
                            } else {
                                penetrate(direction, hit.out(), hit.penetration());
                                velocity = velocity.multiply(0.5);
                                Coord3 coord = Coord3.coord3(speed, velocity.sphericalYaw(), velocity.sphericalPitch());
                                velocity = coord
                                    .yaw(coord.yaw() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                                    .pitch(coord.pitch() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                                    .cartesian();
                            }
                        }
                    };
                    scheduler.run(Task.repeating(ctx -> ctx.run(Task.single(projectile::tick)), Ticks.MSPT));
                } else {
                    scheduler.cancel();
                }
            }
        }, this);
    }

    public void damage(LivingEntity entity, double damage, @Nullable LivingEntity source) {
        if (entity == source)
            entity.damage(damage);
        else
            entity.damage(damage, source);
    }

    @Override
    public void load() {
        super.load();
        explosions.load();
        penetration.load();
        raycastBuilder = PaperRaycast.builder(); // TODO raycast options
        for (var data : playerData.values()) {
            data.load();
        }
    }

    @Override
    protected CalibreCommand createCommand() throws Exception {
        return new CalibreCommand(this);
    }

    public static String format2(Locale locale, double value) {
        DecimalFormat format = DECIMAL_FORMATS.computeIfAbsent(locale, k -> new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(k)));
        return format.format(value);
    }

    public static String format2Raw(Locale locale, double value) {
        return String.format(locale, "%.2f", value);
    }
}
