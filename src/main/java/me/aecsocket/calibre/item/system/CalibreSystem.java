package me.aecsocket.calibre.item.system;

import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.calibre.item.component.CalibreComponent;

/**
 * A system that can interact with a {@link CalibreComponent} through:
 * <ul>
 *     <li>Running code on event calls (right click, hold, etc.)</li>
 *     <li>Accepting custom data on deserialization</li>
 * </ul>
 */
public interface CalibreSystem extends Identifiable {}
