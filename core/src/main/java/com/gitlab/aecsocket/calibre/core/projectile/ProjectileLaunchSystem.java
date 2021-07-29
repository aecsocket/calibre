package com.gitlab.aecsocket.calibre.core.projectile;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.VectorStat.*;

public abstract class ProjectileLaunchSystem extends AbstractSystem {
    public static final String ID = "projectile_launch";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("slot_tag_chamber", stringStat())
            .put("launch_offset", vector3Stat())
            .put("launch_velocity", doubleStat())

            .put("recoil", vector2Stat())
            .put("recoil_random", vector2Stat())
            .put("recoil_speed", doubleStat())
            .put("recoil_recovery", doubleStat())
            .put("recoil_recovery_speed", doubleStat())
            .put("recoil_recovery_after", longStat())
            .build();
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .build();

    public abstract class Instance extends AbstractSystem.Instance {
        protected SchedulerSystem<?>.Instance scheduler;
        protected ProjectileProvider provider;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract ProjectileLaunchSystem base();

        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }
        public ProjectileProvider provider() { return provider; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            provider = softDepend(ProjectileProvider.class);
            parent.events().register(ItemTreeEvent.Input.class, this::event);
        }

        protected abstract void recoil(ItemUser user, Vector2 recoil, double speed, double recovery,
                                       double recoverySpeed, long recoveryAfter);

        protected void launchProjectile(ProjectileProvider provider, ItemUser user, Vector3 position, Vector3 velocity) {
            provider.launchProjectile(user, position, velocity);
            parent.stats().<Vector2>desc("recoil").ifPresent(recoil -> {
                Vector2 random = parent.stats().<Vector2>desc("recoil_random").orElse(null);
                if (random != null) {
                    Random rng = ThreadLocalRandom.current();
                    recoil = recoil.add(
                            recoil.x() + rng.nextGaussian() * random.x(),
                            recoil.y() + rng.nextGaussian() * random.y()
                    );
                }

                recoil(user, recoil,
                        parent.stats().<Double>desc("recoil_speed").orElse(1d),
                        parent.stats().<Double>desc("recoil_recovery").orElse(0d),
                        parent.stats().<Double>desc("recoil_recovery_speed").orElse(0d),
                        parent.stats().<Long>desc("recoil_recovery_after").orElse(0L));
            });
        }

        protected void launch(ItemTreeEvent.Input event) {
            ItemUser user = event.user();
            Vector3 dir = user.direction();
            Vector3 pos = user.position().add(parent.stats().<Vector3>desc("launch_offset")
                    .map(offset -> Vector3.offset(dir, user instanceof PlayerUser player && player.leftHanded()
                            ? offset.x(-offset.x()) : offset))
                    .orElse(dir));
            Vector3 velocity = dir.multiply(parent.stats().<Double>reqDesc("launch_velocity"));
            // TODO converge, zero

            parent.stats().<String>val("slot_tag_chamber").ifPresentOrElse(tag -> {
                parent.visitSlots((parent, slot, child, path) -> {
                    if (!slot.tagged(tag) || child == null)
                        return;
                    child.system(ProjectileProvider.class).ifPresent(provider -> {
                        // TODO parent.removeChild(slot.key());
                        launchProjectile(provider, user, pos, velocity);
                    });
                });
                event.update();
            }, () -> {
                if (provider == null)
                    throw new IllegalStateException("Trying to launch root component as projectile, but component [" + parent.value().id() + "] has no system [" + ProjectileProvider.class.getSimpleName() + "]");
                launchProjectile(provider, user, pos, velocity);
                event.update(is -> is.add(-1));
            });
        }

        private void handle(ItemTreeEvent.Input event, Consumer<ItemTreeEvent.Input> function) {
            event.cancel();
            if (scheduler.available())
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
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULES; }
    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        inputs = Serializers.require(cfg.node("inputs"), InputMapper.class);
    }

    public static final class Rules {
        private Rules() {}

        private static final String namespace = ID + ":";

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
    }
}
