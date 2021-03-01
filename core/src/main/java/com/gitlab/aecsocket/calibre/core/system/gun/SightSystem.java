package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.component.CalibreComponent;
import com.gitlab.aecsocket.calibre.core.component.ComponentTree;
import com.gitlab.aecsocket.calibre.core.rule.RuledStatCollectionList;
import com.gitlab.aecsocket.calibre.core.system.StatRenderer;
import com.gitlab.aecsocket.calibre.core.world.item.Item;
import io.leangen.geantyref.TypeToken;
import com.gitlab.aecsocket.calibre.core.system.AbstractSystem;
import com.gitlab.aecsocket.calibre.core.system.FromMaster;
import com.gitlab.aecsocket.calibre.core.system.SystemSetupException;
import com.gitlab.aecsocket.unifiedframework.core.event.EventDispatcher;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class SightSystem extends AbstractSystem {
    @ConfigSerializable
    public static class Sight {
        protected String id;
        protected RuledStatCollectionList stats;

        public Sight() {}

        public String id() { return id; }
        public Sight id(String id) { this.id = id; return this; }

        public RuledStatCollectionList stats() { return stats; }
        public Sight stats(RuledStatCollectionList activeStats) { this.stats = activeStats; return this; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Sight sight = (Sight) o;
            return id.equals(sight.id) && Objects.equals(stats, sight.stats);
        }

        @Override public int hashCode() { return Objects.hash(id, stats); }
        @Override public String toString() { return "Sight{" + id + '}'; }
    }
    public static final String ID = "sight";
    public static final int LISTENER_PRIORITY = 1060;

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
    public SightSystem() { super(LISTENER_PRIORITY); }

    /**
     * Used for copying.
     * @param o The other instance.
     */
    public SightSystem(SightSystem o) {
        super(o);
        sights = new ArrayList<>(o.sights);
    }

    @Override public String id() { return ID; }

    public StatRenderer statRenderer() { return statRenderer; }
    public List<Sight> sights() { return sights; }

    @Override public void setup(CalibreComponent<?> parent) throws SystemSetupException {
        super.setup(parent);
        if (dependencies != null) {
            sights = deserialize(dependencies.sights, new TypeToken<>(){}, "sights");
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
        Locale locale = event.locale();
        List<Component> info = new ArrayList<>();
        for (Sight sight : sights) {
            info.add(gen(locale, "system." + ID + ".header",
                    "name", gen(locale, "sight.full." + sight.id)));

            if (statRenderer != null) {
                if (sight.stats != null)
                    info.addAll(statRenderer.createInfo(locale, sight.stats.build(event.component()), gen(locale, "system." + ID + ".stat_prefix")));
            }
        }

        if (info.size() > 0)
            event.item().addInfo(info);
    }

    @Override public abstract SightSystem copy();

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
