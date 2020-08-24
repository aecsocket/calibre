package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

// TODO remove from prod
public class OtherSystem implements CalibreSystem<Void>, ItemEvents.Hold.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("number_stat_2", new NumberStat.Int(7))
            .get();
    
    private CalibreComponent parent;

    @Override public String getId() { return "other"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public void setParent(CalibreComponent parent) { this.parent = parent; }
    public CalibreComponent getParent() { return parent; }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Hold.class, this, 1);
    }

    @Override
    public void onHold(ItemStack itemStack, Player player, EquipmentSlot hand) {}

    @Override public OtherSystem clone() { try { return (OtherSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public OtherSystem copy() { return clone(); }
}
