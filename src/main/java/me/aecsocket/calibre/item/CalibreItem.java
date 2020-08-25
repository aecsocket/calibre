package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.event.Event;

/**
 * A class for a generic {@link CalibreItemSupplier} which:
 * <ul>
 *     <li>provides a way to call {@link Event}s to this item</li>
 * </ul>
 */
public interface CalibreItem extends CalibreItemSupplier {
    /**
     * Calls an event to this item. Depends on the implementation.
     * @param event The event to call.
     */
    void callEvent(Event<?> event);
}
