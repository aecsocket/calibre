package me.aecsocket.calibre.item;

import me.aecsocket.calibre.util.AcceptsCalibrePlugin;
import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A tag interface for a generic Calibre object which:
 * <ul>
 *     <li>has an ID</li>
 *     <li>must be validated on load</li>
 *     <li>accepts a {@link me.aecsocket.calibre.CalibrePlugin}</li>
 * </ul>
 */
public interface CalibreIdentifiable extends Identifiable, AcceptsCalibrePlugin {
    /** The valid characters that are accepted for the ID on validation. */
    String ID_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789_.";

    @Override
    default void validate() throws ValidationException {
        String id = getId();
        if (!id.chars().allMatch(c -> ID_CHARACTERS.contains(Character.toString(c))))
            throw new ValidationException(TextUtils.format("ID must only use characters {chars} (provided {id})", "chars", ID_CHARACTERS, "id", id));
    }

    /**
     * Gets the practical Calibre type of this item, used in {@link CalibreIdentifiable#getLocalizedName(String)}.
     * @return The type.
     */
    String getCalibreType();

    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link CalibreIdentifiable#getCalibreType()}.{@link Identifiable#getId()}</code>.
     * @param locale The locale to use.
     * @return The localized name.
     */
    default String getLocalizedName(String locale) { return getPlugin().gen(locale, getCalibreType() + "." + getId()); }

    /**
     * Gets the localized name of the item, looked up in the plugin's locale manager.
     * <p>
     * This is defined as <code>{@link CalibreIdentifiable#getCalibreType()}.{@link Identifiable#getId()}</code>.
     * @param sender The command sender to use the locale for.
     * @return The localized name.
     */
    default String getLocalizedName(CommandSender sender) { return getLocalizedName(sender instanceof Player ? ((Player) sender).getLocale() : getPlugin().getLocaleManager().getDefaultLocale()); }

    /**
     * Gets a short, important piece of info used in <code>/calibre list</code>.
     * Can be null.
     * @param sender The command's sender.
     * @return The info.
     */
    @Nullable default String getShortInfo(CommandSender sender) { return getLocalizedName(sender); }

    /**
     * Gets lines of info used in <code>/calibre info</code>. The string is split
     * by <code>\n</code> to create the line separations. Can be null.
     * @param sender The command's sender.
     * @return The info.
     */
    @Nullable String getLongInfo(CommandSender sender);
}
