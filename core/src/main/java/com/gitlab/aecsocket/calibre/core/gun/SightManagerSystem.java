package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.Numbers;
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
import com.gitlab.aecsocket.sokol.core.wrapper.ItemStack;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.NumberStat.*;

public abstract class SightManagerSystem extends AbstractSystem {
    public static final String ID = "sight_manager";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("aim_in_delay", longStat(0L))
            .put("aim_out_delay", longStat(0L))
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
        public void aiming(boolean aiming) { this.aiming = aiming; }

        public Optional<SystemPath> targetSystem() { return Optional.ofNullable(targetSystem); }
        public void targetSystem(SystemPath targetSystem) { this.targetSystem = targetSystem; }

        public int targetIndex() { return targetIndex; }
        public void targetIndex(int targetIndex) { this.targetIndex = targetIndex; }

        public Action action() { return action; }
        public void action(Action action) { this.action = action; }

        public long actionStart() { return actionStart; }
        public void actionStart(long actionStart) { this.actionStart = actionStart; }

        public long actionEnd() { return actionEnd; }
        public void actionEnd(long actionEnd) { this.actionEnd = actionEnd; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            parent.events().register(ItemTreeEvent.InputEvent.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Hold.class, this::event, listenerPriority);

            sight().ifPresentOrElse(
                    sight -> stats.add(sight.stats()),
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

        public Optional<Sight> sight() {
            return targetSystem == null ? Optional.empty() : targetSystem.<SightsSystem.Instance>get(parent)
                    .map(sys -> targetIndex >= sys.base().sights.size()
                            ? null
                            : sys.base().sights.get(targetIndex));
        }

        public record SightReference(SightsSystem.Instance system, int index, Sight sight) {}

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

        private void aim(Action action, String key) {
            if (sight().isEmpty())
                return;
            action(action, parent.stats().<Long>desc("aim_" + key + "_delay").orElse(0L));
        }

        public void aimIn() {
            aim(Action.AIMING_IN, "in");
        }

        public void aimOut() {
            aim(Action.AIMING_OUT, "out");
        }

        private void event(ItemTreeEvent.InputEvent event) {
            if (!parent.isRoot())
                return;
            if (!scheduler.available())
                return;
            inputs.run(this, event, handlers -> handlers
                    .put("toggle_aiming", () -> {
                        if ((!aiming && action != Action.AIMING_IN) || (aiming && action == Action.AIMING_OUT))
                            aimIn();
                        else
                            aimOut();
                        event.cancel();
                        event.queueUpdate();
                    })
                    .put("aim_in", () -> {
                        if (aiming || action == Action.AIMING_IN)
                            return;
                        aimIn();
                        event.cancel();
                        event.queueUpdate();
                    })
                    .put("aim_out", () -> {
                        if (!aiming || action == Action.AIMING_OUT)
                            return;
                        aimOut();
                        event.cancel();
                        event.queueUpdate();
                    })
                    .put("next_sight", () -> {
                        List<SightReference> sights = collectSights();
                        if (sights.size() == 0)
                            return;
                        int newIdx = (sightIndex(sights) + 1) % sights.size();
                        SightReference ref = sights.get(newIdx);
                        targetSystem = SystemPath.path(ref.system);
                        targetIndex = ref.index;
                        zoom(event.user(), ref.sight.zoom());
                        event.cancel();
                        event.queueUpdate();
                    })
                    .put("previous_sight", () -> {
                        List<SightReference> sights = collectSights();
                        if (sights.size() == 0)
                            return;
                        int newIdx = sightIndex(sights) - 1;
                        if (newIdx < 0) newIdx += sights.size();
                        SightReference ref = sights.get(newIdx);
                        targetSystem = SystemPath.path(ref.system);
                        targetIndex = ref.index;
                        zoom(event.user(), ref.sight.zoom());
                        event.cancel();
                        event.queueUpdate();
                    })
            );
        }

        protected abstract void zoom(ItemUser user, double zoom);

        private void event(ItemTreeEvent.Hold event) {
            if (!parent.isRoot())
                return;
            if (action == null)
                return;

            if (!event.sync())
                return;
            sight().ifPresent(sight -> {
                double progress = (double) (System.currentTimeMillis() - actionStart) / (actionEnd - actionStart);
                zoom(event.user(), sight.zoom() * Numbers.sqr(Numbers.clamp01(action == Action.AIMING_IN ? progress : 1 - progress)));
                if (progress >= 1) {
                    aiming = action == Action.AIMING_IN;
                    action = null;
                    event.queueUpdate(ItemStack::hideUpdate);
                }
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
}
