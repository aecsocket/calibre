package com.gitlab.aecsocket.calibre.paper.system.gun.projectile;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.rule.RuledStatCollectionList;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.StatRenderer;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.calibre.core.system.builtin.ProjectileSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.calibre.core.system.gun.Projectiles;
import com.gitlab.aecsocket.calibre.paper.system.PaperSystem;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.BukkitItemUser;
import com.gitlab.aecsocket.calibre.paper.wrapper.user.EntityUser;
import com.gitlab.aecsocket.unifiedframework.core.event.Cancellable;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;
import com.gitlab.aecsocket.unifiedframework.core.stat.Stat;
import com.gitlab.aecsocket.unifiedframework.core.stat.StatMap;
import com.gitlab.aecsocket.unifiedframework.core.util.log.LogLevel;
import com.gitlab.aecsocket.unifiedframework.paper.loop.SchedulerLoop;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.ParticleDataStat;
import com.gitlab.aecsocket.unifiedframework.paper.stat.impl.data.SoundDataStat;
import com.gitlab.aecsocket.unifiedframework.core.stat.impl.descriptor.NumberDescriptorStat;
import com.gitlab.aecsocket.unifiedframework.core.util.MapInit;
import com.gitlab.aecsocket.unifiedframework.paper.util.VectorUtils;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.ParticleData;
import com.gitlab.aecsocket.unifiedframework.paper.util.data.SoundData;
import com.gitlab.aecsocket.unifiedframework.core.util.descriptor.NumberDescriptor;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitCollidable;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitProjectile;
import com.gitlab.aecsocket.unifiedframework.paper.util.projectile.BukkitRayTrace;
import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector3D;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

public final class PaperProjectileSystem extends AbstractSystem implements PaperSystem, ProjectileSystem {
    public enum HitResult {
        STOP,
        BOUNCE,
        CONTINUE
    }
    public static final String ID = "projectile";
    public static final int LISTENER_PRIORITY = 1090;
    public static final Map<String, Stat<?>> BUKKIT_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(Projectiles.STATS)
            .init("projectile_bounce", NumberDescriptorStat.of(0d))
            .init("projectile_drag", NumberDescriptorStat.of(0d))
            .init("projectile_expansion", NumberDescriptorStat.of(0d))
            .get();

    public static final Map<String, Stat<?>> PAPER_STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init(BUKKIT_STATS)
            .init("raycast_distance", NumberDescriptorStat.of(0d))

            .init("trail_particle", new ParticleDataStat())
            .init("trail_distance", NumberDescriptorStat.of(1d))

            .init("hit_particle", new ParticleDataStat())
            .init("hit_sound", new SoundDataStat())
            .init("hit_sound_source", new SoundDataStat())

            .init("hit_block_particle", new ParticleDataStat())
            .init("hit_block_sound", new SoundDataStat())
            .init("hit_block_sound_source", new SoundDataStat())

            .init("hit_entity_particle", new ParticleDataStat())
            .init("hit_entity_sound", new SoundDataStat())
            .init("hit_entity_sound_source", new SoundDataStat())
            .get();

    @ConfigSerializable
    private static class Dependencies {
        private ConfigurationNode stats;
    }

    @Setting(nodeFromParent = true)
    private Dependencies dependencies;
    @FromMaster(fromDefault = true)
    private transient CalibrePlugin plugin;
    @FromMaster private transient RuledStatCollectionList stats;

    private transient StatRenderer statRenderer;

    /**
     * Used for registration.
     * @param plugin The plugin.
     */
    public PaperProjectileSystem(CalibrePlugin plugin) {
        super(LISTENER_PRIORITY);
        this.plugin = plugin;
    }

    /**
     * Used for deserialization.
     */
    private PaperProjectileSystem() {
        super(LISTENER_PRIORITY);
        plugin = null;
    }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public PaperProjectileSystem(PaperProjectileSystem o) {
        super(o);
        plugin = o.plugin;
        stats = o.stats == null ? null : o.stats.copy();
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return PAPER_STATS; }

    @Override public CalibrePlugin calibre() { return plugin; }
    @Override public RuledStatCollectionList stats() { return stats; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        stats = deserialize(dependencies.stats, RuledStatCollectionList.class);
        dependencies = null;
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = listenerPriority();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);

        statRenderer = parent.system(StatRenderer.class);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        Locale locale = event.locale();
        List<Component> info = new ArrayList<>();
        if (statRenderer != null) {
            if (stats != null) {
                info.addAll(statRenderer.createInfo(locale, stats.build(event.component()), gen(locale, "system." + ID + ".stat_prefix")));
            }
        }

        if (info.size() > 0) {
            info.add(0, gen(locale, "system." + ID + ".header"));
            event.item().addInfo(info);
        }
    }

    @Override
    public void createProjectile(ItemUser user, Vector3D position, Vector3D velocity) {
        if (!(user instanceof BukkitItemUser))
            return;
        BukkitItemUser bukkitUser = (BukkitItemUser) user;

        PaperProjectile projectile = new PaperProjectile(this, bukkitUser.world(), position, velocity);
        applyTo(projectile, tree().stats());
        projectile.source(user instanceof EntityUser ? ((EntityUser) user).entity() : null);

        tree().call(new Events.Create(projectile, user, position, velocity));

        // raycast the first specified amount metres
        SchedulerLoop loop = plugin.schedulerLoop();
        long period = loop.period();

        double raycastDistance = tree().<NumberDescriptor.Double>stat("raycast_distance").apply();
        // max tick time of 1000ms, as a failsafe
        long end = System.currentTimeMillis() + 1000;
        while (projectile.travelled() < raycastDistance) {
            if (System.currentTimeMillis() >= end) {
                plugin.log(LogLevel.WARN, new Throwable(), "Projectile raycast ticked for longer than 1000ms");
                break;
            }
            TickContext context = loop.context(projectile, period);
            projectile.tick(context);
            if (context.removed())
                return;
        }
        loop.register(projectile);
    }

    @Override public PaperProjectileSystem copy() { return new PaperProjectileSystem(this); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o != null && getClass() == o.getClass();
    }

    @Override public int hashCode() { return 1; }

    public static class PaperProjectile extends BukkitProjectile {
        protected final ProjectileSystem system;
        protected final EventDispatcher events;
        protected final StatMap stats;
        protected ParticleData[] trailParticle;
        protected double trailDistance;

        protected ParticleData[] hitParticle;
        protected SoundData[] hitSound;
        protected SoundData[] hitSoundSource;

        protected ParticleData[] hitBlockParticle;
        protected SoundData[] hitBlockSound;
        protected SoundData[] hitBlockSoundSource;

        protected ParticleData[] hitEntityParticle;
        protected SoundData[] hitEntitySound;
        protected SoundData[] hitEntitySoundSource;

        public PaperProjectile(PaperProjectileSystem system, World world, Vector3D position, Vector3D velocity) {
            super(world, position, velocity);
            this.system = system;
            events = new EventDispatcher(system.tree().events());
            stats = new StatMap(system.tree().stats());

            trailParticle = stats.val("trail_particle");
            trailDistance = stats.<NumberDescriptor.Double>val("trail_distance").apply();

            hitParticle = stats.val("hit_particle");
            hitSound = stats.val("hit_sound");
            hitSoundSource = stats.val("hit_sound_source");

            hitBlockParticle = stats.val("hit_block_particle");
            hitBlockSound = stats.val("hit_block_sound");
            hitBlockSoundSource = stats.val("hit_block_sound_source");

            hitEntityParticle = stats.val("hit_entity_particle");
            hitEntitySound = stats.val("hit_entity_sound");
            hitEntitySoundSource = stats.val("hit_entity_sound_source");
        }

        public ProjectileSystem system() { return system; }
        public StatMap stats() { return stats; }

        public ParticleData[] trailParticle() { return trailParticle; }
        public PaperProjectile trailParticle(ParticleData[] trailParticle) { this.trailParticle = trailParticle; return this; }

        public double trailDistance() { return trailDistance; }
        public PaperProjectile trailDistance(double trailDistance) { this.trailDistance = trailDistance; return this; }

        public ParticleData[] hitParticle() { return hitParticle; }
        public PaperProjectile hitParticle(ParticleData[] hitParticle) { this.hitParticle = hitParticle; return this; }

        public SoundData[] hitSound() { return hitSound; }
        public PaperProjectile hitSound(SoundData[] hitSound) { this.hitSound = hitSound; return this; }

        public SoundData[] hitSoundSource() { return hitSoundSource; }
        public PaperProjectile hitSoundSource(SoundData[] hitSoundSource) { this.hitSoundSource = hitSoundSource; return this; }

        public ParticleData[] hitBlockParticle() { return hitBlockParticle; }
        public PaperProjectile hitBlockParticle(ParticleData[] hitBlockParticle) { this.hitBlockParticle = hitBlockParticle; return this; }

        public SoundData[] hitBlockSound() { return hitBlockSound; }
        public PaperProjectile hitBlockSound(SoundData[] hitBlockSound) { this.hitBlockSound = hitBlockSound; return this; }

        public SoundData[] hitBlockSoundSource() { return hitBlockSoundSource; }
        public PaperProjectile hitBlockSoundSource(SoundData[] hitBlockSoundSource) { this.hitBlockSoundSource = hitBlockSoundSource; return this; }

        public ParticleData[] hitEntityParticle() { return hitEntityParticle; }
        public PaperProjectile hitEntityParticle(ParticleData[] hitEntityParticle) { this.hitEntityParticle = hitEntityParticle; return this; }

        public SoundData[] hitEntitySound() { return hitEntitySound; }
        public PaperProjectile hitEntitySound(SoundData[] hitEntitySound) { this.hitEntitySound = hitEntitySound; return this; }

        public SoundData[] hitEntitySoundSource() { return hitEntitySoundSource; }
        public PaperProjectile hitEntitySoundSource(SoundData[] hitEntitySoundSource) { this.hitEntitySoundSource = hitEntitySoundSource; return this; }

        @Override
        protected void step(TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
            super.step(tickContext, ray, from, delta, deltaLength);
            Vector step = VectorUtils.toBukkit(delta.normalize().multiply(trailDistance));
            Location current = VectorUtils.toBukkit(from).toLocation(world);
            double trailMult = tickContext.delta() / 1000d;
            for (double d = 0; d < deltaLength; d += trailDistance) {
                ParticleData.spawn(current, data -> {
                    if (data.count() == 0)
                        return data.size(velocity.multiply(trailMult));
                    return data;
                }, trailParticle);
                current.add(step);
            }
            events.call(new Events.Step(this, tickContext, ray, from, delta, deltaLength));
        }

        @Override
        public void tick(TickContext tickContext) {
            super.tick(tickContext);
            events.call(new Events.Tick(this, tickContext));
        }

        @Override
        protected void collide(TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided) {
            Location location = VectorUtils.toBukkit(ray.position()).toLocation(world);

            HitResult result;
            if (collided.isBlock()) {
                result = bounce > 0 ? HitResult.BOUNCE : HitResult.CONTINUE;
            } else
                result = HitResult.STOP;
            Events.Collide event = events.call(new Events.Collide(this, tickContext, ray, collided, result));
            result = event.result;
            if (!event.cancelled) {
                if (collided.isBlock()) {
                    Block block = collided.block();
                    ParticleData.spawn(location, block.getBlockData(), hitBlockParticle);
                    SoundData.play(() -> location, hitBlockSound);
                    if (source instanceof Player)
                        SoundData.play((Player) source, () -> source.getLocation(), hitBlockSoundSource);
                }
                if (collided.isEntity()) {
                    ParticleData.spawn(location, hitEntityParticle);
                    SoundData.play(() -> location, hitEntitySound);
                    if (source instanceof Player)
                        SoundData.play((Player) source, () -> source.getLocation(), hitEntitySoundSource);
                }

                ParticleData.spawn(location, hitParticle);
                SoundData.play(() -> location, hitSound);
                if (source instanceof Player)
                    SoundData.play((Player) source, () -> source.getLocation(), hitSoundSource);

                if (result == HitResult.BOUNCE && bounce > 0) {
                    bounce(ray);
                } else if (result != HitResult.CONTINUE) {
                    tickContext.remove();
                }
            }
        }

        @Override
        public void bounce(BukkitRayTrace ray) {
            if (!events.call(new Events.Bounce(this, ray)).cancelled) {
                super.bounce(ray);
            }
        }

        @Override
        protected void remove() {
            super.remove();
            events.call(new Events.Remove(this));
        }
    }

    public static void applyTo(BukkitProjectile projectile, StatMap stats) {
        projectile
                .bounce(stats.<NumberDescriptor.Double>val("projectile_bounce").apply())
                .drag(stats.<NumberDescriptor.Double>val("projectile_drag").apply())
                .expansion(stats.<NumberDescriptor.Double>val("projectile_expansion").apply())
                .gravity(stats.<NumberDescriptor.Double>val("projectile_gravity").apply());
    }

    public static final class Events {
        private Events() {}

        public static class Create {
            private final PaperProjectile projectile;
            private final ItemUser user;
            private final Vector3D position;
            private final Vector3D velocity;

            public Create(PaperProjectile projectile, ItemUser user, Vector3D position, Vector3D velocity) {
                this.projectile = projectile;
                this.user = user;
                this.position = position;
                this.velocity = velocity;
            }

            public PaperProjectile projectile() { return projectile; }
            public ItemUser user() { return user; }
            public Vector3D position() { return position; }
            public Vector3D velocity() { return velocity; }
        }

        public static class Step {
            private final PaperProjectile projectile;
            private final TickContext tickContext;
            private final BukkitRayTrace ray;
            private final Vector3D from;
            private final Vector3D delta;
            private final double deltaLength;

            public Step(PaperProjectile projectile, TickContext tickContext, BukkitRayTrace ray, Vector3D from, Vector3D delta, double deltaLength) {
                this.projectile = projectile;
                this.tickContext = tickContext;
                this.ray = ray;
                this.from = from;
                this.delta = delta;
                this.deltaLength = deltaLength;
            }

            public PaperProjectile projectile() { return projectile; }
            public TickContext tickContext() { return tickContext; }
            public BukkitRayTrace ray() { return ray; }
            public Vector3D from() { return from; }
            public Vector3D delta() { return delta; }
            public double deltaLength() { return deltaLength; }
        }

        public static class Tick {
            private final PaperProjectile projectile;
            private final TickContext tickContext;

            public Tick(PaperProjectile projectile, TickContext tickContext) {
                this.projectile = projectile;
                this.tickContext = tickContext;
            }

            public PaperProjectile projectile() { return projectile; }
            public TickContext tickContext() { return tickContext; }
        }

        public static class Collide implements Cancellable {
            private final PaperProjectile projectile;
            private final TickContext tickContext;
            private final BukkitRayTrace ray;
            private final BukkitCollidable collided;
            private HitResult result;
            private boolean cancelled;

            public Collide(PaperProjectile projectile, TickContext tickContext, BukkitRayTrace ray, BukkitCollidable collided, HitResult result) {
                this.projectile = projectile;
                this.tickContext = tickContext;
                this.ray = ray;
                this.collided = collided;
                this.result = result;
            }

            public PaperProjectile projectile() { return projectile; }
            public TickContext tickContext() { return tickContext; }
            public BukkitRayTrace ray() { return ray; }
            public BukkitCollidable collided() { return collided; }

            public HitResult result() { return result; }
            public void result(HitResult result) { this.result = result; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class Bounce implements Cancellable {
            private final PaperProjectile projectile;
            private final BukkitRayTrace ray;
            private boolean cancelled;

            public Bounce(PaperProjectile projectile, BukkitRayTrace ray) {
                this.projectile = projectile;
                this.ray = ray;
            }

            public PaperProjectile projectile() { return projectile; }
            public BukkitRayTrace ray() { return ray; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancel() { cancelled = true; }
        }

        public static class Remove {
            private final PaperProjectile projectile;

            public Remove(PaperProjectile projectile) {
                this.projectile = projectile;
            }

            public PaperProjectile projectile() { return projectile; }
        }
    }
}
