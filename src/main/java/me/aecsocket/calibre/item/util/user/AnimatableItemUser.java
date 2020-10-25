package me.aecsocket.calibre.item.util.user;

import me.aecsocket.calibre.item.ItemAnimation;
import org.bukkit.inventory.EquipmentSlot;

public interface AnimatableItemUser {
    ItemAnimation.Instance startAnimation(ItemAnimation animation, EquipmentSlot slot);
    ItemAnimation.Instance getAnimation();
}
