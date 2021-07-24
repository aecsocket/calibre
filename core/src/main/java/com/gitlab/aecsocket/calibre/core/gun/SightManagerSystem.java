package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.stat.StatLists;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.tree.event.TreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;

public abstract class SightManagerSystem extends AbstractSystem {
    public static final String ID = "sight_manager";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("aim_in_after", longStat())
            .put("aim_out_after", longStat())
            .build();
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .put(Rules.Aiming.TYPE, Rules.Aiming.class)
            .put(Rules.InAction.TYPE, Rules.InAction.class)
            .put(Rules.HasTarget.TYPE, Rules.HasTarget.class)
            .build();

    /*

    Let's determine how guns work.

    -> SightManagerSystem: provides zoom capabilities on input, different sights, aiming rule
       -> SightSystem: actually holds the sights that the SightManager uses
    -> SelectorSystem: can select different "mode"s for an item, providing rules
    -> FiringSystem: inputs to fire a gun from a single chamber. No chambering, just shoot projectile.
    -> LoadingSystem: handles ammo containers, reloading ammo containers, loading from ammo into chamber
     */

    public abstract class Instance extends AbstractSystem.Instance {
        public enum Action {
            AIMING_IN, AIMING_OUT
        }

        protected SchedulerSystem<?>.Instance scheduler;
        protected boolean aiming;
        protected @Nullable SystemPath targetSystem;
        protected int targetIndex;

        protected @Nullable Action action;
        protected long actionStart;
        protected long actionEnd;

        protected SightReference sight;

        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex, @Nullable Action action, long actionStart, long actionEnd) {
            super(parent);
            this.aiming = aiming;
            this.targetSystem = targetSystem;
            this.targetIndex = targetIndex;
            this.action = action;
            this.actionStart = actionStart;
            this.actionEnd = actionEnd;
        }

        @Override public abstract SightManagerSystem base();

        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }
        public boolean aiming() { return aiming; }
        public Optional<SystemPath> targetSystem() { return Optional.ofNullable(targetSystem); }
        public int targetIndex() { return targetIndex; }
        public Action action() { return action; }
        public long actionStart() { return actionStart; }
        public long actionEnd() { return actionEnd; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            parent.events().register(ItemTreeEvent.Input.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.ShowItem.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Hold.class, this::event, listenerPriority);

            sight().ifPresentOrElse(
                    sight -> {
                        if (sight.sight.stats() != null)
                            stats.add(sight.sight.stats());
                    },
                    () -> {
                        List<SightReference> sights = collectSights();
                        if (sights.size() > 0) {
                            SightReference ref = sights.get(0);
                            targetSystem = SystemPath.path(ref.system);
                            targetIndex = ref.index;
                        }
                    });
        }

        public boolean actionEnded() { return System.currentTimeMillis() >= actionEnd; }
        public void action(Action action, long ms) {
            this.action = action;
            actionStart = System.currentTimeMillis();
            actionEnd = actionStart + ms;
        }

        public record SightReference(SightsSystem.Instance system, int index, Sight sight) {}

        public Optional<SightReference> sight(SystemPath targetSystem, int targetIndex) {
            return targetSystem.<SightsSystem.Instance>get(parent)
                    .map(sys -> targetIndex >= sys.base().sights.size()
                            ? null
                            : new SightReference(sys, targetIndex, sys.base().sights.get(targetIndex)));
        }

        public Optional<SightReference> sight() {
            if (sight != null)
                return Optional.of(sight);
            Optional<SightReference> result = targetSystem == null ? Optional.empty() : sight(targetSystem, targetIndex);
            result.ifPresent(v -> sight = v);
            return result;
        }

        public List<SightReference> collectSights() {
            List<SightReference> result = new ArrayList<>();
            parent.visitNodes((node, path) -> node.system(SightsSystem.KEY).ifPresent(sys -> {
                List<Sight> sights = sys.base().sights;
                for (int i = 0; i < sights.size(); i++) {
                    result.add(new SightReference(sys, i, sights.get(i)));
                }
            }));
            return result;
        }

        public int sightIndex(List<SightReference> sights) {
            if (targetSystem == null)
                return -1;
            for (int i = 0; i < sights.size(); i++) {
                SightReference ref = sights.get(i);
                if (ref.index == targetIndex && Arrays.equals(ref.system.parent().path(), targetSystem.nodes()))
                    return i;
            }
            return -1;
        }

        protected void applySight(ItemUser user, ItemSlot slot, SightReference sight) {}

        protected void aim(ItemUser user, ItemSlot slot, boolean aiming, Action action, String key) {
            sight().ifPresent(sight -> {
                if (new Events.Aim(this, user, slot, aiming).call())
                    return;
                runAction(scheduler, user, slot, "aim_" + key);
                action(action, parent.stats().<Long>desc("aim_" + key + "_after").orElse(0L));
                if (aiming)
                    applySight(user, slot, sight);
            });
        }

        public void aimIn(ItemUser user, ItemSlot slot) {
            aim(user, slot, true, Action.AIMING_IN, "in");
        }

        public void aimOut(ItemUser user, ItemSlot slot) {
            aim(user, slot, false, Action.AIMING_OUT, "out");
        }

        public void changeSight(ItemUser user, ItemSlot slot, SightReference newSight) {
            if (new Events.ChangeSight(this, user, slot, sight().orElse(null), newSight).call())
                return;
            targetSystem = SystemPath.path(newSight.system);
            targetIndex = newSight.index;
            sight = newSight;
            zoom(user, newSight.sight.zoom());
            applySight(user, slot, newSight);
        }

        public void changeSight(ItemUser user, ItemSlot slot, SystemPath targetSystem, int targetIndex) {
            changeSight(user, slot, sight(targetSystem, targetIndex)
                    .orElseThrow(() -> new IllegalArgumentException("Provided path " + targetIndex + " @ " + targetSystem + " has no sight")));
        }

        public void cycleSight(ItemUser user, ItemSlot slot, int direction) {
            List<SightReference> sights = collectSights();
            if (sights.size() == 0)
                return;
            int newIdx = (sightIndex(sights) + direction) % sights.size();
            if (newIdx < 0) newIdx += sights.size();
            changeSight(user, slot, sights.get(newIdx));
        }

        protected void toggleAiming(ItemTreeEvent.Input event) {
            if ((!aiming && action != Action.AIMING_IN) || (aiming && action == Action.AIMING_OUT))
                aimIn(event.user(), event.slot());
            else
                aimOut(event.user(), event.slot());
            event.cancel();
            event.queueUpdate();
        }

        protected void aimIn(ItemTreeEvent.Input event) {
            if (action != Action.AIMING_OUT && (aiming || action == Action.AIMING_IN))
                return;
            aimIn(event.user(), event.slot());
            event.cancel();
            event.queueUpdate();
        }

        protected void aimOut(ItemTreeEvent.Input event) {
            if (action != Action.AIMING_IN && (!aiming || action == Action.AIMING_OUT))
                return;
            aimOut(event.user(), event.slot());
            event.cancel();
            event.queueUpdate();
        }

        protected void nextSight(ItemTreeEvent.Input event) {
            cycleSight(event.user(), event.slot(), 1);
            event.cancel();
            event.queueUpdate();
        }

        protected void previousSight(ItemTreeEvent.Input event) {
            cycleSight(event.user(), event.slot(), -1);
            event.cancel();
            event.queueUpdate();
        }

        private void event(ItemTreeEvent.Input event) {
            if (!parent.isRoot())
                return;
            if (!scheduler.available())
                return;
            inputs.run(this, event, handlers -> handlers
                    .put("toggle_aiming", () -> toggleAiming(event))
                    .put("aim_in", () -> aimIn(event))
                    .put("aim_out", () -> aimOut(event))
                    .put("next_sight", () -> nextSight(event))
                    .put("previous_sight", () -> previousSight(event))
            );
        }

        protected abstract void zoom(ItemUser user, double zoom);

        private void event(ItemTreeEvent.ShowItem event) {
            if (action == Action.AIMING_IN || (aiming && action == null))
                event.cancel();
        }

        private void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (!event.sync())
                return;

            sight().ifPresentOrElse(sight -> {
                if (action == null)
                    return;
                double progress = Numbers.clamp01((double) (System.currentTimeMillis() - actionStart) / (actionEnd - actionStart));
                double multiplier = action == Action.AIMING_IN ? progress : 1 - progress;
                double zoom = sight.sight.zoom();
                if (zoom >= 0) {
                    zoom(event.user(), zoom * (multiplier * multiplier));
                } else {
                    zoom(event.user(), zoom - ((0.25 / Math.max(multiplier, 1e-3)) - 0.25));
                }
                if (progress >= 1) {
                    aiming = action == Action.AIMING_IN;
                    if (!aiming)
                        zoom(event.user(), 0);
                    action = null;
                    event.queueUpdate(ItemStack::hideUpdate);
                }
            }, () -> {
                if (aiming || action == Action.AIMING_IN)
                    aimOut(event.user(), event.slot());
            });
        }
    }

    protected InputMapper inputs;

    public SightManagerSystem(int listenerPriority, @Nullable InputMapper inputs) {
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

        @ConfigSerializable
        public static final class Aiming extends Rule.Singleton {
            public static final String TYPE = namespace + "aiming";

            public static final Aiming INSTANCE = new Aiming();

            private Aiming() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(sys -> sys.aiming)
                        .orElse(false);
            }
        }

        @ConfigSerializable
        public static final class InAction extends Rule.Singleton {
            public static final String TYPE = namespace + "in_action";

            private final Instance.Action action;

            public InAction(Instance.Action action) {
                this.action = action;
            }

            @SuppressWarnings("ConstantConditions")
            private InAction() { this(null); }

            @Override public String type() { return TYPE; }

            public Instance.Action action() { return action; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(sys -> sys.action == action)
                        .orElse(false);
            }
        }

        @ConfigSerializable
        public static final class HasTarget extends Rule.Singleton {
            public static final String TYPE = namespace + "has_target";

            public static final HasTarget INSTANCE = new HasTarget();

            private HasTarget() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(sys -> sys.targetSystem != null)
                        .orElse(false);
            }
        }
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

        public static final class Aim extends Base implements Cancellable {
            private final boolean aiming;
            private boolean cancelled;

            public Aim(Instance system, ItemUser user, ItemSlot slot, boolean aiming) {
                super(system, user, slot);
                this.aiming = aiming;
            }

            public boolean aiming() { return aiming; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class FinishAim extends Base implements Cancellable {
            private final boolean aiming;
            private boolean cancelled;

            public FinishAim(Instance system, ItemUser user, ItemSlot slot, boolean aiming) {
                super(system, user, slot);
                this.aiming = aiming;
            }

            public boolean aiming() { return aiming; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        public static final class ChangeSight extends Base implements Cancellable {
            private final Instance.@Nullable SightReference oldSight;
            private final Instance.SightReference newSight;
            private boolean cancelled;

            public ChangeSight(Instance system, ItemUser user, ItemSlot slot, Instance.@Nullable SightReference oldSight, Instance.SightReference newSight) {
                super(system, user, slot);
                this.oldSight = oldSight;
                this.newSight = newSight;
            }

            public Optional<Instance.SightReference> oldSight() { return Optional.ofNullable(oldSight); }
            public Instance.SightReference newSight() { return newSight; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
