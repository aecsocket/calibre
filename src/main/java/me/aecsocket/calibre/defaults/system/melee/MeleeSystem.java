package me.aecsocket.calibre.defaults.system.melee;

import com.google.gson.annotations.Expose;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.util.stat.AnimationStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.util.MapInit;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeleeSystem extends BaseSystem {
    public static final String ID = "melee";
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("damage", new NumberStat.Double(0d))

            .init("swing_delay", new NumberStat.Long(0L))
            .init("swing_sound", new SoundStat())
            .init("swing_animation", new AnimationStat())
            .get();

    // TODO dictates if this item is just used for stats or as an actual melee, document this
    @Expose(serialize = false) private boolean usable;
    private transient ItemSystem itemSystem;

    public MeleeSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public boolean isUsable() { return usable; }
    public void setUsable(boolean usable) { this.usable = usable; }

    @Override
    public void initialize(CalibreComponent parent) {
        super.initialize(parent);

        itemSystem = parent.getSystemService(ItemSystem.class);
    }

    @Override public void treeInitialize(CalibreComponent parent, ComponentTree tree) {
        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Attack.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    private void onEvent(ItemEvents.Attack event) {
        if (!usable) return;
        itemSystem.doAction(this, "swing", event.getUser(), event.getSlot());
        // TODO blah blah
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public CalibreSystem copy() { return this; }

    public static final class Events {
        private Events() {}

        // TODO swing event or whatever
    }
}