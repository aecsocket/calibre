package me.aecsocket.calibre.defaults.system.gun;

import com.google.gson.annotations.Expose;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ComponentProviderSystem;
import me.aecsocket.calibre.defaults.system.ItemSystem;
import me.aecsocket.calibre.defaults.system.ProjectileProviderSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.component.SystemSearchOptions;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.util.slot.EquipmentItemSlot;
import me.aecsocket.calibre.item.util.slot.ItemSlot;
import me.aecsocket.calibre.item.util.user.EntityItemUser;
import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.calibre.util.stat.ItemAnimationStat;
import me.aecsocket.calibre.util.stat.ParticleStat;
import me.aecsocket.calibre.util.stat.SoundStat;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.loop.SchedulerLoop;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.impl.BooleanStat;
import me.aecsocket.unifiedframework.stat.impl.NumberStat;
import me.aecsocket.unifiedframework.util.MapInit;
import me.aecsocket.unifiedframework.util.Projectile;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.Location;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FireableSystem extends BaseSystem {
    public static final String ID = "fireable";
    public static final Map<String, Stat<?>> STATS = MapInit.of(new LinkedHashMap<String, Stat<?>>())
            .init("damage", new NumberStat.Double(0d))
            .init("muzzle_velocity", new NumberStat.Double(0d))
            .init("auto_chamber", new BooleanStat(true))

            .init("shots", new NumberStat.Int(1))
            .init("shot_delay", new NumberStat.Long(0L))

            .init("projectiles", new NumberStat.Int(1))
            .init("projectile_bounce", new NumberStat.Double(0d))
            .init("projectile_drag", new NumberStat.Double(0d))
            .init("projectile_gravity", new NumberStat.Double(Projectile.GRAVITY))
            .init("projectile_expansion", new NumberStat.Double(0d))

            .init("chamber_priority", new NumberStat.Int(0))
            .init("ammo_priority", new NumberStat.Int(0))

            .init("fire_delay", new NumberStat.Long(0L))
            .init("fire_sound", new SoundStat())
            .init("fire_particle", new ParticleStat())
            .init("fire_animation", new ItemAnimationStat())
            .get();

    @Expose(serialize = false) private boolean usable;
    private transient ItemSystem itemSystem;
    private long[] shootAt;

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
        events.registerListener(ItemEvents.Equip.class, this::onEvent, 0);
        events.registerListener(ItemEvents.Interact.class, this::onEvent, 0);
    }

    @Override public Map<String, Stat<?>> getDefaultStats() { return STATS; }

    private void onEvent(ItemEvents.Equip event) {
        if (!(event.getTickContext().getLoop() instanceof SchedulerLoop)) return;
        if (event.getSlot() instanceof EquipmentItemSlot && ((EquipmentItemSlot) event.getSlot()).getEquipmentSlot() != EquipmentSlot.HAND) return;
        while (shootAt != null && shootAt.length > 0 && System.currentTimeMillis() >= shootAt[0]) {
            shootAt = Arrays.copyOf(shootAt, shootAt.length - 1);
            fireOnce(new Events.PreFireOnce(
                    event.getStack(),
                    event.getSlot(),
                    event.getUser(),
                    this
            ));
        }
    }

    private void onEvent(ItemEvents.Interact event) {
        fire(new Events.Fire(
                event.getStack(),
                event.getSlot(),
                event.getUser(),
                this
        ));
    }

    public void fire(Events.Fire event) {
        if (!usable) return;
        if (!itemSystem.isAvailable()) return;
        if (callEvent(event).cancelled) return;

        int shots = stat("shots");
        long shotDelay = stat("shot_delay");
        shootAt = new long[shots];
        for (int i = 0; i < shots; i++)
            shootAt[i] = System.currentTimeMillis() + shotDelay;
        event.updateItem(this);
    }

    public void fireOnce(Events.PreFireOnce event) {
        Map.Entry<CalibreComponentSlot, ProjectileProviderSystem> chamberEntry = Utils.atOr(parent.collectSystems(
                new SystemSearchOptions<>(ProjectileProviderSystem.class)
                        .slotTag("chamber")
                        .targetPriority(stat("chamber_priority"))
        ), 0);
        if (chamberEntry == null) {
            event.updateItem(this);
            return;
        }
        CalibreComponentSlot chamberSlot = chamberEntry.getKey();
        ProjectileProviderSystem chamber = chamberEntry.getValue();

        Location location = event.getUser().getLocation();
        for (int i = 0; i < (int) stat("shots"); i++) {
            plugin.getSchedulerLoop().registerTickable(chamber.createProjectile(new ProjectileProviderSystem.Data(
                    location,
                    location.getDirection().multiply(stat("muzzle_velocity")),
                    stat("projectile_bounce"), stat("projectile_drag"), stat("projectile_gravity"), stat("projectile_expansion")
            )).inEntity(event.getUser() instanceof EntityItemUser ? ((EntityItemUser) event.getUser()).getEntity() : null));
        }

        chamberEntry.getKey().set(null);
        if (stat("auto_chamber")) {
            Map.Entry<CalibreComponentSlot, ComponentProviderSystem> ammo = Utils.atOr(parent.collectSystems(
                    new SystemSearchOptions<>(ComponentProviderSystem.class)
                            .slotTag("ammo")
                            .targetPriority(stat("ammo_priority"))
            ), 0);
            if (ammo != null) {
                CalibreComponent nextChamber = ammo.getValue().next();
                if (chamberSlot.isCompatible(nextChamber))
                    chamberSlot.set(nextChamber);
            }
        }

        event.updateItem(this);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public FireableSystem clone() { return (FireableSystem) super.clone(); }
    @Override public FireableSystem copy() {
        FireableSystem copy = clone();
        copy.shootAt = shootAt == null ? null : shootAt.clone();
        return copy;
    }

    public static final class Events {
        private Events() {}

        public static class Event extends ItemEvents.SystemEvent<FireableSystem> {
            public Event(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }
        }

        public static class Fire extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public Fire(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }


        public static class PreFireOnce extends Event implements ItemEvents.Cancellable {
            private boolean cancelled;

            public PreFireOnce(ItemStack stack, ItemSlot slot, ItemUser user, FireableSystem system) {
                super(stack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
