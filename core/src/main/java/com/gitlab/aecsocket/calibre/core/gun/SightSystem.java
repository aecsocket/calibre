package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.rule.Visitor;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.PlayerUser;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class SightSystem extends AbstractSystem {
    public static final String ID = "sight";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .put(Rules.Aiming.TYPE, Rules.Aiming.class)
            .put(Rules.HasTarget.TYPE, Rules.HasTarget.class)
            .build();

    /*

    Let's determine how guns work.

    -> SightSystem: provides zoom capabilities on input, different sights, aiming rule
    -> SelectorSystem: can select different "mode"s for an item, providing rules
    -> FiringSystem: inputs to fire a gun from a single chamber. No chambering, just shoot projectile.
    -> LoadingSystem: handles ammo containers, reloading ammo containers, loading from ammo into chamber
     */

    protected final InputMapper inputs;

    public SightSystem(int listenerPriority, InputMapper inputs) {
        super(listenerPriority);
        this.inputs = inputs;
    }

    public InputMapper inputs() { return inputs; }

    public abstract class Instance extends AbstractSystem.Instance {
        protected SchedulerSystem<?>.Instance scheduler;
        protected boolean aiming;
        protected SystemPath targetSystem;
        protected int targetIndex;

        public Instance(TreeNode parent, boolean aiming, @Nullable SystemPath targetSystem, int targetIndex) {
            super(parent);
            this.aiming = aiming;
            this.targetSystem = targetSystem;
            this.targetIndex = targetIndex;
        }

        public boolean aiming() { return aiming; }
        public void aiming(boolean aiming) { this.aiming = aiming; }

        public Optional<SystemPath> targetSystem() { return Optional.ofNullable(targetSystem); }
        public void targetSystem(SystemPath targetSystem) { this.targetSystem = targetSystem; }

        public int targetIndex() { return targetIndex; }
        public void targetIndex(int targetIndex) { this.targetIndex = targetIndex; }

        @Override
        public void build() {
            scheduler = depend(SchedulerSystem.KEY);
            parent.events().register(ItemTreeEvent.InputEvent.class, this::event, listenerPriority);
        }

        private void event(ItemTreeEvent.InputEvent event) {
            if (!parent.isRoot())
                return;
            if (!scheduler.available())
                return;
            inputs.run(this, event, handlers -> handlers
                    .put("toggle_aiming", () -> {
                        scheduler.delay(1000);
                        scheduler.schedule(this, 1000, (self, evt, ctx) -> {
                            self.aiming = !self.aiming;
                            evt.queueUpdate();
                        });
                        event.queueUpdate();
                    })
                    .put("aim_in", () -> {
                        if (aiming)
                            return;
                        aiming = true;
                        event.queueUpdate();
                    })
                    .put("aim_out", () -> {
                        if (!aiming)
                            return;
                        aiming = false;
                        event.queueUpdate();
                    })
            );
            ((PlayerUser) event.user()).sendMessage(Component.text("aiming? " + aiming));
        }
    }

    @Override public String id() { return ID; }
    @Override public Map<String, Class<? extends Rule>> ruleTypes() { return RULES; }

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

            @Override
            public void visit(Visitor visitor) {
                visitor.visit(this);
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

            @Override
            public void visit(Visitor visitor) {
                visitor.visit(this);
            }
        }
    }
}
