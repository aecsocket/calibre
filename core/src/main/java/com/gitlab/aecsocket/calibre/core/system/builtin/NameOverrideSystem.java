package com.gitlab.aecsocket.calibre.core.system.builtin;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.CalibreSlot;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.rule.Rule;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Objects;

public abstract class NameOverrideSystem extends AbstractSystem {
    public static final String ID = "name_override";
    public static final int LISTENER_PRIORITY = 100001;

    @ConfigSerializable
    private static class Dependencies {
        private ConfigurationNode when;
    }

    @Setting(nodeFromParent = true)
    private Dependencies dependencies;
    @FromMaster private String key;
    @FromMaster private transient Rule when;

    /**
     * Used for registration + deserialization.
     */
    public NameOverrideSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public NameOverrideSystem(NameOverrideSystem o) {
        super(o);
        key = o.key;
        when = o.when;
    }

    @Override public String id() { return ID; }

    public String key() { return key; }
    public void key(String key) { this.key = key; }

    public Rule when() { return when; }
    public void when(Rule when) { this.when = when; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        if (key == null)
            throw new SystemSetupException("No key specified");
        if (dependencies != null) {
            when = deserialize(dependencies.when, Rule.class, "when");
        }
        dependencies = null;
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.NameCreate.class, this::onEvent, listenerPriority);
    }

    protected void onEvent(CalibreComponent.Events.NameCreate<?> event) {
        if (when == null || when.applies(event.component())) {
            event.result(gen(event.locale(), key));
        }
    }

    @Override public abstract NameOverrideSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameOverrideSystem that = (NameOverrideSystem) o;
        return key.equals(that.key) && Objects.equals(when, that.when);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, when);
    }
}
