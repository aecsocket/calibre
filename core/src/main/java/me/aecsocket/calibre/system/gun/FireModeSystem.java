package me.aecsocket.calibre.system.gun;

import io.leangen.geantyref.TypeToken;
import me.aecsocket.calibre.component.CalibreComponent;
import me.aecsocket.calibre.component.ComponentTree;
import me.aecsocket.calibre.system.AbstractSystem;
import me.aecsocket.calibre.system.FromParent;
import me.aecsocket.calibre.system.SystemSetupException;
import me.aecsocket.calibre.system.builtin.StatDisplaySystem;
import me.aecsocket.calibre.world.Item;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class FireModeSystem extends AbstractSystem {
    public static final String ID = "fire_mode";

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode fireModes;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;

    @FromParent
    protected transient List<FireMode> fireModes;
    @FromParent(fromDefaulted = true)
    protected transient StatDisplaySystem statDisplay;

    public FireModeSystem() {}

    public FireModeSystem(StatDisplaySystem statDisplay) {
        this.statDisplay = statDisplay;
    }

    public FireModeSystem(FireModeSystem o) {
        super(o);
        fireModes = o.fireModes == null ? null : new ArrayList<>(o.fireModes);
    }

    public List<FireMode> fireModes() { return fireModes; }

    @Override public String id() { return ID; }

    public StatDisplaySystem statDisplay() { return statDisplay; }
    public void statDisplay(StatDisplaySystem statDisplay) { this.statDisplay = statDisplay; }

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
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, listenerPriority());
    }

    protected abstract int listenerPriority();

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();
        for (FireMode fireMode : fireModes) {
            info.add(localize(locale, "system." + ID + ".header",
                    "name", localize(locale, "fire_mode.full." + fireMode.id)));
            if (fireMode.activeStats != null)
                info.addAll(statDisplay.createInfo(locale, fireMode.activeStats));
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
