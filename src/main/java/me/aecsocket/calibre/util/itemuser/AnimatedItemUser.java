package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.item.animation.Animation;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Represents an {@link ItemUser} which can have animations applied.
 */
public interface AnimatedItemUser extends ItemUser {
    /**
     * Starts an animation for this user.
     * @param animation The animation.
     * @param slot The item slot to animate.
     */
    void startAnimation(Animation animation, EquipmentSlot slot);

    /**
     * Gets the currently running animation for this user.
     * @return The animation.
     */
    Animation.Instance getAnimation();
}
