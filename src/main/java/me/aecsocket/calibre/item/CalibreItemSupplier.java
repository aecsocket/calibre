package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.item.Item;

/**
 * A class for a generic {@link CalibreIdentifiable} which:
 * <ul>
 *     <li>can be converted to an {@link org.bukkit.inventory.ItemStack}</li>
 * </ul>
 */
public interface CalibreItemSupplier extends CalibreIdentifiable, Item {
    default String getCalibreType() { return getItemType(); }
}
