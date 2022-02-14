package com.github.aecsocket.calibre.paper.mode;

import com.github.aecsocket.calibre.core.mode.Mode;
import com.github.aecsocket.calibre.core.mode.ModesSystem;
import com.gitlab.aecsocket.sokol.core.system.LoadProvider;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import com.gitlab.aecsocket.sokol.paper.PaperTreeNode;
import com.gitlab.aecsocket.sokol.paper.SokolPlugin;
import com.gitlab.aecsocket.sokol.paper.system.PaperSystem;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;

public final class PaperModesSystem extends ModesSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.empty(ID);

    public final class Instance extends ModesSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperModesSystem base() { return PaperModesSystem.this; }
        @Override public SokolPlugin platform() { return platform; }
    }

    private final SokolPlugin platform;

    public PaperModesSystem(SokolPlugin platform, List<Mode> sights) {
        super(sights);
        this.platform = platform;
    }

    public SokolPlugin platform() { return platform; }

    @Override
    public Instance create(TreeNode node) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, PersistentDataContainer data) {
        return new Instance(node);
    }

    @Override
    public Instance load(PaperTreeNode node, java.lang.reflect.Type type, ConfigurationNode cfg) throws SerializationException {
        return new Instance(node);
    }

    public static ConfigType type(SokolPlugin platform) {
        return cfg -> new PaperModesSystem(platform,
                new ArrayList<>());
    }
}
