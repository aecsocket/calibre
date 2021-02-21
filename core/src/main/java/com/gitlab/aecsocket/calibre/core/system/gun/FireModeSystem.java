package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.system.StatRenderer;
import com.gitlab.aecsocket.calibre.core.world.Item;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

public abstract class FireModeSystem extends AbstractSystem {
    public static final String ID = "fire_mode";
    public static final int LISTENER_PRIORITY = 1030;

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode fireModes;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;

    @FromMaster protected transient List<FireMode> fireModes;
    protected transient StatRenderer statRenderer;

    /**
     * Used for registration + deserialization.
     */
    public FireModeSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public FireModeSystem(FireModeSystem o) {
        super(o);
        fireModes = new ArrayList<>(o.fireModes);
    }

    @Override public String id() { return ID; }

    public StatRenderer statRenderer() { return statRenderer; }
    public List<FireMode> fireModes() { return fireModes; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        if (dependencies != null) {
            fireModes = deserialize(dependencies.fireModes, new TypeToken<>(){}, "fireModes");
            dependencies = null;
        }
        require(StatRenderer.class);
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        statRenderer = require(StatRenderer.class);

        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();
        for (FireMode fireMode : fireModes) {
            info.add(gen(locale, "system." + ID + ".header",
                    "name", gen(locale, "fire_mode.full." + fireMode.id)));
            if (statRenderer != null) {
                if (fireMode.activeStats != null)
                    info.addAll(statRenderer.createInfo(locale, fireMode.activeStats, gen(locale, "system." + ID + ".stat_prefix")));
            }
        }

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    @Override public abstract FireModeSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FireModeSystem that = (FireModeSystem) o;
        return fireModes.equals(that.fireModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fireModes);
    }
}
