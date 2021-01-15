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

public abstract class SightSystem extends AbstractSystem {
    public static final String ID = "sight";

    @ConfigSerializable
    protected static class Dependencies {
        protected ConfigurationNode sights;
    }

    @Setting(nodeFromParent = true)
    protected Dependencies dependencies;

    @FromParent
    protected transient List<Sight> sights;
    @FromParent(fromDefaulted = true)
    protected transient StatDisplaySystem statDisplay;

    public SightSystem() {
        sights = new ArrayList<>();
    }

    public SightSystem(StatDisplaySystem statDisplay) {
        this();
        this.statDisplay = statDisplay;
    }

    public SightSystem(SightSystem o) {
        super(o);
        sights = o.sights == null ? null : new ArrayList<>(o.sights);
    }

    public List<Sight> sights() { return sights; }

    @Override public String id() { return ID; }

    public StatDisplaySystem statDisplay() { return statDisplay; }
    public void statDisplay(StatDisplaySystem statDisplay) { this.statDisplay = statDisplay; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        if (dependencies != null) {
            sights = deserialize(dependencies.sights, new TypeToken<>(){}, "sights");
            dependencies = null;
        }}

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
        for (Sight sight : sights) {
            info.add(localize(locale, "system." + ID + ".header",
                    "name", localize(locale, "sight.full." + sight.id)));

            if (sight.activeStats != null) {
                List<Component> toAdd = statDisplay.createInfo(locale, sight.activeStats);
                if (toAdd.size() > 0)
                    toAdd.add(0, localize(locale, "system." + ID + ".active"));
                info.addAll(toAdd);
            }

            if (sight.aimingStats != null) {
                List<Component> toAdd = statDisplay.createInfo(locale, sight.aimingStats);
                if (toAdd.size() > 0)
                    toAdd.add(0, localize(locale, "system." + ID + ".aiming"));
                info.addAll(toAdd);
            }
        }

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    public abstract SightSystem copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SightSystem that = (SightSystem) o;
        return sights.equals(that.sights);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sights);
    }
}
