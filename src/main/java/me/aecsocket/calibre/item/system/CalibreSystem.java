package me.aecsocket.calibre.item.system;

import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.item.CalibreIdentifiable;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.event.EventDispatcher;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.stat.Stat;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;

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
    /**
     * Gets all stats that this system provides.
     * @return A map of stats.
     */
    default Map<String, Stat<?>> getDefaultStats() { return null; }

    /**
     * Gets all systems that must be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<? extends CalibreSystem<?>>> getDependencies() { return Collections.emptyList(); }

    /**
     * Gets all systems that must not be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<? extends CalibreSystem<?>>> getConflicts() { return Collections.emptyList(); }

    /**
     * Accepts the parent {@link CalibreComponent} that this System is a system of.
     * @param parent The parent component.
     */
    void setParent(CalibreComponent parent);

    /**
     * Registers all of the listeners that this system uses into an {@link EventDispatcher}.
     * @param dispatcher The EventDispatcher.
     */
    default void registerListeners(EventDispatcher dispatcher) {}

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

    /**
     * Creates an exact copy of this system, but with references in fields copied.
     * @return The copy.
     */
    CalibreSystem<D> copy();

    @Override @Nullable default String getShortInfo(CommandSender sender) { return null; }
    @Override @Nullable default String getLongInfo(CommandSender sender) { return null; }

    /**
     * Copies a map of class-systems, copying both the map and the systems. Used in {@link CalibreComponent#copy()}.
     * @param original The original map.
     * @return The copied map, with copied elements.
     */
    static Map<Class<? extends CalibreSystem<?>>, CalibreSystem<?>> copyMap(Map<Class<? extends CalibreSystem<?>>, CalibreSystem<?>> original) {
        Map<Class<? extends CalibreSystem<?>>, CalibreSystem<?>> copy = new HashMap<>();
        for (Map.Entry<Class<? extends CalibreSystem<?>>, CalibreSystem<?>> entry : original.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().copy());
        }
        return copy;
    }
}
