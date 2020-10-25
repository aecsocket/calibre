package me.aecsocket.calibre.defaults.system.gun;

import com.google.gson.annotations.Expose;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.component.SystemSearchOptions;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.util.user.PlayerItemUser;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.ParticleStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class FireableSystem extends BaseSystem {
    public static final String ID = "fireable";
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("damage", new NumberStat.Double(0d))

            .init("chamber_priority", new NumberStat.Int(0))

            .init("fire_delay", new NumberStat.Long(0L))
            .init("fire_sound", new SoundStat())
            .init("fire_particle", new ParticleStat())
            .init("fire_animation", new ItemAnimationStat())
            .get();

    @Expose(serialize = false) private boolean usable;
    private transient ItemSystem itemSystem;

    public FireableSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public boolean isUsable() { return usable; }
    public void setUsable(boolean usable) { this.usable = usable; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);

        itemSystem = parent.getSystemService(ItemSystem.class);

        EventDispatcher events = tree.getEventDispatcher();
        events.registerListener(ItemEvents.Interact.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    private void onEvent(ItemEvents.Interact event) {
        if (!usable) return;
        if (!itemSystem.isAvailable()) return;
        Map.Entry<CalibreComponentSlot, ProjectileProviderSystem> chamber = Utils.atOr(parent.collectSystems(
                new SystemSearchOptions<>(ProjectileProviderSystem.class)
                .slotTag("chamber")
                .targetPriority(stat("chamber_priority"))
        ), 0);
        if (chamber == null) return;
        ((PlayerItemUser) event.getUser()).getEntity().sendMessage("chamber is " + chamber.getValue().getId());
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public CalibreSystem copy() { return this; }

    public static final class Events {
        private Events() {}

        // TODO
    }
}
