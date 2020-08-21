package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.item.CalibreIdentifiable;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.stat.Stat;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A system that can interact with a {@link CalibreComponent} through:
 * <ul>
 *     <li>Running code on event calls (right click, hold, etc.)</li>
 *     <li>Accepting custom data on deserialization</li>
 *     <li>Providing stat definitions</li>
 * </ul>
 */
public interface CalibreSystem extends CalibreIdentifiable, Cloneable {
    /**
     * Gets all stats that this system provides.
     * @return A map of stats.
     */
    default Map<String, Stat<?>> getDefaultStats() { return null; }

    /**
     * Gets all systems that must be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<? extends CalibreSystem>> getDependencies() { return Collections.emptyList(); }

    /**
     * Gets all systems that must not be present for this to be able to load.
     * @return The list of system types.
     */
    default @NotNull Collection<Class<? extends CalibreSystem>> getConflicts() { return Collections.emptyList(); }

    /**
     * Accepts a map of CalibreSystems linked to their type which exist in the component that this system is in.
     * This will include this system.
     * @param dependencies The map of systems linked with their type.
     */
    default void acceptSystems(Map<Class<? extends CalibreSystem>, CalibreSystem> dependencies) {}

    /**
     * Creates an exact copy of this system, but with references in fields copied.
     * @return The copy.
     */
    CalibreSystem copy();

    @Override @Nullable default String getShortInfo(CommandSender sender) { return null; }
    @Override @Nullable default String getLongInfo(CommandSender sender) { return null; }
}
