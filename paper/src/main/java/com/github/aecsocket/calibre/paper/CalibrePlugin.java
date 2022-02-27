package com.github.aecsocket.calibre.paper;

import com.github.aecsocket.calibre.core.Bullet;
import com.github.aecsocket.calibre.core.Projectile;
import com.github.aecsocket.minecommons.core.Ticks;
import com.github.aecsocket.minecommons.core.bounds.Bound;
import com.github.aecsocket.minecommons.core.raycast.Raycast;
import com.github.aecsocket.minecommons.core.scheduler.Task;
import com.github.aecsocket.minecommons.core.scheduler.TaskContext;
import com.github.aecsocket.minecommons.core.serializers.BasicBoundSerializer;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.github.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.github.aecsocket.minecommons.core.vector.polar.Coord3;
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

            record PaperMedium(double friction, @Nullable BlockData block, @Nullable Entity entity) implements Projectile.Medium<PaperRaycast.PaperBoundable> {
                @Override
                public String toString() {
                    StringJoiner res = new StringJoiner(":");
                    res.add("μ=" + friction);
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
                    //System.out.println(block + " / " + hit.block());
                    return (block != null && hit.block() != null && block.getMaterial() == hit.block().getType())
                        || (entity != null && hit.entity() != null && hit.entity().getEntityId() == entity.getEntityId());
                }

                boolean hasWater() {
                    return block != null && hasWater(block);
                }

                static boolean hasWater(BlockData block) {
                    return block.getMaterial() == Material.WATER
                        || block instanceof Waterlogged wl && wl.isWaterlogged();
                }
            }

            PaperMedium mediumOf0(Block block) {
                BlockData data = block.getBlockData();
                Material mat = block.getType();
                return new PaperMedium(switch (mat) {
                    case AIR -> 0.005;
                    case WATER -> 1;
                    case LAVA -> 0.025;
                    default -> mat.getBlastResistance() * 10; // todo
                    /*case AIR -> 0.999;
                    case WATER -> 0.99;
                    case LAVA -> 0.98;
                    default -> 0.5; // todo*/
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
                    Projectile<PaperRaycast.PaperBoundable, PaperMedium> projectile = new Bullet<>(
                        raycasts.build(options, world), pos, dir.multiply(910), Projectile.GRAVITY, mediumOf0(eye.getBlock())
                    ) {
                        @Override
                        protected Predicate<PaperRaycast.PaperBoundable> castTest() {
                            return b -> b.entity() == null || b.entity() != player;
                        }

                        @Override
                        protected PaperMedium mediumOf(Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit) {
                            var obj = hit.hit();
                            if (obj.entity() != null) {
                                return new PaperMedium(0.1, null, obj.entity());
                            }
                            if (obj.block() != null) {
                                return mediumOf0(obj.block());
                            }
                            throw new IllegalStateException();
                        }

                        @Override
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
                        protected boolean changeMedium(StepContext ctx, Raycast.Result<PaperRaycast.PaperBoundable> ray, Raycast.Hit<PaperRaycast.PaperBoundable> hit, Vector3 position, PaperMedium oldMedium, PaperMedium newMedium) {
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
                        }
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
