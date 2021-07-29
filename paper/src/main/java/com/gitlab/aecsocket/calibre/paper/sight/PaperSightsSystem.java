package com.gitlab.aecsocket.calibre.paper.sight;

import com.gitlab.aecsocket.calibre.core.sight.Sight;
import com.gitlab.aecsocket.calibre.core.sight.SightsSystem;
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

public final class PaperSightsSystem extends SightsSystem implements PaperSystem {
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);
    public static final LoadProvider LOAD_PROVIDER = LoadProvider.empty();

    public final class Instance extends SightsSystem.Instance implements PaperSystem.Instance {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public PaperSightsSystem base() { return PaperSightsSystem.this; }
        @Override public SokolPlugin platform() { return platform; }
    }

    private final SokolPlugin platform;

    public PaperSightsSystem(SokolPlugin platform, List<Sight> sights) {
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
        return cfg -> new PaperSightsSystem(platform,
                new ArrayList<>());
    }
}
