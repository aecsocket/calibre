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

    @FromMaster protected transient List<Sight> sights;
    protected transient StatRenderer statRenderer;

    /**
     * Used for registration + deserialization.
     */
    public SightSystem() {}

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SightSystem(SightSystem o) {
        super(o);
        sights = new ArrayList<>(o.sights);
    }

    public List<Sight> sights() { return sights; }

    @Override public String id() { return ID; }

    public StatRenderer statRenderer() { return statRenderer; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        if (dependencies != null) {
            sights = deserialize(dependencies.sights, new TypeToken<>(){}, "sights");
            dependencies = null;
        }
    }

    @Override
    public void parentTo(ComponentTree tree, CalibreComponent<?> parent) {
        super.parentTo(tree, parent);
        if (!parent.isRoot()) return;

        EventDispatcher events = tree.events();
        int priority = listenerPriority(1060);
        events.registerListener(CalibreComponent.Events.ItemCreate.class, this::onEvent, priority);

        statRenderer = parent.system(StatRenderer.class);
    }

    protected <I extends Item> void onEvent(CalibreComponent.Events.ItemCreate<I> event) {
        String locale = event.locale();
        List<Component> info = new ArrayList<>();
        for (Sight sight : sights) {
            info.add(gen(locale, "system." + ID + ".header",
                    "name", gen(locale, "sight.full." + sight.id)));

            if (statRenderer != null) {
                Component prefix = gen(locale, "system." + ID + ".stat_prefix");
                if (sight.activeStats != null) {
                    List<Component> toAdd = statRenderer.createInfo(locale, sight.activeStats, prefix);
                    if (toAdd.size() > 0)
                        toAdd.add(0, gen(locale, "system." + ID + ".active"));
                    info.addAll(toAdd);
                }

                if (sight.aimingStats != null) {
                    List<Component> toAdd = statRenderer.createInfo(locale, sight.aimingStats, prefix);
                    if (toAdd.size() > 0)
                        toAdd.add(0, gen(locale, "system." + ID + ".aiming"));
                    info.addAll(toAdd);
                }
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
