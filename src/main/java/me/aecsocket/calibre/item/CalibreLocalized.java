package me.aecsocket.calibre.item;

import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A generic Calibre object which has a localizable name.
 */
public interface CalibreLocalized extends AcceptsCalibrePlugin {
    /**
     * Gets the practical Calibre type of this item, used in {@link #getLocalizedName(String)}.
     * @return The type.
     */
    String getCalibreType();

    /**
     * Gets the ID of this item, used in {@link #getLocalizedName(String)}.
     * @return The type.
     */
    String getId();

    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link #getCalibreType()}.{@link #getId()}</code>.
     * @param locale The locale to use.
     * @return The localized name.
     */
    default String getLocalizedName(String locale) { return getPlugin().gen(locale, getCalibreType() + "." + getId()); }

    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link #getCalibreType()}.{@link #getId()}</code>.
     * @param sender The command sender to use the locale for.
     * @return The localized name.
     */
    default String getLocalizedName(CommandSender sender) { return getLocalizedName(sender instanceof Player ? ((Player) sender).getLocale() : getPlugin().getLocaleManager().getDefaultLocale()); }
}
