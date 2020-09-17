package me.aecsocket.calibre.item.system;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.item.CalibreIdentifiable;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.util.itemuser.ItemUser;
import me.aecsocket.unifiedframework.component.Component;
import me.aecsocket.unifiedframework.event.Event;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.stat.Stat;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A system that can interact with a {@link CalibreComponent} through:
 * <ul>
 *     <li>Running code on event calls (right click, hold, etc.)</li>
 *     <li>Accepting custom data on deserialization</li>
 *     <li>Providing stat definitions</li>
 * </ul>
 * @param <D> The descriptor type.
 */
public interface CalibreSystem<D> extends CalibreIdentifiable, Cloneable {
    //region System Info
    /**
     * Gets all stats that this system provides.
     * @return A map of stats.
     */
    default Map<String, Stat<?>> getDefaultStats() { return null; }

    /**
     * Gets all systems that must be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<?>> getDependencies() { return Collections.emptyList(); }

    /**
     * Gets all systems that must not be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<?>> getConflicts() { return Collections.emptyList(); }

    /**
     * Gets the parent {@link CalibreComponent} that this System is a system of.
     * @return The parent component.
     */
    CalibreComponent getParent();

    /**
     * Accepts the parent {@link CalibreComponent} that this System is a system of.
     * @param parent The parent component.
     */
    void acceptParent(CalibreComponent parent);

    /**
     * Registers all of the listeners that this system uses into an {@link EventDispatcher}.
     * @param dispatcher The EventDispatcher.
     */
    default void registerListeners(EventDispatcher dispatcher) {}

    /**
     * Gets the types of services that this system provides, or null if it does not provide services.
     * @return The types of services that this system provides.
     */
    default @Nullable Collection<Class<?>> getServiceTypes() { return null; }

    /**
     * Gets the type of this system's descriptor of type {@link D}, or null if this system does not accept descriptors.
     * @return The type.
     */
    default TypeToken<D> getDescriptorType() { return null; }

    /**
     * Accepts a descriptor of type {@link D}, describing instance data about this system.
     * @param descriptor The descriptor.
     */
    default void acceptDescriptor(D descriptor) {}

    /**
     * Creates a descriptor of type {@link D} from the data of this instance.
     * @return The descriptor.
     */
    default D createDescriptor() { return null; }

    //endregion

    //region Utilities
    /**
     * Gets the {@link ComponentTree} that this instance's parent is a part of.
     * @return The tree.
     */
    default ComponentTree getTree() { return getParent().getTree(); }

    /**
     * Gets a stat value from the {@link CalibreSystem#getTree()}'s StatMap.
     * @param key The key of the stat.
     * @param <T> The stat's value's type.
     * @return The stat value.
     */
    default <T> T stat(String key) { return getTree().stat(key); }

    /**
     * Sets the item in the equipment slot of the specified entity's inventory to this parent's tree's root's {@link CalibreComponent#createItem(Player, ItemStack)}.
     * This retains the stack amount of the old slot.
     * @param user The user to get items from, and if a {@link me.aecsocket.calibre.util.itemuser.PlayerItemUser}, to be passed to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * @param slot The equipment slot.
     * @return The new item.
     */
    default ItemStack updateItem(ItemUser user, EquipmentSlot slot) { return getTree().getRoot().updateItem(user, slot); }

    /**
     * Sets the item in the equipment slot of the specified entity's inventory to this parent's tree's root's {@link CalibreComponent#createItem(Player, ItemStack)}.
     * This retains the stack amount of the old slot.
     * @param user The user to get items from, and if a {@link me.aecsocket.calibre.util.itemuser.PlayerItemUser}, to be passed to {@link CalibreComponent#createItem(Player, ItemStack)}.
     * @param slot The equipment slot.
     * @param animationUsed The animation applied to this item before updating.
     * @return The new item.
     */
    default ItemStack updateItem(ItemUser user, EquipmentSlot slot, Animation animationUsed) { return getTree().getRoot().updateItem(user, slot, animationUsed); }

    /**
     * Calls an event to the tree.
     * @param event The event to call.
     * @param <T> The called event type.
     * @return The event called.
     */
    default <T extends Event<?>> T callEvent(T event) { return getParent().callEvent(event); }

    /**
     * Gets if the parent is {@link CalibreComponent#isCompleteRoot()}.
     * @return The result.
     */
    default boolean isCompleteRoot() { return getParent().isCompleteRoot(); }

    /**
     * Gets another system on the parent component by its type.
     * @param type The type of the system.
     * @param <T> The type of the system.
     * @return The system.
     */
    default <T> T getSystem(Class<T> type) {
        T system = getParent().getSystem(type);
        if (system == null || !type.isAssignableFrom(system.getClass())) return null;
        return type.cast(system);
    }

    /**
     * Iterates through all slots, components and systems which match the {@link SystemSearchOptions} and runs the
     * consumer on them.
     * @param options The {@link SystemSearchOptions}.
     * @param consumer The action to run for each {@link SystemSearchResult}. If returns true, will stop searching.
     * @param <T> The type of system to search for.
     */
    default <T> void searchSystems(SystemSearchOptions<T> options, Predicate<SystemSearchResult<T>> consumer) {
        getParent().searchSystems(options, consumer);
    }

    //endregion

    //region Identifiable Info

    @Override default String getCalibreType() { return "system"; }

    /**
     * Creates an exact copy of this system, but with references in fields copied.
     * @return The copy.
     */
    CalibreSystem<D> copy();

    @Override @Nullable default String getLongInfo(CommandSender sender) { return null; }

    //endregion

    /**
     * Copies a map of class-systems, copying both the map and the systems. Used in {@link CalibreComponent#copy()}.
     * @param original The original map.
     * @return The copied map, with copied elements.
     */
    static Map<Class<?>, CalibreSystem<?>> copyMap(Map<Class<?>, CalibreSystem<?>> original) {
        Map<Class<?>, CalibreSystem<?>> copy = new HashMap<>();
        for (Map.Entry<Class<?>, CalibreSystem<?>> entry : original.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }
}
