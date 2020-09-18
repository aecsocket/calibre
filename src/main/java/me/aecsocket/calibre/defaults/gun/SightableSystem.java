package me.aecsocket.calibre.defaults.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ActionSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SightableSystem implements CalibreSystem<Void>,
        ItemEvents.Interact.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("aim_in_delay", new NumberStat.Long(1L))
            .init("aim_in_sound", new DataStat.Sound())
            .init("aim_in_animation", new AnimationStat())

            .init("aim_out_delay", new NumberStat.Long(1L))
            .init("aim_out_sound", new DataStat.Sound())
            .init("aim_out_animation", new AnimationStat())
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient ActionSystem actionSystem;

    public SightableSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public ActionSystem getActionSystem() { return actionSystem; }

    @Override public String getId() { return "sightable"; }
    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) {
        this.parent = parent;
        actionSystem = getSystem(ActionSystem.class);
    }

    @Override
    public @NotNull Collection<Class<?>> getDependencies() {
        return Arrays.asList(
                ActionSystem.class
        );
    }

    @Override
    public void registerListeners(EventDispatcher dispatcher) {
        dispatcher.registerListener(ItemEvents.Interact.class, this, 0);
    }

    @Override
    public void onEvent(ItemEvents.Interact<?> event) {

    }

    @Override public SightableSystem clone() { try { return (SightableSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public SightableSystem copy() { return clone(); }

    public static final class Events {
        private Events() {}

        public static class ToggleAim<L extends ToggleAim.Listener> extends ItemEvents.UserEvent<L> implements ItemEvents.SystemEvent<SightableSystem>, Cancellable {
            public interface Listener { void onEvent(ToggleAim<?> event); }

            private final SightableSystem system;
            private boolean aiming;
            private boolean cancelled;

            public ToggleAim(ItemStack itemStack, EquipmentSlot slot, ItemUser user, SightableSystem system, boolean aiming) {
                super(itemStack, slot, user);
                this.system = system;
                this.aiming = aiming;
            }

            @Override public SightableSystem getSystem() { return system; }

            public boolean isAiming() { return aiming; }
            public void setAiming(boolean aiming) { this.aiming = aiming; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            @Override public void call(Listener listener) { listener.onEvent(this); }
        }
    }
}
