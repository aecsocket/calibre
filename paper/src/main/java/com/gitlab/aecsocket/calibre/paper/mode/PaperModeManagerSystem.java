package com.gitlab.aecsocket.calibre.paper.mode;

import com.gitlab.aecsocket.calibre.core.mode.Mode;
import com.gitlab.aecsocket.calibre.core.mode.ModeManagerSystem;
import com.gitlab.aecsocket.calibre.core.mode.ModesSystem;
import com.gitlab.aecsocket.calibre.paper.CalibrePlugin;
import com.gitlab.aecsocket.minecommons.core.CollectionBuilder;
import com.gitlab.aecsocket.sokol.core.stat.Stat;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.system.util.InputMapper;
import com.gitlab.aecsocket.sokol.core.system.util.SystemPath;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemSlot;
import com.gitlab.aecsocket.sokol.core.wrapper.ItemUser;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;

import static com.gitlab.aecsocket.sokol.paper.stat.SoundsStat.*;
import static com.gitlab.aecsocket.sokol.paper.stat.AnimationStat.*;

public final class PaperModeManagerSystem extends ModeManagerSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final Map<String, Stat<?>> STATS = CollectionBuilder.map(new HashMap<String, Stat<?>>())
            .put(ModeManagerSystem.STATS)
            .put("change_mode_sound", soundsStat())
            .put("change_mode_animation", animationStat())
            .build();
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.ofBoth(STATS, RULES);

    private static final String keyTargetSystem = "target_system";
    private static final String keyTargetIndex = "target_index";

    public final class Instance extends ModeManagerSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent, @Nullable SystemPath targetSystem, int targetIndex) {
            super(parent, targetSystem, targetIndex);
        }

        @Override public PaperModeManagerSystem base() { return PaperModeManagerSystem.this; }
        @Override public SokolPlugin platform() { return platform; }

        @Override
        protected void apply(ItemUser user, ItemSlot slot, Reference<ModesSystem.Instance, Mode> mode) {
            super.apply(user, slot, mode);
        }

        @Override
        protected boolean changeMode(ItemTreeEvent.Input event, int direction) {
            if (super.changeMode(event, direction)) {
                selected().ifPresent(s -> {
                    if (event.updated() && s.selection() instanceof PaperMode mode && mode.applyAnimation() != null)
                        event.update(com.gitlab.aecsocket.sokol.core.wrapper.ItemStack::hideUpdate);
                });
                return true;
            }
            return false;
        }

        @Override
        public PersistentDataContainer save(PersistentDataAdapterContext ctx) throws IllegalArgumentException {
            PersistentDataContainer data = ctx.newPersistentDataContainer();
            if (targetSystem != null) data.set(platform.key(keyTargetSystem), platform.typeSystemPath(), targetSystem);
            data.set(platform.key(keyTargetIndex), PersistentDataType.INTEGER, targetIndex);
            return data;
        }

        @Override
        public void save(java.lang.reflect.Type type, ConfigurationNode node) throws SerializationException {
            node.node(keyTargetSystem).set(targetSystem);
            node.node(keyTargetIndex).set(targetIndex);
        }
    }

    private final SokolPlugin platform;
    private final CalibrePlugin calibre;

    public PaperModeManagerSystem(SokolPlugin platform, CalibrePlugin calibre, int listenerPriority, @Nullable InputMapper inputs) {
        super(listenerPriority, inputs);
        this.platform = platform;
        this.calibre = calibre;
    }

    public SokolPlugin platform() { return platform; }
    public CalibrePlugin calibre() { return calibre; }

    @Override public Map<String, Stat<?>> statTypes() { return STATS; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node, null, 0);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node,
                data.get(platform.key(keyTargetSystem), platform.typeSystemPath()),
                data.getOrDefault(platform.key(keyTargetIndex), PersistentDataType.INTEGER, 0));
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node,
                cfg.node(keyTargetSystem).get(SystemPath.class),
                cfg.node(keyTargetIndex).getInt());
    }

    public static ConfigType type(SokolPlugin platform, CalibrePlugin calibre) {
        return cfg -> new PaperModeManagerSystem(platform, calibre,
                cfg.node(keyListenerPriority).getInt(),
                null);
    }
}
