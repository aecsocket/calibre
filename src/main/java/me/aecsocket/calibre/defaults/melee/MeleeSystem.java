package me.aecsocket.calibre.defaults.melee;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.action.ActionSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeleeSystem implements CalibreSystem<Void>, ItemEvents.Interact.Listener, ItemEvents.Damage.Listener, ItemEvents.BukkitDamage.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("damage", new NumberStat.Double(0d))

            .init("swing_delay", new NumberStat.Long(1L))
            .init("swing_sound", new DataStat.Sound())
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient ActionSystem actionSystem;

    public MeleeSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "melee"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) {
        this.parent = parent;
        actionSystem = getSystem(ActionSystem.class);
    }

    @Override
    public @NotNull Collection<Class<? extends CalibreSystem<?>>> getDependencies() {
        return Arrays.asList(
                ActionSystem.class
        );
    }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Interact.class, this, 0);
        dispatcher.registerListener(ItemEvents.Damage.class, this, 0);
        dispatcher.registerListener(ItemEvents.BukkitDamage.class, this, 0);
    }

    @Override
    public void onEvent(ItemEvents.Interact event) {
        if (!isCompleteRoot()) return;
        if (event.isRightClick()) return;
        event.getPlayer().sendMessage("interact");
    }

    @Override
    public void onEvent(ItemEvents.Damage event) {
        if (!isCompleteRoot()) return;
        if (!actionSystem.isAvailable()) return;
        // todo
        Player damager = (Player) event.getDamager();
        damager.sendMessage("damaged " + event.getVictim() + " for " + stat("damage"));
        ((LivingEntity) event.getVictim()).damage(stat("damage"));
        actionSystem.startAction(stat("swing_delay"));
        updateItem(damager, event.getSlot());
    }

    @Override
    public void onEvent(ItemEvents.BukkitDamage event) {
        event.getBukkitEvent().setCancelled(true);
    }

    @Override public MeleeSystem clone() { try { return (MeleeSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public MeleeSystem copy() { return clone(); }
}
