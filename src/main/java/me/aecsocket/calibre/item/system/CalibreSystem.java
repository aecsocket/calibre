package me.aecsocket.calibre.item.system;

import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.unifiedframework.stat.Stat;

import java.util.Map;

/**
 * A system that can interact with a {@link CalibreComponent} through:
 * <ul>
 *     <li>Running code on event calls (right click, hold, etc.)</li>
 *     <li>Accepting custom data on deserialization</li>
 *     <li>Providing stat definitions</li>
 * </ul>
 */
public interface CalibreSystem extends Identifiable {
    /**
     * Gets all stats that this system provides.
     * @return A map of stats.
     */
    default Map<String, Stat<?>> getDefaultStats() { return null; }
}
