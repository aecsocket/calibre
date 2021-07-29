package com.gitlab.aecsocket.calibre.core.mode;

import com.gitlab.aecsocket.calibre.core.SelectorHolderSystem;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public abstract class ModesSystem extends AbstractSystem {
    public static final String ID = "modes";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public abstract class Instance extends AbstractSystem.Instance implements SelectorHolderSystem<Mode> {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract ModesSystem base();
        @Override public List<Mode> selections() { return base().modes; }
    }

    protected final List<Mode> modes;

    public ModesSystem(List<Mode> modes) {
        super(0);
        this.modes = modes;
    }

    public List<Mode> sights() { return modes; }

    @Override public String id() { return ID; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        modes.addAll(Serializers.require(cfg, new TypeToken<List<Mode>>() {}));
    }
}
