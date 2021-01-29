package me.aecsocket.calibre.system.gun;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromMaster;
import me.aecsocket.calibre.system.StatRenderer;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.*;

public abstract class FireModeSystem extends AbstractSystem {
    public static final String ID = "fire_mode";

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode fireModes;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;

    @FromMaster
    protected transient List<FireMode> fireModes;
    protected transient StatRenderer statRenderer;

    /**
     * Used for registration + deserialization.
     */
    public FireModeSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public FireModeSystem(FireModeSystem o) {
        super(o);
        fireModes = new ArrayList<>(o.fireModes);
    }

    public List<FireMode> fireModes() { return fireModes; }

    @Override public String id() { return ID; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        if (dependencies != null) {
            fireModes = deserialize(dependencies.fireModes, new TypeToken<>(){}, "fireModes");
            dependencies = null;
        }
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = setting("listener_priority").getInt(1030);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);

        statRenderer = parent.system(StatRenderer.class);
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

    public abstract FireModeSystem copy();

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
