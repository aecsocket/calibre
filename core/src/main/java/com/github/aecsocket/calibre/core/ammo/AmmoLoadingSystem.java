package com.github.aecsocket.calibre.core.ammo;

import com.github.aecsocket.calibre.core.projectile.ProjectileLaunchSystem;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.rule.Rule;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatLists;
import com.gitlab.aecsocket.sokol.core.stat.collection.StatTypes;
import com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.NodeProviderSystem;
import com.gitlab.aecsocket.sokol.core.system.inbuilt.SchedulerSystem;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.gitlab.aecsocket.sokol.core.stat.inbuilt.StringStat.*;

public abstract class AmmoLoadingSystem extends AbstractSystem {
    public static final String ID = "ammo_loading";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public static final StringStat STAT_SLOT_TAG_AMMO = stringStat("slot_tag_ammo");
    public static final StatTypes STATS = StatTypes.of(STAT_SLOT_TAG_AMMO);
    public static final Map<String, Class<? extends Rule>> RULES = CollectionBuilder.map(new HashMap<String, Class<? extends Rule>>())
            .build();

    public abstract class Instance extends AbstractSystem.Instance {
        protected SchedulerSystem<?>.Instance scheduler;
        protected NodeProviderSystem nodeProvider;

        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract AmmoLoadingSystem base();

        public SchedulerSystem<?>.Instance scheduler() { return scheduler; }
        public NodeProviderSystem nodeProvider() { return nodeProvider; }

        @Override
        public void build(StatLists stats) {
            depend(ProjectileLaunchSystem.KEY);
            scheduler = depend(SchedulerSystem.KEY);
            nodeProvider = depend(NodeProviderSystem.class);
            parent.events().register(ItemTreeEvent.Input.class, this::event, listenerPriority);
            parent.events().register(ProjectileLaunchSystem.Events.LaunchChamber.class, this::event, listenerPriority);
        }

        protected boolean chamber(ItemTreeEvent.Input event) {
            return true;
        }

        protected boolean reload(ItemTreeEvent.Input event) {
            return true;
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
                    .put("chamber", () -> handle(event, this::chamber))
                    .put("reload", () -> handle(event, this::reload))
            );
        }

        protected void event(ProjectileLaunchSystem.Events.LaunchChamber event) {
            if (!parent.isRoot())
                return;
            parent.stats().val(ProjectileLaunchSystem.STAT_SLOT_TAG_CHAMBER).ifPresent(tag -> {
                event.chamberParent().chil
            });
        }
    }

    protected InputMapper inputs;

    public AmmoLoadingSystem(int listenerPriority, @Nullable InputMapper inputs) {
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
}
