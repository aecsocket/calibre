package me.aecsocket.calibre.util;

import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.bukkit.command.CommandSender;

/**
 * Calibre's implementation of {@link Identifiable}.
 */
public interface CalibreIdentifiable extends Identifiable, AcceptsCalibrePlugin {
    String ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.";

    @Override
    default void validate() throws ValidationException {
        if(getId() == null)
            throw new ValidationException("ID is not set");
        if (!getId().chars().allMatch(c -> ID_CHARS.contains(Character.toString(c))))
            throw new ValidationException(TextUtils.format(
                    "ID contains characters outside of {valid}",
                    "valid", ID_CHARS
            ));
    }

    /**
     * Gets the translation key for the name of this object.
     * @return The translation key.
     */
    String getNameKey();

    /**
     * Gets the localized, human-readable name of this object.
     * @param locale The locale to create the name for.
     * @return The localized name.
     */
    default String getLocalizedName(String locale) { return getPlugin().gen(locale, getNameKey()); }

    /**
     * Gets the localized, human-readable name of this object.
     * @param sender The command sender to create the name for.
     * @return The localized name.
     */
    default String getLocalizedName(CommandSender sender) { return getPlugin().gen(sender, getNameKey()); }

    /**
     * Gets a short, one-line description of this object.
     * @param locale The locale to create the info for.
     * @return The short info string.
     */
    default String getShortInfo(String locale) { return getLocalizedName(locale); }

    // TODO add long info for items
    /**
     * Gets a more extensive description of this object.
     * @param locale The locale to create the info for.
     * @return The long info string, with lines separated by {@code \n}.
     */
    default String getLongInfo(String locale) { return getPlugin().gen(locale, "no_info"); }
}
