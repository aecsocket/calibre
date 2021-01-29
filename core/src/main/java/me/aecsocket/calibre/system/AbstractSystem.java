package me.aecsocket.calibre.system;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.unifiedframework.util.Utils;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Field;

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

    protected <T> T deserialize(ConfigurationNode node, TypeToken<T> type, String fieldName) {
        if (node == null)
            return null;
        try {
            return node.get(type);
        } catch (SerializationException e) {
            throw new SystemSetupException("Could not set up " + fieldName, e);
        }
    }

    protected <T> T deserialize(ConfigurationNode node, Class<T> type, String fieldName) {
        return deserialize(node, TypeToken.get(type), fieldName);
    }

    @Override
    public void inherit(CalibreSystem master, boolean fromDefault) {
        if (!master.getClass().equals(getClass())) return;
        for (Field field : Utils.getAllModelFields(getClass())) {
            if (!field.isAnnotationPresent(FromMaster.class))
                continue;
            FromMaster annotation = field.getAnnotation(FromMaster.class);
            if (fromDefault && !annotation.fromDefault())
                continue;
            field.setAccessible(true);
            try {
                field.set(this, field.get(master));
            } catch (IllegalAccessException ignore) {}
        }
    }

    @Override public String toString() { return id(); }
}
