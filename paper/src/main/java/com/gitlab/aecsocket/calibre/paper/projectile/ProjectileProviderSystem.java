package com.gitlab.aecsocket.calibre.paper.projectile;

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
import com.gitlab.aecsocket.minecommons.paper.display.PreciseSound;
import com.gitlab.aecsocket.minecommons.paper.raycast.PaperRaycast;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.StatMap;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.ParticlesStat.*;

public final class ProjectileProviderSystem extends AbstractSystem implements PaperSystem {
    public static final String ID = "projectile_provider";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("raytrace_distance", doubleStat())

            .put("trail_interval", doubleStat())
            .put("trail_particles", particlesStat())

            .put("hit_particles", particlesStat())
            .put("hit_sounds", soundsStat())

            .put("hit_block_particles", particlesStat())
            .put("hit_block_sounds", soundsStat())

            .put("hit_entity_particles", particlesStat())
            .put("hit_entity_sounds", soundsStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofStats(ID, STATS);
    public static final long RAYTRACE_MAX = 1000;

    public final class Instance extends AbstractSystem.Instance implements PaperSystem.Instance, ProjectileProvider {
        private double trailInterval;
        private List<Particles> trailParticles;
        private List<Particles> hitParticles;
        private List<PreciseSound> hitSounds;
        private List<Particles> hitBlockParticles;
        private List<PreciseSound> hitBlockSounds;
        private List<Particles> hitEntityParticles;
        private List<PreciseSound> hitEntitySounds;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public ProjectileProviderSystem base() { return ProjectileProviderSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        public void build(StatLists stats) {
            parent.events().register(Projectile.Events.Create.class, this::event, listenerPriority);
            parent.events().register(Projectile.Events.Tick.class, this::event, listenerPriority);
            parent.events().register(Projectile.Events.Hit.class, this::event, listenerPriority);
        }

        @Override
        public void launchProjectile(ItemUser user, Vector3 origin, Vector3 velocity) {
            if (!(user instanceof EntityUser paper))
                return;

            Scheduler scheduler = calibre.paperScheduler();
            // TODO expand this to block shooters, how?
            World world = paper.location().getWorld();
            PaperRaycast raycast = calibre.raycast(world);
            PaperProjectile projectile = new PaperProjectile(parent.root().asRoot(), parent.asRoot(), raycast,
                    origin, velocity, world, paper.handle());
            if (
                    new Projectile.Events.Create(projectile.fullTree(), projectile, false, user, origin, velocity).call()
                    | new Projectile.Events.Create(projectile.localTree(), projectile, true, user, origin, velocity).call()
            )
                return;

            AtomicBoolean cont = new AtomicBoolean(true);
            scheduler.run(Task.single(ctx -> {
                parent.stats().<Double>val("raytrace_distance").ifPresent(dist -> {
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

        protected void event(Projectile.Events.Create event) {
            if (!event.local())
                return;
            StatMap stats = event.projectile().fullTree().stats();
            stats.<Double>val("trail_interval").ifPresent(interval -> {
                trailInterval = interval;
                trailParticles = stats.<List<Particles>>val("trail_particles").orElse(null);
            });
            hitParticles = stats.<List<Particles>>val("hit_particles").orElse(null);
            hitSounds = stats.<List<PreciseSound>>val("hit_sounds").orElse(null);
            hitBlockParticles = stats.<List<Particles>>val("hit_block_particles").orElse(null);
            hitBlockSounds = stats.<List<PreciseSound>>val("hit_block_sounds").orElse(null);
            hitEntityParticles = stats.<List<Particles>>val("hit_entity_particles").orElse(null);
            hitEntitySounds = stats.<List<PreciseSound>>val("hit_entity_sounds").orElse(null);
        }

        protected void event(Projectile.Events.Tick event) {
            if (!event.local() || !(event.projectile() instanceof PaperProjectile projectile))
                return;
            if (trailInterval > 0) {
                Location loc = PaperUtils.toBukkit(event.oPosition(), projectile.world());
                Vector step = PaperUtils.toBukkit(event.oVelocity().normalize().multiply(trailInterval));
                Vector3 velocity = event.oVelocity().multiply(event.step());
                for (double f = 0; f < event.ray().distance(); f += trailInterval) {
                    trailParticles.forEach(p -> {
                        if (p.count() == 0)
                            p = p.size(velocity);
                        p.spawn(loc);
                    });
                }
            }
        }

        protected void event(Projectile.Events.Hit event) {
            if (!event.local() || !(event.projectile() instanceof PaperProjectile projectile))
                return;
            Location location = PaperUtils.toBukkit(event.ray().pos(), projectile.world());
            if (hitParticles != null) hitParticles.forEach(p -> p.spawn(location));
            if (hitSounds != null) hitSounds.forEach(s -> s.play(location));

            if (!(event.hit().hit() instanceof PaperRaycast.PaperBoundable hit))
                return;
            if (hit.block() != null) {
                BlockData data = hit.block().getBlockData();
                if (hitBlockParticles != null) hitBlockParticles.forEach(p -> p.spawn(location, data));
                if (hitBlockSounds != null) hitBlockSounds.forEach(s -> s.play(location));
            }
            if (hit.entity() != null) {
                if (hitEntityParticles != null) hitEntityParticles.forEach(p -> p.spawn(location));
                if (hitEntitySounds != null) hitEntitySounds.forEach(s -> s.play(location));
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
