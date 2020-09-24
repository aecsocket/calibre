package me.aecsocket.calibre.defaults.gun.sight;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.service.system.ActionSystem;
import me.aecsocket.calibre.item.ItemEvents;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.CalibreSystem;
import me.aecsocket.calibre.item.system.SystemSearchOptions;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.stat.DataStat;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.calibre.util.itemuser.PlayerItemUser;
import me.aecsocket.unifiedframework.component.ComponentSlot;
import me.aecsocket.unifiedframework.event.Cancellable;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.NumberStat;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SightableSystem implements CalibreSystem<SightableSystem> {
    public static class SightPath {
        public final String[] path;
        public final String id;

        public SightPath(String[] path, String id) {
            this.path = path;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SightPath sightPath = (SightPath) o;
            return Arrays.equals(path, sightPath.path) &&
                    id.equals(sightPath.id);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(id);
            result = 31 * result + Arrays.hashCode(path);
            return result;
        }

        @Override
        public String toString() { return String.join(".", path) + ":" + id; }
    }

    public static class SightInstance {
        public SightSystem system;
        public SightPath path;
        public Sight sight;

        public SightInstance(SightSystem system, SightPath path, Sight sight) {
            this.system = system;
            this.path = path;
            this.sight = sight;
        }
    }

    public static final Map<String, Stat<?>> STATS = new Utils.MapInitializer<String, Stat<?>, LinkedHashMap<String, Stat<?>>>(new LinkedHashMap<>())
            .init("change_sight_delay", new NumberStat.Long(1L))
            .init("change_sight_sound", new DataStat.Sound())
            .init("change_sight_animation", new AnimationStat())

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
    private SightPath sightPath;
    private boolean aiming;

    public SightableSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    public ActionSystem getActionSystem() { return actionSystem; }

    public SightPath getSightPath() { return sightPath; }
    public void setSightPath(SightPath sightPath) { this.sightPath = sightPath; }

    public boolean isAiming() { return aiming; }
    public void setAiming(boolean aiming) { this.aiming = aiming; }

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
    public void acceptTree(ComponentTree tree) {
        EventDispatcher dispatcher = tree.getEventDispatcher();
        dispatcher.registerListener(ItemEvents.Interact.class, this::onEvent, 0);

        validateSight();
        if (sightPath != null) {
            SightInstance sightInst = getSight(sightPath);
            if (sightInst != null) {
                Sight sight = sightInst.sight;
                if (sight.getSelected() != null) tree.getStats().addAll(sight.getSelected());
                if (aiming && sight.getActive() != null) tree.getStats().addAll(sight.getActive());
            }
        }
    }

    public List<SightPath> findSights() {
        List<SightPath> sights = new ArrayList<>();
        searchSystems(
                new SystemSearchOptions<SightSystem>()
                .slotTags("sight")
                .systemType(SightSystem.class),
                result -> {
                    if (result.getSystem().getSights() != null)
                        result.getSystem().getSights().forEach((id, sight) ->
                                sights.add(new SightPath(
                                        result.getWalkData().getPath(),
                                        id
                                ))
                        );
                    return false;
                }
        );
        return sights;
    }

    public int findSightIndex(List<SightPath> sights, SightPath path) {
        return sights.indexOf(path);
    }

    public int findSightIndex(List<SightPath> sights) { return findSightIndex(sights, sightPath); }

    public SightInstance getSight(SightPath path) {
        ComponentSlot rawSlot = parent.getSlot(path.path);
        if (rawSlot.get() == null) return null;
        if (!(rawSlot instanceof CalibreComponentSlot)) return null;
        SightSystem system = ((CalibreComponentSlot) rawSlot).get().getService(SightSystem.class);
        if (system == null || system.getSights() == null) return null;
        Map<String, Sight> sights = system.getSights();
        return sights.containsKey(path.id) ? new SightInstance(
                system,
                path,
                sights.get(path.id)
        ) : null;
    }

    public void validateSight() {
        if (sightPath == null || getSight(sightPath) == null) {
            List<SightPath> sights = findSights();
            if (sights.size() > 0)
                sightPath = sights.get(0);
        }
    }

    public void changeSight(Events.ChangeSight event) {
        if (callEvent(event).cancelled) return;

        SightInstance newSight = getSight(event.newPath);
        if (newSight == null) return;
        sightPath = newSight.path;
        getTree().rebuild();
        actionSystem.startAction(
                stat("change_sight_delay"),
                event.getUser().getLocation(), stat("change_sight_sound"), null,
                event.getUser(), event.getSlot(), stat("change_sight_animation")
        );
    }

    public void nextSight(Events.NextSightEvent event) {
        if (callEvent(event).cancelled) return;

        List<SightPath> sights = findSights();
        int sightIndex = findSightIndex(sights);
        int newIndex = sightIndex > sights.size() ? 0 : (sightIndex + 1) % sights.size();
        if (newIndex < sights.size())
            changeSight(new Events.ChangeSight(
                    event.getItemStack(),
                    event.getSlot(),
                    event.getUser(),
                    this,
                    sightPath,
                    sights.get(newIndex)
            ));
    }

    public void toggleAim(Events.ToggleAim event) {
        if (callEvent(event).cancelled) return;

        if (aiming == event.aiming) return;
        aiming = event.aiming;
        getTree().rebuild();
        // TODO should not be real delays!!!! should only delay the application of aim stats, NOT actual gun operation
        if (aiming)
            actionSystem.startAction(
                    stat("aim_in_delay"),
                    event.getUser().getLocation(), stat("aim_in_sound"), null,
                    event.getUser(), event.getSlot(), stat("aim_in_animation")
            );
        else
            actionSystem.startAction(
                    stat("aim_out_delay"),
                    event.getUser().getLocation(), stat("aim_out_sound"), null,
                    event.getUser(), event.getSlot(), stat("aim_out_animation")
            );
    }

    public void onEvent(ItemEvents.Interact event) {
        if (!isCompleteRoot()) return;
        if (!event.isRightClick()) return;
        if (!actionSystem.isAvailable()) return;
        List<SightPath> sights = findSights();
        if (sights.size() > 1 && aiming && event.getUser() instanceof PlayerItemUser) {
            Player player = ((PlayerItemUser) event.getUser()).getEntity();
            if (player.isSneaking()) {
                nextSight(new Events.NextSightEvent(
                        event.getItemStack(),
                        event.getSlot(),
                        event.getUser(),
                        this
                ));
                return;
            }
        }
        if (sights.size() > 0) {
            toggleAim(new Events.ToggleAim(
                    event.getItemStack(),
                    event.getSlot(),
                    event.getUser(),
                    this,
                    !aiming
            ));
        }
    }

    @Override public TypeToken<SightableSystem> getDescriptorType() { return new TypeToken<>(){}; }
    @Override public SightableSystem createDescriptor() { return this; }
    @Override
    public void acceptDescriptor(SightableSystem descriptor) {
        sightPath = descriptor.sightPath;
        aiming = descriptor.aiming;
    }

    @Override public SightableSystem clone() { try { return (SightableSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public SightableSystem copy() { return clone(); }

    public static final class Events {
        private Events() {}

        /**
         * Base class for SightableSystem-related events.
         */
        public static class SightableEvent extends ItemEvents.BaseEvent implements ItemEvents.SystemEvent<SightableSystem> {
            private final SightableSystem system;

            public SightableEvent(ItemStack itemStack, EquipmentSlot slot, ItemUser user, SightableSystem system) {
                super(itemStack, slot, user);
                this.system = system;
            }

            @Override public SightableSystem getSystem() { return system; }
        }

        /**
         * Runs when changing the sight to the next available one.
         */
        public static class NextSightEvent extends SightableEvent implements Cancellable {
            private boolean cancelled;

            public NextSightEvent(ItemStack itemStack, EquipmentSlot slot, ItemUser user, SightableSystem system) {
                super(itemStack, slot, user, system);
            }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        /**
         * Runs when changing the selected sight.
         */
        public static class ChangeSight extends SightableEvent implements Cancellable {
            private SightPath oldPath;
            private SightPath newPath;
            private boolean cancelled;

            public ChangeSight(ItemStack itemStack, EquipmentSlot slot, ItemUser user, SightableSystem system, SightPath oldPath, SightPath newPath) {
                super(itemStack, slot, user, system);
                this.oldPath = oldPath;
                this.newPath = newPath;
            }

            public SightPath getOldPath() { return oldPath; }
            public void setOldPath(SightPath oldPath) { this.oldPath = oldPath; }

            public SightPath getNewPath() { return newPath; }
            public void setNewPath(SightPath newPath) { this.newPath = newPath; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }

        /**
         * Runs when toggling aiming in or out.
         */
        public static class ToggleAim extends SightableEvent implements Cancellable {
            private boolean aiming;
            private boolean cancelled;

            public ToggleAim(ItemStack itemStack, EquipmentSlot slot, ItemUser user, SightableSystem system, boolean aiming) {
                super(itemStack, slot, user, system);
                this.aiming = aiming;
            }

            public boolean isAiming() { return aiming; }
            public void setAiming(boolean aiming) { this.aiming = aiming; }

            @Override public boolean isCancelled() { return cancelled; }
            @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        }
    }
}
