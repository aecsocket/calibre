package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.calibre.core.Projectile;
import com.github.aecsocket.minecommons.core.Ticks;
import com.github.aecsocket.minecommons.core.bounds.Bound;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.Task;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.serializers.BasicBoundSerializer;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.github.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import com.google.common.collect.ImmutableMap;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Predicate;

public final class CalibrePlugin extends BasePlugin<CalibrePlugin> {
    private static final Map<Locale, DecimalFormat> DECIMAL_FORMATS = new HashMap<>();

    private final PaperScheduler scheduler = new PaperScheduler(this);
    private final PaperEffectors effectors = new PaperEffectors(this);
    private final Map<Player, PlayerData> playerData = new HashMap<>();
    private final Explosions explosions = new Explosions(this);
    private final Penetration penetration = new Penetration(this);
    private final Raycasts raycasts = new Raycasts(this);

    public PaperScheduler scheduler() { return scheduler; }
    public PaperEffectors effectors() { return effectors; }
    public Explosions explosions() { return explosions; }
    public Penetration penetration() { return penetration; }
    public Raycasts raycasts() { return raycasts; }

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

            record PaperFluid(double density, Material material) implements Projectile.Fluid<PaperRaycast.PaperBoundable> {
                @Override
                public String toString() {
                    return "[μ=" + density + ":" + material.key().value() + "]";
                }

                @Override
                public boolean isOf(PaperRaycast.PaperBoundable hit) {
                    return hit.block() != null && hit.block().getType() == material;
                }
            }

            PaperFluid air = new PaperFluid(1.225, Material.AIR);
            Map<Material, PaperFluid> fluids = ImmutableMap.<Material, PaperFluid>builder()
                .put(Material.WATER, new PaperFluid(997, Material.WATER))
                .put(Material.LAVA, new PaperFluid(2500, Material.LAVA))
                .build();

            Optional<PaperFluid> optFluid(Block block) {
                return Optional.ofNullable(fluids.get(block.getType()));
            }

            @EventHandler
            void onEvent(PlayerInteractEvent event) {
                if (event.getAction() == Action.LEFT_CLICK_AIR) {
                    Player player = event.getPlayer();
                    World world = player.getWorld();
                    Location eye = player.getEyeLocation();
                    Vector3 pos = PaperUtils.toCommons(eye);
                    Vector3 dir = PaperUtils.toCommons(eye.getDirection());

                    PaperRaycast.Options options = new PaperRaycast.Options(
                        new PaperRaycast.Options.OfBlock(false, false, null),
                        new PaperRaycast.Options.OfEntity(null)
                    );
                    Projectile<PaperRaycast.PaperBoundable, PaperFluid> projectile = new Projectile<>(
                        raycasts.build(options, world), pos, dir.multiply(50), Projectile.GRAVITY, optFluid(eye.getBlock()).orElse(air)
                    ) {
                        @Override
                        protected Predicate<PaperRaycast.PaperBoundable> castTest() {
                            return b -> b.entity() == null || b.entity() != player;
                        }

                        @Override
                        protected @Nullable PaperFluid fluidOf(Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit) {
                            if (hit.hit().block() == null)
                                return null;
                            return optFluid(hit.hit().block()).orElse(null);
                        }

                        @Override
                        protected double dragCoeff() { return 0.06; }

                        @Override
                        protected double dragArea() { return 2.55e-5; }

                        @Override
                        protected double mass() { return 0.004; }

                        @Override
                        protected double changeFluid(TaskContext ctx, double sec, double maxDistance, Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit, PaperFluid oldFluid, PaperFluid newFluid) {
                            Location location = PaperUtils.toPaper(ray.pos(), world);
                            if (
                                newFluid.material == Material.WATER ^ oldFluid.material == Material.WATER
                            ) {
                                player.spawnParticle(Particle.WATER_SPLASH, location, 16);
                                player.spawnParticle(Particle.WATER_BUBBLE, location, 4);
                                player.playSound(location, Sound.ENTITY_GENERIC_SPLASH, 1f, 1f);
                            }
                            return super.changeFluid(ctx, sec, maxDistance, ray, hit, oldFluid, newFluid);
                        }

                        @Override
                        protected double step(TaskContext ctx, double sec) {
                            if (!new Location(world, position.x(), position.y(), position.z()).isChunkLoaded()) {
                                ctx.cancel();
                                return 0;
                            }

                            Vector3 origin = position;
                            double res = super.step(ctx, sec);
                            Vector3 direction = position.subtract(origin);
                            double distance = direction.length();
                            direction = direction.divide(distance);
                            for (double d = 0; d < distance; d += 3) {
                                player.spawnParticle(Particle.FLAME, PaperUtils.toPaper(origin.add(direction.multiply(d)), world), 0);
                            }
                            return res;
                        }

                        /*@Override
                        protected double hardness(PaperRaycast.PaperBoundable hit) {
                            return hit.map(block -> penetration.hardness(block.getBlockData()), entity -> 0d); // todo hard
                        }

                        @Override
                        protected double penetration() {
                            return 10;
                        }

                        @Override
                        protected Vector2 penetrationDeflect() {
                            return Vector2.vec2(0.05);
                        }

                        @Override
                        protected double deflectThreshold() {
                            return 0.5;
                        }

                        @Override
                        protected void tick0(TaskContext ctx, double sec) {
                            Vector3 origin = position;
                            Vector3 direction = direction();
                            double distance = travelled();

                            super.tick0(ctx, sec);

                            distance = travelled() - distance;
                            for (double d = 0; d < distance; d += 3) {
                                player.spawnParticle(Particle.FLAME, PaperUtils.toPaper(origin.add(direction.multiply(d)), world), 0);
                            }
                        }

                        @Override
                        protected double step(TaskContext ctx, double remaining) {
                            if (!new Location(world, position.x(), position.y(), position.z()).isChunkLoaded()) {
                                ctx.cancel();
                                return 0;
                            }
                            return super.step(ctx, remaining);
                        }

                        @Override
                        protected boolean changeMedium(StepContext ctx, Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit, Vector3 position, PaperFluid oldMedium, PaperFluid newMedium) {
                            Location location = PaperUtils.toPaper(position, world);
                            world.spawnParticle(Particle.END_ROD, location, 0);

                            if (
                                newMedium.block != null
                                && oldMedium.hasWater() != newMedium.hasWater()
                                && !newMedium.block.getMaterial().isCollidable()
                            ) {
                                world.spawnParticle(Particle.WATER_SPLASH, location, 16);
                                world.spawnParticle(Particle.WATER_BUBBLE, location, 4, 0.05, 0, 0.05, 0);
                                world.playSound(location, Sound.ENTITY_GENERIC_SPLASH, 1f, 1f);
                            }

                            return super.changeMedium(ctx, ray, hit, position, oldMedium, newMedium);
                        }*/
                    };
                    scheduler.run(Task.repeating(projectile::tick, Ticks.MSPT));
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
    protected void configOptionsDefaults(TypeSerializerCollection.Builder serializers, ObjectMapper.Factory.Builder mapperFactory) {
        super.configOptionsDefaults(serializers, mapperFactory);
        serializers
            .registerExact(Bound.class, BasicBoundSerializer.INSTANCE);
    }

    @Override
    public void load() {
        super.load();
        explosions.load();
        penetration.load();
        raycasts.load();
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
