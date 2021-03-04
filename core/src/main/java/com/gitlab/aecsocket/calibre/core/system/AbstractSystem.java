package com.gitlab.aecsocket.calibre.core.system;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.core.util.StatCollection;
import com.gitlab.aecsocket.unifiedframework.core.util.Utils;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Field;

public abstract class AbstractSystem implements CalibreSystem {
    protected transient CalibreComponent<?> parent;
    @FromMaster protected final int listenerPriority;

    public AbstractSystem(int listenerPriority) {
        this.listenerPriority = listenerPriority;
    }

    public AbstractSystem(AbstractSystem o) {
        parent = o.parent;
        listenerPriority = o.listenerPriority;
    }

    @Override public void id(String s) { throw new UnsupportedOperationException(); }

    public int listenerPriority() { return listenerPriority; }

    @Override public CalibreComponent<?> parent() { return parent; }

    @Override
    public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        this.parent = parent;
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        this.parent = parent;
    }

    protected <S> S require(Class<S> type) {
        S system = parent.system(type);
        if (system == null)
            throw new SystemSetupException("System [" + id() + "] depends on system of type [" + type.getName() + "]");
        return system;
    }

    protected <T> T deserialize(ConfigurationNode node, TypeToken<T> type) {
        if (node == null)
            return null;
        try {
            return node.get(type);
        } catch (SerializationException e) {
            throw new SystemSetupException("Could not set up " + node.key(), e);
        }
    }


    protected <T> T deserialize(ConfigurationNode node, Class<T> type) {
        return deserialize(node, TypeToken.get(type));
    }

    protected void combine(StatCollection base, StatCollection add) {
        if (add != null)
            base.combine(add);
    }

    // TODO stuff with this, since we don't deserialize configurate tree anymore
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
            } catch (IllegalAccessException e) { e.printStackTrace(); }
        }
    }

    @Override public String toString() { return id(); }
}
