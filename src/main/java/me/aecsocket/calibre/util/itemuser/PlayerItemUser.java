package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A {@link Player} item user.
 */
public class PlayerItemUser extends LivingEntityItemUser {
    public PlayerItemUser(Player player) {
        super(player);
    }

    @Override public Player getEntity() { return (Player) super.getEntity(); }

    @Override
    public void startAnimation(CalibrePlugin plugin, Animation animation, EquipmentSlot slot) {
        plugin.getPlayerData(getEntity()).startAnimation(animation, slot);
    }
}
