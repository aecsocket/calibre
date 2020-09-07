package me.aecsocket.calibre.defaults.melee;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.ActionSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.defaults.service.damage.CalibreDamageService;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MeleeSystem implements CalibreSystem<MeleeSystem>,
        ItemEvents.Interact.Listener,
        ItemEvents.Damage.Listener,
        ItemEvents.BukkitDamage.Listener {
    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("damage", new NumberStat.Double(0d))
            .init("backstab_threshold", new NumberStat.Double(0d))
            .init("backstab_multiplier", new NumberStat.Double(1d))

            .init("swing_delay", new NumberStat.Long(1L))
            .init("swing_sound", new DataStat.Sound())
            .init("swing_animation", new AnimationStat())
            .get();

    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient ActionSystem actionSystem;
    private int lastDamage;

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
        if (!actionSystem.isAvailable()) return;

        Player player = event.getPlayer();

        if (callEvent(new Events.Swing(
                event.getItemStack(),
                event.getSlot(),
                player,
                null,
                0
        )).cancelled)
            return;

        actionSystem.startAction(
                stat("swing_delay"),
                player.getLocation(), stat("swing_sound"), null,
                player, event.getSlot(), stat("swing_animation"));
        lastDamage = Bukkit.getCurrentTick();
        updateItem(player, event.getSlot(), stat("swing_animation"));
    }

    @Override
    public void onEvent(ItemEvents.Damage event) {
        if (!isCompleteRoot()) return;
        if (!actionSystem.isAvailable()) return;
        if (lastDamage == Bukkit.getCurrentTick()) return;

        LivingEntity damager = event.getDamager();
        Entity victim = event.getVictim();
        Location location = damager.getLocation();

        double damage = stat("damage");
        double dot = location.getDirection().dot(victim.getLocation().getDirection());
        if (dot > (double) stat("backstab_threshold"))
            damage *= (double) stat("backstab_multiplier");

        Events.Swing iEvent = new Events.Swing(
                event.getItemStack(),
                event.getSlot(),
                damager,
                victim,
                damage
        );
        if (callEvent(iEvent).cancelled)
            return;

        damage = iEvent.damage;

        actionSystem.startAction(
                stat("swing_delay"),
                location, stat("swing_sound"), null,
                damager instanceof Player ? (Player) damager : null, event.getSlot(), stat("swing_animation"));
        lastDamage = Bukkit.getCurrentTick();
        updateItem(damager, event.getSlot(), stat("swing_animation"));

        double fDamage = damage;
        RayTraceResult ray = damager.getWorld().rayTrace(
                damager.getEyeLocation(), location.getDirection(), 5, FluidCollisionMode.NEVER, true, 0, e -> e == victim);
        Utils.useService(CalibreDamageService.class, provider -> provider.damage(
                damager, victim, fDamage, ray == null ? new Vector() : ray.getHitPosition(), event.getItemStack()));
    }

    @Override
    public void onEvent(ItemEvents.BukkitDamage event) {
        if (!isCompleteRoot()) return;
        if (lastDamage == Bukkit.getCurrentTick()) return;
        event.getBukkitEvent().setCancelled(true);
    }

    @Override public TypeToken<MeleeSystem> getDescriptorType() { return new TypeToken<>(){}; }

    @Override
    public void acceptDescriptor(MeleeSystem descriptor) {
        lastDamage = descriptor.lastDamage;
    }

    @Override public MeleeSystem createDescriptor() { return this; }

    @Override public MeleeSystem clone() { try { return (MeleeSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public MeleeSystem copy() { return clone(); }

    public static final class Events {
        private Events() {}

        /**
         * Runs when a component with a MeleeSystem is left-clicked or damages an entity.
         */
        public static class Swing extends ItemEvents.Event<Swing.Listener> implements Cancellable {
            public interface Listener { void onEvent(Swing event); }

            private final LivingEntity damager;
            private final Entity victim;
            private double damage;
            private boolean cancelled;

            public Swing(ItemStack itemStack, EquipmentSlot slot, LivingEntity damager, Entity victim, double damage) {
                super(itemStack, slot);
                this.damager = damager;
                this.victim = victim;
                this.damage = damage;
            }

            public LivingEntity getDamager() { return damager; }
            public Entity getVictim() { return victim; }

            public double getDamage() { return damage; }
            public void setDamage(double damage) { this.damage = damage; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

            public void call(Listener listener) { listener.onEvent(this); }
        }
    }
}
