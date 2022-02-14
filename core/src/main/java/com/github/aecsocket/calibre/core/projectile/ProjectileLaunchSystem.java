package com.github.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.minecommons.core.vector.polar.Coord3;
import com.gitlab.aecsocket.sokol.core.component.Slot;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.util.Availability;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.VectorStat.*;

public abstract class ProjectileLaunchSystem extends AbstractSystem {
    public static final String ID = "projectile_launch";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final StringStat STAT_SLOT_TAG_CHAMBER = stringStat("slot_tag_chamber");
    public static final SVector3 STAT_LAUNCH_OFFSET = vector3Stat("launch_offset");
    public static final SDouble STAT_LAUNCH_VELOCITY = doubleStat("launch_velocity");
    public static final SLong STAT_LAUNCH_DELAY = longStat("launch_delay");
    public static final SLong STAT_LAUNCH_AFTER = longStat("launch_after");
    public static final SInteger STAT_LAUNCHES = intStat("launches");
    public static final SLong STAT_LAUNCH_INTERVAL = longStat("launch_interval");
    public static final SInteger STAT_PROJECTILES = intStat("projectiles");

    public static final SDouble STAT_PROJECTILE_GRAVITY = doubleStat("projectile_gravity");

    public static final SDouble STAT_ZERO_DISTANCE = doubleStat("zero_distance");
    public static final SDouble STAT_CONVERGE_DISTANCE = doubleStat("converge_distance");

    public static final SLong STAT_FAIL_DELAY = longStat("fail_delay");

    public static final SVector2 STAT_SPREAD = vector2Stat("spread");
    public static final SVector2 STAT_SPREAD_PROJECTILE = vector2Stat("spread_projectile");

    public static final SVector2 STAT_RECOIL = vector2Stat("recoil");
    public static final SVector2 STAT_RECOIL_RANDOM = vector2Stat("recoil_random");
    public static final SDouble STAT_RECOIL_SPEED = doubleStat("recoil_speed");
    public static final SDouble STAT_RECOIL_RECOVERY = doubleStat("recoil_recovery");
    public static final SDouble STAT_RECOIL_RECOVERY_SPEED = doubleStat("recoil_recovery_speed");
    public static final SLong STAT_RECOIL_RECOVERY_AFTER = longStat("recoil_recovery_after");

    public static final StatTypes STATS = StatTypes.of(
            STAT_SLOT_TAG_CHAMBER, STAT_LAUNCH_OFFSET, STAT_LAUNCH_VELOCITY, STAT_LAUNCH_DELAY, STAT_LAUNCH_AFTER, STAT_LAUNCHES, STAT_LAUNCH_INTERVAL, STAT_PROJECTILES,
            STAT_PROJECTILE_GRAVITY,
            STAT_ZERO_DISTANCE, STAT_CONVERGE_DISTANCE,
            STAT_FAIL_DELAY,
            STAT_SPREAD, STAT_SPREAD_PROJECTILE,
            STAT_RECOIL, STAT_RECOIL_RANDOM, STAT_RECOIL_SPEED, STAT_RECOIL_RECOVERY, STAT_RECOIL_RECOVERY_SPEED, STAT_RECOIL_RECOVERY_AFTER
    );
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .build();

    public abstract class Instance extends AbstractSystem.Instance implements Availability {
        protected SchedulerSystem<?>.Instance scheduler;
        protected ProjectileProvider provider;
        protected long availableAt;

        public Instance(TreeNode parent, long availableAt) {
            super(parent);
            this.availableAt = availableAt;
        }

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract ProjectileLaunchSystem base();

        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }
        public ProjectileProvider provider() { return provider; }

        @Override public boolean available() { return System.currentTimeMillis() >= availableAt; }
        @Override public void delay(long ms) { availableAt = System.currentTimeMillis() + ms; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            provider = softDepend(ProjectileProvider.class);
            parent.events().register(ItemTreeEvent.Input.class, this::event, listenerPriority);
        }

        protected abstract void recoil(ItemUser user, Vector2 recoil, double speed, double recovery,
                                       double recoverySpeed, long recoveryAfter);

        protected Vector3 rotate(Vector3 val, Vector2 bounds) {
            Random rng = ThreadLocalRandom.current();
            Coord3 coord = val.spherical();
            return coord
                    .yaw(coord.yaw() + rng.nextGaussian() * Math.toRadians(bounds.x()))
                    .pitch(coord.pitch() + rng.nextGaussian() * Math.toRadians(bounds.y()))
                    .cartesian();
        }

        protected void launchProjectiles(ProjectileProvider provider, ItemUser user, Vector3 position, Vector3 direction) {
            Random rng = ThreadLocalRandom.current();
            Vector2 spread = parent.stats().val(STAT_SPREAD).orElse(null);
            Vector2 spreadProjectile = parent.stats().val(STAT_SPREAD_PROJECTILE).orElse(null);
            double launchVelocity = parent.stats().req(STAT_LAUNCH_VELOCITY);
            if (spread != null)
                direction = rotate(direction, spread);

            for (int i = 0; i < parent.stats().val(STAT_PROJECTILES).orElse(1); i++) {
                Vector3 thisDirection = spreadProjectile == null ? direction : rotate(direction, spreadProjectile);
                Vector3 velocity = thisDirection.multiply(launchVelocity);
                provider.launchProjectile(user, position, velocity);
            }

            parent.stats().val(STAT_RECOIL).ifPresent(recoil -> {
                Vector2 random = parent.stats().val(STAT_RECOIL_RANDOM).orElse(null);
                if (random != null) {
                    recoil = recoil.add(
                            recoil.x() + rng.nextGaussian() * random.x(),
                            recoil.y() + rng.nextGaussian() * random.y()
                    );
                }

                recoil(user, recoil,
                        parent.stats().val(STAT_RECOIL_SPEED).orElse(1d),
                        parent.stats().val(STAT_RECOIL_RECOVERY).orElse(0d),
                        parent.stats().val(STAT_RECOIL_RECOVERY_SPEED).orElse(0d),
                        parent.stats().val(STAT_RECOIL_RECOVERY_AFTER).orElse(0L));
            });
        }

        public Function<ItemStack, ItemStack> launchSingle(ItemUser user, ItemSlot slot) {
            Vector3 originalDir = user.direction();
            Vector3 pos = parent.stats().val(STAT_LAUNCH_OFFSET)
                    .map(offset -> user.position().add(Vector3.offset(originalDir, user instanceof PlayerUser player && player.leftHanded()
                            ? offset.x(-offset.x()) : offset)))
                    .orElse(user.position());

            var rDirection = new AtomicReference<>(originalDir);
            // Converge
            parent.stats().val(STAT_CONVERGE_DISTANCE).ifPresent(dst -> {
                Vector3 fwd = user.position().add(rDirection.get().multiply(dst));
                Vector3 delta = fwd.subtract(pos);
                rDirection.set(delta.normalize());
            });
            // Zero
            parent.stats().val(STAT_ZERO_DISTANCE).ifPresent(dst -> {
                rDirection.set(zero(rDirection.get(),
                        parent.stats().req(STAT_LAUNCH_VELOCITY),
                        dst,
                        0,
                        parent.stats().val(STAT_PROJECTILE_GRAVITY).orElse(Projectile.GRAVITY)));
            });
            Vector3 direction = rDirection.get();

            // TODO some better way of doing this overall
            // TODO this is an INSANE mess. please fix later, me.
            AtomicReference<Function<ItemStack, ItemStack>> result = new AtomicReference<>();
            parent.stats().val(STAT_SLOT_TAG_CHAMBER).ifPresentOrElse(tag -> {
                parent.visitSlots((parent, s, child, path) -> {
                    if (!s.tagged(tag) || child == null)
                        return;
                    child.system(ProjectileProvider.class).ifPresent(provider -> {
                        // TODO parent.removeChild(slot.key());
                        var evt = parent.events().call(new Events.LaunchChamber(this, user, slot, pos, direction, parent, s, child, provider));
                        if (!evt.cancelled()) {
                            launchProjectiles(provider, user, evt.position(), evt.direction());
                            result.set(is -> is);
                        }
                    });
                });
            }, () -> {
                if (provider == null)
                    throw new IllegalStateException("Trying to launch root component as projectile, but component [" + parent.value().id() + "] has no system [" + ProjectileProvider.class.getSimpleName() + "]");
                var evt = parent.events().call(new Events.LaunchRoot(this, user, slot, pos, direction));
                if (!evt.cancelled()) {
                    launchProjectiles(provider, user, evt.position(), evt.direction());
                    result.set(is -> is.add(-1));
                }
            });
            if (result.get() == null)
                fail(user, slot, pos);
            else
                runAction(this, "launch", user, slot, pos);
            return result.get();
        }

        protected void launch(ItemTreeEvent.Input event) {
            long after = parent.stats().val(STAT_LAUNCH_AFTER).orElse(0L);
            long interval = parent.stats().val(STAT_LAUNCH_INTERVAL).orElse(0L);
            for (int i = 0; i < parent.stats().val(STAT_LAUNCHES).orElse(1); i++) {
                scheduler.schedule(this, after + i * interval, (self, evt, ctx) -> {
                    var result = self.launchSingle(evt.user(), evt.slot());
                    if (result != null)
                        evt.update(result);
                });
            }
            event.update(ItemStack::hideUpdate);
        }

        protected void fail(ItemUser user, ItemSlot slot, Vector3 position) {
            runAction(this, "fail", user, slot, position);
        }

        private void handle(ItemTreeEvent.Input event, Consumer<ItemTreeEvent.Input> function) {
            event.cancel();
            if (scheduler.available() && available())
                function.accept(event);
        }

        protected void event(ItemTreeEvent.Input event) {
            if (!parent.isRoot())
                return;
            inputs.run(this, event, handlers -> handlers
                    .put("launch", () -> handle(event, this::launch))
            );
        }
    }

    protected InputMapper inputs;

    public ProjectileLaunchSystem(int listenerPriority, @Nullable InputMapper inputs) {
        super(listenerPriority);
        this.inputs = inputs;
    }

    public InputMapper inputs() { return inputs; }

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return STATS; }
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULES; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        inputs = Serializers.require(cfg.node("inputs"), InputMapper.class);
    }

    public static Vector3 zero(Vector3 dir, double v /* speed */, double x /* dist */, double y /* height */, double g /* gravity */) {
        double theta = (float) (
                Math.atan((Math.pow(v, 2) - Math.sqrt(Math.pow(v, 4) - (g * (g*Math.pow(x, 2) + 2*y*Math.pow(v, 2))))) / (g*x))
        );
        if (!Double.isFinite(theta))
            return dir;
        Coord3 coord = dir.spherical();
        return coord.pitch(coord.pitch() - Math.toDegrees(theta)).cartesian();
    }


    public static final class Events {
        private Events() {}

        public static class Base extends TreeEvent.BaseItemEvent implements TreeEvent.SystemEvent<Instance> {
            private final Instance system;
            private final ItemUser user;
            private final ItemSlot slot;

            private Base(Instance system, ItemUser user, ItemSlot slot) {
                this.system = system;
                this.user = user;
                this.slot = slot;
            }

            @Override public Instance system() { return system; }
            @Override public ItemUser user() { return user; }
            @Override public ItemSlot slot() { return slot; }
        }

        public static class Launch extends Base implements Cancellable {
            private Vector3 position;
            private Vector3 direction;
            private boolean cancelled;

            public Launch(Instance system, ItemUser user, ItemSlot slot, Vector3 position, Vector3 direction) {
                super(system, user, slot);
                this.position = position;
                this.direction = direction;
            }

            public Vector3 position() { return position; }
            public void position(Vector3 position) { this.position = position; }

            public Vector3 direction() { return direction; }
            public void direction(Vector3 direction) { this.direction = direction; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static class LaunchChamber extends Launch {
            private final TreeNode chamberParent;
            private final Slot chamberSlot;
            private final TreeNode chamber;
            private final ProjectileProvider provider;

            public LaunchChamber(Instance system, ItemUser user, ItemSlot slot, Vector3 position, Vector3 direction, TreeNode chamberParent, Slot chamberSlot, TreeNode chamber, ProjectileProvider provider) {
                super(system, user, slot, position, direction);
                this.chamberParent = chamberParent;
                this.chamberSlot = chamberSlot;
                this.chamber = chamber;
                this.provider = provider;
            }

            public TreeNode chamberParent() { return chamberParent; }
            public Slot chamberSlot() { return chamberSlot; }
            public TreeNode chamber() { return chamber; }
            public ProjectileProvider provider() { return provider; }
        }

        public static class LaunchRoot extends Launch {
            public LaunchRoot(Instance system, ItemUser user, ItemSlot slot, Vector3 position, Vector3 direction) {
                super(system, user, slot, position, direction);
            }
        }
    }
}
