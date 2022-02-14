package com.github.aecsocket.calibre.core.sight;

import com.github.aecsocket.calibre.core.SelectorHolderSystem;
import com.gitlab.aecsocket.minecommons.core.serializers.Serializers;
import com.gitlab.aecsocket.sokol.core.system.AbstractSystem;
import com.gitlab.aecsocket.sokol.core.tree.TreeNode;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public abstract class SightsSystem extends AbstractSystem {
    public static final String ID = "sights";
    public static final Key<Instance> KEY = new Key<>(ID, Instance.class);

    public abstract class Instance extends AbstractSystem.Instance implements SelectorHolderSystem<Sight> {
        public Instance(TreeNode parent) {
            super(parent);
        }

        @Override public abstract SightsSystem base();
        @Override public List<Sight> selections() { return base().sights; }
    }

    protected final List<Sight> sights;

    public SightsSystem(List<Sight> sights) {
        super(0);
        this.sights = sights;
    }

    public List<Sight> sights() { return sights; }

    @Override public String id() { return ID; }

    @Override
    public void loadSelf(ConfigurationNode cfg) throws SerializationException {
        sights.addAll(Serializers.require(cfg, new TypeToken<List<Sight>>() {}));
    }
}
