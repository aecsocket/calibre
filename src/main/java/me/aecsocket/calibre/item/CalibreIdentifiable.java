package me.aecsocket.calibre.item;

import me.aecsocket.unifiedframework.registry.Identifiable;
import me.aecsocket.unifiedframework.registry.ValidationException;
import me.aecsocket.unifiedframework.util.TextUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

/**
 * A tag interface for a generic Calibre object which:
 * <ul>
 *     <li>has an ID</li>
 *     <li>must be validated on load</li>
 * </ul>
 */
public interface CalibreIdentifiable extends Identifiable {
    String ID_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789_.";

    @Override
    default void validate() throws ValidationException {
        String id = getId();
        if (!id.chars().allMatch(c -> ID_CHARACTERS.contains(Character.toString(c))))
            throw new ValidationException(TextUtils.format("ID must only use characters {chars} (provided {id})", "chars", ID_CHARACTERS, "id", id));
    }

    /**
     * Gets a short, important piece of info used in <code>/calibre list</code>.
     * Can be null.
     * @param sender The command's sender.
     * @return The info.
     */
    @Nullable String getShortInfo(CommandSender sender);

    /**
     * Gets lines of info used in <code>/calibre info</code>. The string is split
     * by <code>\n</code> to create the line separations. Can be null.
     * @param sender The command's sender.
     * @return The info.
     */
    @Nullable String getLongInfo(CommandSender sender);
}
