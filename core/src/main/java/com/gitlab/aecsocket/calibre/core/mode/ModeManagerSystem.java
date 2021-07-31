package com.gitlab.aecsocket.calibre.core.mode;

import com.gitlab.aecsocket.calibre.core.SelectorManagerSystem;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
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
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.PrimitiveStat.longStat;

public abstract class ModeManagerSystem extends AbstractSystem {
    public static final String ID = "mode_manager";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put("change_mode_delay", longStat())
            .build();
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .put(Rules.HasTarget.TYPE, Rules.HasTarget.class)
            .build();

    public abstract class Instance extends SelectorManagerSystem<ModesSystem.Instance, Mode> {
        protected SchedulerSystem<?>.Instance scheduler;

        public Instance(TreeNode parent, @Nullable SystemPath targetSystem, int targetIndex) {
            super(parent, targetSystem, targetIndex);
            this.targetSystem = targetSystem;
            this.targetIndex = targetIndex;
        }

        @Override public abstract ModeManagerSystem base();

        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }

        @Override protected Key<ModesSystem.Instance> holderKey() { return ModesSystem.KEY; }

        @Override
        public void build(StatLists stats) {
            scheduler = depend(SchedulerSystem.KEY);
            parent.events().register(ItemTreeEvent.Input.class, this::event, listenerPriority);
            parent.events().register(ItemTreeEvent.Hold.class, event -> {
                // TODO debug
                ((PlayerUser) event.user()).sendActionBar(Component.text("Mode: " + selected().map(r -> r.selection().id()).orElse("none")));
            });

            selected().ifPresentOrElse(
                    mode -> {
                        if (mode.selection().stats() != null)
                            stats.add(mode.selection().stats());
                    },
                    () -> {
                        var modes = collect();
                        if (modes.size() > 0) {
                            var ref = modes.get(0);
                            targetSystem = SystemPath.path(ref.system());
                            targetIndex = ref.index();
                        }
                    });
        }

        protected void apply(ItemUser user, ItemSlot slot, Reference<ModesSystem.Instance, Mode> mode) {}

        protected boolean changeMode0(ItemUser user, ItemSlot slot, Reference<ModesSystem.Instance, Mode> newMode) {
            runAction(scheduler, "change_mode", user, slot, null);
            targetSystem = SystemPath.path(newMode.system());
            targetIndex = newMode.index();
            selected = newMode;
            apply(user, slot, newMode);
            return true;
        }

        public boolean changeMode(ItemUser user, ItemSlot slot, Reference<ModesSystem.Instance, Mode> newMode) {
            if (new Events.ChangeMode(this, user, slot, selected().orElse(null), newMode).call())
                return false;
            return changeMode0(user, slot, newMode);
        }

        public void changeMode(ItemUser user, ItemSlot slot, SystemPath targetSystem, int targetIndex) {
            changeMode(user, slot, selected(targetSystem, targetIndex)
                    .orElseThrow(() -> new IllegalArgumentException("Provided path " + targetIndex + " @ " + targetSystem + " has no selection")));
        }

        public boolean cycleMode(ItemUser user, ItemSlot slot, int direction) {
            var modes = collect();
            if (modes.size() <= 1)
                return false;
            int newIdx = (selectedIndex(modes) + direction) % modes.size();
            if (newIdx < 0) newIdx += modes.size();
            return changeMode(user, slot, modes.get(newIdx));
        }

        protected boolean changeMode(ItemTreeEvent.Input event, int direction) {
            if (cycleMode(event.user(), event.slot(), direction)) {
                event.update();
                return true;
            }
            return false;
        }

        protected boolean nextMode(ItemTreeEvent.Input event) {
            return changeMode(event, 1);
        }

        protected boolean previousMode(ItemTreeEvent.Input event) {
            return changeMode(event, -1);
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
                    .put("next_mode", () -> handle(event, this::nextMode))
                    .put("previous_mode", () -> handle(event, this::previousMode))
            );
        }
    }

    protected InputMapper inputs;

    public ModeManagerSystem(int listenerPriority, @Nullable InputMapper inputs) {
        super(listenerPriority);
        this.inputs = inputs;
    }

    public InputMapper inputs() { return inputs; }

    @Override public String id() { return ID; }
    @Override public Map<String, Stat<?>> statTypes() { return STATS; }
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULES; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        inputs = Serializers.require(cfg.node("inputs"), InputMapper.class);
    }

    public static final class Rules {
        private Rules() {}

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

        public static final class ChangeMode extends Base implements Cancellable {
            private final Instance.@Nullable Reference<ModesSystem.Instance, Mode> oldMode;
            private final Instance.Reference<ModesSystem.Instance, Mode> newMode;
            private boolean cancelled;

            public ChangeMode(Instance system, ItemUser user, ItemSlot slot, Instance.@Nullable Reference<ModesSystem.Instance, Mode> oldMode, Instance.Reference<ModesSystem.Instance, Mode> newMode) {
                super(system, user, slot);
                this.oldMode = oldMode;
                this.newMode = newMode;
            }

            public Optional<Instance.Reference<ModesSystem.Instance, Mode>> oldMode() { return Optional.ofNullable(oldMode); }
            public Instance.Reference<ModesSystem.Instance, Mode> newMode() { return newMode; }

            @Override public boolean cancelled() { return cancelled; }
            @Override public void cancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
