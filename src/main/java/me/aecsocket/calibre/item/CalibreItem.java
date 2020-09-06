package me.aecsocket.calibre.item;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.unifiedframework.event.Event;
import me.aecsocket.unifiedframework.util.Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

    /**
     * Modifies a flag of an item which makes it hidden to the player who holds it. Items with this flag do not appear
     * during item animations.
     * @param plugin The {@link CalibrePlugin}.
     * @param item The item to modify. The reference passed will not be cloned.
     * @param hidden Enables the flag or not.
     * @return The modified item.
     */
     static ItemStack setHidden(CalibrePlugin plugin, ItemStack item, boolean hidden) {
        return Utils.modMeta(item, meta -> {
            if (hidden)
                meta.getPersistentDataContainer().set(plugin.key("hidden"), PersistentDataType.BYTE, (byte) 1);
            else
                meta.getPersistentDataContainer().remove(plugin.key("hidden"));
        });
    }

    /**
     * Modifies a flag of an item to make it hidden to the player who holds it. Items with this flag do not appear
     * during item animations.
     * @param plugin The {@link CalibrePlugin}.
     * @param item The item to modify. The reference passed will not be cloned.
     * @return The modified item.
     */
    static ItemStack setHidden(CalibrePlugin plugin, ItemStack item) {
        return setHidden(plugin, item, true);
    }
}
