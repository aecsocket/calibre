package me.aecsocket.calibre.defaults.melee;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class MeleeSystem implements CalibreSystem<Void>, ItemEvents.Interact.Listener, ItemEvents.Damage.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("damage", new NumberStat.Double(0d))
            .init("swing_speed", new NumberStat.Long(1L))
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;

    public MeleeSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "melee"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void setParent(CalibreComponent parent) { this.parent = parent; }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Interact.class, this, 0);
        dispatcher.registerListener(ItemEvents.Damage.class, this, 0);
    }

    @Override
    public void onEvent(ItemEvents.Interact event) {
        event.getPlayer().sendMessage("interact");
    }

    @Override
    public void onEvent(ItemEvents.Damage event) {
        event.getDamager().sendMessage("damaged " + event.getVictim());
    }

    @Override public MeleeSystem clone() { try { return (MeleeSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public CalibreSystem<Void> copy() { return clone(); }
}
