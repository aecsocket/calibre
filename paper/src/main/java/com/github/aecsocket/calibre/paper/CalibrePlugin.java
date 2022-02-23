package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.calibre.core.Projectile;
import com.github.aecsocket.minecommons.core.Ticks;
import com.github.aecsocket.minecommons.core.bounds.Bound;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.Task;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.serializers.BasicBoundSerializer;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.paper.PaperUtils;
import com.github.aecsocket.minecommons.paper.effect.PaperEffectors;
import com.github.aecsocket.minecommons.paper.plugin.BasePlugin;
import com.github.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.github.aecsocket.minecommons.paper.scheduler.PaperScheduler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
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

            record PaperMedium(double density, @Nullable BlockData block, @Nullable Entity entity) implements Projectile.Medium<PaperRaycast.PaperBoundable> {
                @Override
                public String toString() {
                    StringJoiner res = new StringJoiner(":");
                    res.add(""+density);
                    if (block != null)
                        res.add(block.getMaterial().getKey().value());
                    if (entity != null)
                        res.add(entity.getName());
                    return "[" + res.toString() + "]";
                }

                // so this is bad cause it only compares blocks by material
                // TODO make it compare by more stuff for blocks
                @Override
                public boolean isOf(PaperRaycast.PaperBoundable hit) {
                    if (block != null && hit.block() != null && block.getMaterial() == hit.block().getType()) return true;
                    if (entity != null && hit.entity() != null && hit.entity().getEntityId() == entity.getEntityId()) return true;
                    return false;
                }

                boolean hasWater() {
                    if (block == null)
                        return false;
                    if (block.getMaterial() == Material.WATER)
                        return true;
                    if (block instanceof Waterlogged wl && wl.isWaterlogged())
                        return true;
                    return false;
                }
            }

            PaperMedium mediumOf0(Block block) {
                Material mat = block.getType();
                return new PaperMedium(switch (mat) {
                    case AIR -> 1.225;
                    case WATER -> 997;
                    case LAVA -> 2500;
                    default -> mat.getBlastResistance() * 500;
                }, block.getBlockData(), null);
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
                    Projectile<PaperRaycast.PaperBoundable, PaperMedium> projectile = new Projectile<>(
                        raycasts.build(options, world), pos, dir.multiply(50), Projectile.GRAVITY,
                        0.06, 2.55e-5, 0.004, mediumOf0(eye.getBlock())
                    ) {
                        @Override
                        protected Predicate<PaperRaycast.PaperBoundable> castTest() {
                            return b -> b.entity() == null || b.entity() != player;
                        }

                        @Override
                        protected PaperMedium mediumOf(Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit) {
                            var obj = hit.hit();
                            if (obj.entity() != null) {
                                return new PaperMedium(0, null, obj.entity());
                            }
                            if (obj.block() != null) {
                                return mediumOf0(obj.block());
                            }
                            throw new IllegalStateException();
                        }

                        @Override
                        protected double step(TaskContext ctx, double sec) {
                            if (!world.isChunkLoaded((int) position.x() / 16, (int) position.z() / 16)) {
                                ctx.cancel();
                                return 0;
                            }
                            double res = super.step(ctx, sec);
                            player.spawnParticle(Particle.FLAME, PaperUtils.toPaper(position, world), 0);
                            return res;
                        }

                        @Override
                        protected void changeMedium(StepContext ctx, Raycast.Result<PaperRaycast.PaperBoundable> ray, Vector3 position, PaperMedium oldMedium, PaperMedium newMedium) {
                            super.changeMedium(ctx, ray, position, oldMedium, newMedium);
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

                            /*if (ray.)

                            double power = speed / originalSpeed;
                            var bound = ray.hit().hi

                            double penetration = 1 - hardness(bound) / (penetration() * power);
                            if (penetration > 0) {
                                // penetrates
                                penetrate(direction, hit.out(), hit.penetration());
                                velocity = velocity.multiply(Math.min(1, penetration));
                                Coord3 coord = Coord3.coord3(speed, velocity.sphericalYaw(), velocity.sphericalPitch());
                                // TODO deflection inside a medium stuff, make it variable
                                velocity = coord
                                        .yaw(coord.yaw() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                                        .pitch(coord.pitch() + ThreadLocalRandom.current().nextGaussian() * 0.05)
                                        .cartesian();
                                ++penetrated;
                            } else {
                                // try deflect
                                Vector3 normal = hit.normal();
                                double dot = Math.abs(direction.dot(normal));
                                double deflectMult = 1 - dot / (deflectThreshold(bound) * power);
                                if (deflectMult > 0) {
                                    // deflect
                                    deflect(direction, normal, deflectMult);
                                } else {
                                    // stuck
                                    ctx.cancel();
                                }
                            }*/
                        }
                    };
                    /*Projectile<PaperRaycast.PaperBoundable> projectile = new Bullet<>(
                        raycastBuilder.build(world), pos, dir.multiply(100), Projectile.GRAVITY,
                        0.06, 2.55e-5, 0.004
                    ) {
                        @Override
                        protected double deflectThreshold(PaperRaycast.PaperBoundable hit) {
                            return 0.2;
                        }

                        @Override
                        protected double penetration() {
                            return 5;
                        }

                        @Override
                        protected double hardness(PaperRaycast.PaperBoundable hit) {
                            return hit.block() == null ? 0 : penetration.hardness(hit.block().getBlockData());
                        }

                        @Override
                        protected double mediumDensity(Vector3 pos) {
                            // air = 1.225 kg/m^3
                            // water = 997 kg/m^3
                            // lava = 2500 kg/m^3
                            Material mat = world.getBlockAt((int) pos.x(), (int) pos.y(), (int) pos.z()).getType();
                            return switch (mat) {
                                case WATER -> 150000;
                                case LAVA -> 2500;
                                default -> 1.225;
                            };
                        }

                        @Override
                        protected Predicate<PaperRaycast.PaperBoundable> test() {
                            return b -> b.entity() == null || b.entity() != player;
                        }

                        @Override
                        protected void step(TaskContext ctx, Vector3 origin, Vector3 direction, double speed, Raycast.Result<PaperRaycast.PaperBoundable> ray) {
                            super.step(ctx, origin, direction, speed, ray);
                            if (!world.isChunkLoaded((int) position.x() / 16, (int) position.z() / 16)) {
                                ctx.cancel();
                                return;
                            }
                            player.spawnParticle(Particle.END_ROD, PaperUtils.toPaper(position, world), 0);
                        }
                    };*/
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
