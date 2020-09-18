package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an object which can use an {@link org.bukkit.inventory.ItemStack} in some capacity.
 */
public interface ItemUser {
    /**
     * Gets the location from which actions will be run.
     * @return The location.
     */
    Location getLocation();

    /**
     * Starts an animation for this user.
     * @param animation The animation.
     * @param slot The item slot to animate.
     */
    void startAnimation(Animation animation, EquipmentSlot slot);

    ItemStack getItem(EquipmentSlot slot);
    void setItem(EquipmentSlot slot, ItemStack item);

    /**
     * Gets an {@link ItemUser} from an object user, using default implementations.
     * @param plugin The {@link CalibrePlugin}.
     * @param user The user.
     * @return The {@link ItemUser} version, or null if no available version was found.
     */
    static ItemUser ofDefault(CalibrePlugin plugin, Object user) {
        if (user instanceof Player)
            return new PlayerItemUser(plugin, (Player) user);
        if (user instanceof LivingEntity)
            return new LivingEntityItemUser((LivingEntity) user);
        return null;
    }
}
