package me.aecsocket.calibre.system;

import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.util.StatCollection;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public abstract class AbstractSystem implements CalibreSystem {
    protected transient CalibreComponent<?> parent;

    public AbstractSystem() {}

    public AbstractSystem(AbstractSystem o) {
        parent = o.parent;
    }

    @Override public void id(String s) {}

    @Override public CalibreComponent<?> parent() { return parent; }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        this.parent = parent;
    }

    protected StatCollection deserializeStats(ConfigurationNode node, String fieldName) {
        if (node == null)
            return null;
        try {
            return node.get(StatCollection.class);
        } catch (SerializationException e) {
            throw new SystemSetupException("Could not set up " + fieldName, e);
        }
    }

    @Override public String toString() { return id(); }
}
