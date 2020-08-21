package me.aecsocket.calibre.item;

import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.item.Item;
import me.aecsocket.unifiedframework.registry.Identifiable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A tag interface for a generic Calibre object which:
 * <ul>
 *     <li>has an ID</li>
 *     <li>must be validated on load</li>
 *     <li>can be converted to an {@link org.bukkit.inventory.ItemStack}</li>
 *     <li>accepts a {@link me.aecsocket.calibre.CalibrePlugin}</li>
 * </ul>
 */
public interface CalibreItem extends CalibreIdentifiable, Item, AcceptsCalibrePlugin {
    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link Item#getItemType()}.{@link Identifiable#getId()}</code>.
     * @param locale The locale to use.
     * @return The localized name.
     */
    default String getLocalizedName(String locale) { return getPlugin().gen(locale, getItemType() + "." + getId()); }

    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link Item#getItemType()}.{@link Identifiable#getId()}</code>.
     * @param sender The command sender to use the locale for.
     * @return The localized name.
     */
    default String getLocalizedName(CommandSender sender) { return getLocalizedName(sender instanceof Player ? ((Player) sender).getLocale() : getPlugin().getLocaleManager().getDefaultLocale()); }
}
