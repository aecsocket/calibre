package com.gitlab.aecsocket.calibre.paper.fire;

import com.gitlab.aecsocket.calibre.core.projectile.Projectile;
import com.gitlab.aecsocket.calibre.core.projectile.ProjectileProvider;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Logging;
import com.gitlab.aecsocket.minecommons.core.Ticks;
import com.gitlab.aecsocket.minecommons.core.scheduler.Scheduler;
import com.gitlab.aecsocket.minecommons.core.scheduler.Task;
import com.gitlab.aecsocket.minecommons.core.scheduler.TaskContext;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.paper.PaperUtils;
import com.gitlab.aecsocket.minecommons.paper.display.Particles;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import com.gitlab.aecsocket.sokol.paper.wrapper.user.EntityUser;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.ParticlesStat.*;

public final class ProjectileProviderSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "projectile_provider";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("trail_particles", particlesStat())
            .put("trail_interval", doubleStat())
            .put("hit_particles", particlesStat())
            .put("hit_block_particles", particlesStat())
            .put("hit_entity_particles", particlesStat())
            .put("raytrace_distance", doubleStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(STATS);
    public static final long RAYTRACE_MAX = 1000;

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance, ProjectileProvider {
        private double originalPenetration = 10;
        private double penetration = originalPenetration;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public ProjectileProviderSystem base() { return ProjectileProviderSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        public double originalPenetration() { return originalPenetration; }

        public double penetration() { return penetration; }
        public void penetration(double penetration) { this.penetration = penetration; }

        @Override
        public void build(StatLists stats) {
            parent.events().register(Projectile.Events.Tick.class, this::event, listenerPriority);
            parent.events().register(Projectile.Events.Hit.class, this::event, listenerPriority);
            /*parent.events().register(Projectile.Events.Hit.class, event -> {
                // TODO
                event.result(Projectile.OnHit.REMOVE);
                if (!(event.projectile() instanceof PaperProjectile projectile))
                    return;
                if (!(event.hit().hit() instanceof PaperRaycast.PaperBoundable paper))
                    return;

                double hardness = 0;
                if (paper.entity() != null)
                    hardness = calibre.materials().hardness(paper.entity().getType(), paper.name());
                if (paper.block() != null)
                    hardness = calibre.materials().hardness(paper.block().getType(), paper.name());

                if (!(paper.entity() instanceof LivingEntity living))
                    return;
                living.damage(5 * (penetration / originalPenetration), projectile.shooter());
                double oldPen = penetration;
                penetration -= hardness;
                if (penetration > 0) {
                    event.result(Projectile.OnHit.PENETRATE);
                    projectile.velocity(projectile.velocity().multiply(penetration / oldPen));
                }
            });*/
        }

        @Override
        public void launchProjectile(ItemUser user, Vector3 origin, Vector3 velocity) {
            if (!(user instanceof EntityUser paper))
                return;
            Scheduler scheduler = calibre.paperScheduler();
            // TODO expand this to block shooters, how?
            World world = paper.location().getWorld();
            PaperRaycast raycast = calibre.raycast(world);
            PaperProjectile projectile = new PaperProjectile(parent.root(), parent, raycast,
                    origin, velocity, world, paper.handle());

            AtomicBoolean cont = new AtomicBoolean(true);
            scheduler.run(Task.single(ctx -> {
                parent.stats().<Double>desc("raytrace_distance").ifPresent(dist -> {
                    long end = System.currentTimeMillis() + RAYTRACE_MAX;
                    for (int i = 0; projectile.travelled() < dist; i++) {
                        if (System.currentTimeMillis() >= end) {
                            calibre.log(Logging.Level.WARNING, "Projectile [" + parent + "] raytraced for longer than " + RAYTRACE_MAX + "ms");
                            break;
                        }

                        var ctx2 = new TaskContext(scheduler, (long) Ticks.MSPT * i, Ticks.MSPT, i);
                        projectile.tick(ctx2);
                        if (ctx2.cancelled()) {
                            cont.set(false);
                            return;
                        }
                    }
                });
                if (cont.get())
                    scheduler.run(Task.repeating(projectile::tick, Ticks.MSPT));
            }));
        }

        protected void event(Projectile.Events.Tick event) {
            if (!event.local() || !(event.projectile() instanceof PaperProjectile projectile))
                return;
            parent.stats().<Double>desc("trail_interval").ifPresent(trailInterval -> {
                List<Particles> particles = parent.stats().reqDesc("trail_particles");
                World world = projectile.world();
                Vector3 pos = event.oPosition();
                Location loc = new Location(world, pos.x(), pos.y(), pos.z());
                Vector step = PaperUtils.toBukkit(event.oVelocity().normalize().multiply(trailInterval));
                Vector3 velocity = projectile.velocity().multiply(event.step());
                for (double f = 0; f < event.ray().distance(); f += trailInterval) {
                    particles.forEach(p -> {
                        if (p.count() == 0)
                            p = new Particles(p.particle(), 0, velocity, p.speed(), p.data());
                        p.spawn(loc);
                    });
                }
            });
        }

        protected void event(Projectile.Events.Hit event) {
            event.result(Projectile.OnHit.REMOVE); // todo
            if (!event.local() || !(event.projectile() instanceof PaperProjectile projectile))
                return;
            Location location = PaperUtils.toBukkit(event.ray().pos(), projectile.world());
            parent.stats().<List<Particles>>desc("hit_particles")
                    .ifPresent(v -> v.forEach(p -> p.spawn(location)));

            if (!(event.hit().hit() instanceof PaperRaycast.PaperBoundable hit))
                return;
            if (hit.block() != null) {
                BlockData data = hit.block().getBlockData();
                parent.stats().<List<Particles>>desc("hit_block_particles")
                        .ifPresent(v -> v.forEach(p -> p.spawn(location, data)));
            }
            if (hit.entity() != null) {
                parent.stats().<List<Particles>>desc("hit_entity_particles")
                        .ifPresent(v -> v.forEach(p -> p.spawn(location)));
            }
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public ProjectileProviderSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority) {
        super(listenerPriority);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new ProjectileProviderSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt());
    }
}
