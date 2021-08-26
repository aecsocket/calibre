package com.gitlab.aecsocket.calibre.core.sight;

import com.gitlab.aecsocket.calibre.core.SelectorManagerSystem;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Numbers;
import com.gitlab.aecsocket.minecommons.core.event.Cancellable;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector3;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatMap;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
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
import java.util.function.Consumer;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.*;
import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.VectorStat.*;

public abstract class SightManagerSystem extends AbstractSystem {
    public static final String ID = "sight_manager";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final SLong STAT_AIM_IN_AFTER = longStat("aim_in_after");
    public static final SLong STAT_AIM_OUT_AFTER = longStat("aim_out_after");

    public static final SVector2 STAT_SWAY = vector2Stat("sway");
    public static final SLong STAT_SWAY_CYCLE = longStat("sway_cycle");

    public static final SVector3 STAT_REST_OFFSET_A = vector3Stat("rest_offset_a");
    public static final SVector3 STAT_REST_OFFSET_B = vector3Stat("rest_offset_b");

    public static final StatTypes STATS = StatTypes.of(
            STAT_AIM_IN_AFTER, STAT_AIM_OUT_AFTER,
            STAT_SWAY, STAT_SWAY_CYCLE,
            STAT_REST_OFFSET_A, STAT_REST_OFFSET_B
    );
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .put(Rules.Aiming.TYPE, Rules.Aiming.class)
            .put(Rules.InAction.TYPE, Rules.InAction.class)
            .put(Rules.AimingIn.TYPE, Rules.AimingIn.class)
            .put(Rules.AimingOut.TYPE, Rules.AimingOut.class)
            .put(Rules.HasTarget.TYPE, Rules.HasTarget.class)
            .put(Rules.Resting.TYPE, Rules.Resting.class)
            .build();

    public abstract class Instance extends SelectorManagerSystem<SightsSystem.Instance, Sight> {
        public enum Action {
            AIMING_IN, AIMING_OUT
        }

        protected SchedulerSystem<?>.Instance scheduler;
        protected SwayStabilizer swayStabilizer;
        protected boolean aiming;

        protected @Nullable Action action;
        protected long actionStart;
        protected long actionEnd;
        protected boolean resting;

        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex,
                        @Nullable Action action, long actionStart, long actionEnd, boolean resting) {
            super(parent, targetSystem, targetIndex);
            this.aiming = aiming;
            this.action = action;
            this.actionStart = actionStart;
            this.actionEnd = actionEnd;
            this.resting = resting;
        }

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract SightManagerSystem base();

        @Override protected Optional<? extends Sight> fallback() { return SightManagerSystem.this.fallback(); }
        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }
        public SwayStabilizer swayStabilizer() { return swayStabilizer; }
        public boolean aiming() { return aiming; }
        public Action action() { return action; }
        public long actionStart() { return actionStart; }
        public long actionEnd() { return actionEnd; }

        public boolean resting() { return resting; }
        public void resting(boolean resting) { this.resting = resting; }

        @Override protected Key<SightsSystem.Instance> holderKey() { return SightsSystem.KEY; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            swayStabilizer = softDepend(SwayStabilizer.class);
            parent.events().register(ItemTreeEvent.Input.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.ShowItem.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Hold.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Unequip.class, this::event, listenerPriority);

            selected().ifPresentOrElse(
                    sight -> {
                        if (sight.stats() != null)
                            stats.add(sight.stats());
                    },
                    () -> {
                        List<Reference<SightsSystem.Instance, Sight>> sights = collect();
                        if (sights.size() > 0) {
                            var ref = sights.get(0);
                            targetSystem = SystemPath.path(ref.system());
                            targetIndex = ref.index();
                        }
                    });
        }

        public boolean actionEnded() { return System.currentTimeMillis() >= actionEnd; }
        public void action(Action action, long ms) {
            this.action = action;
            actionStart = System.currentTimeMillis();
            actionEnd = actionStart + ms;
        }

        public boolean aimingIn() {
            return (aiming && action != Action.AIMING_OUT) || action == Action.AIMING_IN;
        }

        public boolean aimingOut() {
            return (!aiming && action != Action.AIMING_IN) || action == Action.AIMING_OUT;
        }

        protected void apply(ItemUser user, ItemSlot slot, Sight sight) {}

        protected void aim0(ItemUser user, ItemSlot slot, boolean aiming, Action action, String key, Sight sight) {
            runAction(scheduler, "aim_" + key, user, slot, null);
            action(action, parent.stats().<Long>val("aim_" + key + "_after").orElse(0L));
            if (aiming)
                apply(user, slot, sight);
        }

        protected void aim(ItemUser user, ItemSlot slot, boolean aiming, Action action, String key) {
            selected().ifPresent(sight -> {
                if (new Events.Aim(this, user, slot, aiming).call())
                    return;
                aim0(user, slot, aiming, action, key, sight);
            });
        }

        public void aimIn(ItemUser user, ItemSlot slot) {
            aim(user, slot, true, Action.AIMING_IN, "in");
        }

        public void aimOut(ItemUser user, ItemSlot slot) {
            aim(user, slot, false, Action.AIMING_OUT, "out");
        }

        protected boolean changeSight0(ItemUser user, ItemSlot slot, Reference<SightsSystem.Instance, Sight> newSight) {
            runAction(scheduler, "change_sight", user, slot, null);
            targetSystem = SystemPath.path(newSight.system());
            targetIndex = newSight.index();
            selected = newSight;
            zoom(user, newSight.selection().zoom());
            apply(user, slot, newSight.selection());
            return true;
        }

        public boolean changeSight(ItemUser user, ItemSlot slot, Reference<SightsSystem.Instance, Sight> newSight) {
            if (new Events.ChangeSight(this, user, slot, selectedRef().orElse(null), newSight).call())
                return false;
            return changeSight0(user, slot, newSight);
        }

        public boolean changeSight(ItemUser user, ItemSlot slot, SystemPath targetSystem, int targetIndex) {
            return changeSight(user, slot, selectedRef(targetSystem, targetIndex)
                    .orElseThrow(() -> new IllegalArgumentException("Provided path " + targetIndex + " @ " + targetSystem + " has no selection")));
        }

        public boolean cycleSight(ItemUser user, ItemSlot slot, int direction) {
            var sights = collect();
            if (sights.size() <= 1)
                return false;
            int newIdx = (selectedIndex(sights) + direction) % sights.size();
            if (newIdx < 0) newIdx += sights.size();
            return changeSight(user, slot, sights.get(newIdx));
        }

        protected void toggleAiming(ItemTreeEvent.Input event) {
            if (aimingOut())
                aimIn(event.user(), event.slot());
            else
                aimOut(event.user(), event.slot());
            event.update();
        }

        protected void aimIn(ItemTreeEvent.Input event) {
            if (aimingIn())
                return;
            aimIn(event.user(), event.slot());
            event.update();
        }

        protected void aimOut(ItemTreeEvent.Input event) {
            if (aimingOut())
                return;
            aimOut(event.user(), event.slot());
            event.update();
        }

        protected boolean changeSight(ItemTreeEvent.Input event, int direction) {
            event.cancel();
            if (cycleSight(event.user(), event.slot(), direction)) {
                event.update();
                return true;
            }
            return false;
        }

        protected boolean nextSight(ItemTreeEvent.Input event) {
            return changeSight(event, 1);
        }

        protected boolean previousSight(ItemTreeEvent.Input event) {
            return changeSight(event, -1);
        }

        protected abstract void zoom(ItemUser user, double zoom);
        protected abstract void sway(ItemUser user, Vector2 vector);
        protected abstract boolean resting(ItemUser user, Vector3 offsetA, Vector3 offsetB);

        private void handle(ItemTreeEvent.Input event, Consumer<ItemTreeEvent.Input> function) {
            event.cancel();
            if (scheduler.available())
                function.accept(event);
        }

        protected void event(ItemTreeEvent.Input event) {
            if (!parent.isRoot())
                return;
            inputs.run(this, event, handlers -> handlers
                    .put("toggle_aiming", () -> handle(event, this::toggleAiming))
                    .put("aim_in", () -> handle(event, this::aimIn))
                    .put("aim_out", () -> handle(event, this::aimOut))
                    .put("next_sight", () -> handle(event, this::nextSight))
                    .put("previous_sight", () -> handle(event, this::previousSight))
            );
        }

        protected void event(ItemTreeEvent.ShowItem event) {
            if (action == Action.AIMING_IN || (aiming && action == null))
                event.cancel();
        }

        protected void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (event.sync()) {
                selected().ifPresentOrElse(sight -> {
                    parent.stats().val(STAT_REST_OFFSET_A).ifPresent(offset ->
                        resting = resting(event.user(), offset, parent.stats().req(STAT_REST_OFFSET_B)));

                    if (action != null) {
                        double progress = Numbers.clamp01((double) (System.currentTimeMillis() - actionStart) / (actionEnd - actionStart));
                        double mult = action == Action.AIMING_IN ? progress : 1 - progress;
                        double zoom = sight.zoom();

                        if (zoom < 0) {
                            double val = zoom - ((0.2 / Math.max(mult, 1e-3)) - 0.2);
                            zoom(event.user(), val);
                        } else if (zoom >= 0.1) {
                            double val = 0.1 + ((zoom - 0.1) * (mult * mult));
                            zoom(event.user(), val);
                        } else
                            zoom(event.user(), 0);

                        if (progress >= 1) {
                            aiming = action == Action.AIMING_IN;
                            if (!aiming)
                                zoom(event.user(), 0);
                            action = null;
                            event.update(ItemStack::hideUpdate);
                            new Events.FinishAim(this, event.user(), event.slot(), aiming).call();
                        }
                    }

                    if (aiming)
                        event.update();
                }, () -> {
                    if (aiming || action == Action.AIMING_IN)
                        aimOut(event.user(), event.slot());
                });
            } else {
                if (aimingIn()) {
                    StatMap stats = parent.stats();
                    stats.val(STAT_SWAY).ifPresent(sway -> stats.val(STAT_SWAY_CYCLE).ifPresent(swayCycle -> {
                        double angle = ((double) System.currentTimeMillis() / (swayCycle / 2d)) * Math.PI;
                        Vector2 vector = new Vector2(
                                sway.x() * Math.cos(angle),
                                sway.y() * Math.sin(angle)
                        );
                        if (swayStabilizer != null)
                            vector = vector.multiply(swayStabilizer.stabilization(event));
                        if (vector.manhattanLength() > 0)
                            sway(event.user(), vector);
                    }));
                }
            }
        }

        protected void event(ItemTreeEvent.Unequip event) {
            if (!parent.isRoot())
                return;
            aiming = false;
            action = null;
            zoom(event.user(), 0.1);
            event.update();
        }
    }

    protected InputMapper inputs;
    protected @Nullable Sight fallback;

    public SightManagerSystem(int listenerPriority, @Nullable InputMapper inputs, @Nullable Sight fallback) {
        super(listenerPriority);
        this.inputs = inputs;
        this.fallback = fallback;
    }

    public InputMapper inputs() { return inputs; }
    public Optional<? extends Sight> fallback() { return Optional.ofNullable(fallback); }

    @Override public String id() { return ID; }
    @Override public StatTypes statTypes() { return STATS; }
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULES; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        inputs = Serializers.require(cfg.node("inputs"), InputMapper.class);
        fallback = cfg.node("fallback").get(Sight.class);
    }

    public static final class Rules {
        private Rules() {}

        @ConfigSerializable
        public static final class Aiming extends Rule.Singleton {
            public static final String TYPE = "aiming";

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
            public static final String TYPE = "in_action";

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
        public static final class AimingIn extends Rule.Singleton {
            public static final String TYPE = "aiming_in";

            public static final Aiming INSTANCE = new Aiming();

            private AimingIn() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(Instance::aimingIn)
                        .orElse(false);
            }
        }

        @ConfigSerializable
        public static final class AimingOut extends Rule.Singleton {
            public static final String TYPE = "aiming_out";

            public static final Aiming INSTANCE = new Aiming();

            private AimingOut() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(Instance::aimingOut)
                        .orElse(false);
            }
        }

        @ConfigSerializable
        public static final class HasTarget extends Rule.Singleton {
            public static final String TYPE = "has_target";

            public static final HasTarget INSTANCE = new HasTarget();

            private HasTarget() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(sys -> sys.targetSystem().isPresent())
                        .orElse(false);
            }
        }

        @ConfigSerializable
        public static final class Resting extends Rule.Singleton {
            public static final String TYPE = "resting";

            public static final Resting INSTANCE = new Resting();

            private Resting() {}

            @Override public String type() { return TYPE; }

            @Override
            public boolean applies(TreeNode node) {
                return node.system(KEY)
                        .map(sys -> sys.resting)
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

        public static final class FinishAim extends Base {
            private final boolean aiming;

            public FinishAim(Instance system, ItemUser user, ItemSlot slot, boolean aiming) {
                super(system, user, slot);
                this.aiming = aiming;
            }

            public boolean aiming() { return aiming; }
        }

        public static final class ChangeSight extends Base implements Cancellable {
            private final Instance.@Nullable Reference<SightsSystem.Instance, Sight> oldSight;
            private final Instance.Reference<SightsSystem.Instance, Sight> newSight;
            private boolean cancelled;

            public ChangeSight(Instance system, ItemUser user, ItemSlot slot, Instance.@Nullable Reference<SightsSystem.Instance, Sight> oldSight, Instance.Reference<SightsSystem.Instance, Sight> newSight) {
                super(system, user, slot);
                this.oldSight = oldSight;
                this.newSight = newSight;
            }

            public Optional<Instance.Reference<SightsSystem.Instance, Sight>> oldSight() { return Optional.ofNullable(oldSight); }
            public Instance.Reference<SightsSystem.Instance, Sight> newSight() { return newSight; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
