package me.aecsocket.calibre.item.util.user;

import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.util.CalibrePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerItemUser extends LivingEntityItemUser implements DelayableItemUser, AnimatableItemUser {
    private final CalibrePlayer data;

    public PlayerItemUser(Player player, CalibrePlayer data) {
        super(player);
        this.data = data;
    }

    @Override public Player getEntity() { return (Player) super.getEntity(); }
    public CalibrePlayer getData() { return data; }

    @Override public void applyDelay(long ms) { data.applyDelay(ms); }
    @Override public double getDelay() { return data.getDelay(); }

    @Override public ItemAnimation.Instance startAnimation(ItemAnimation animation, EquipmentSlot slot) { return data.startAnimation(animation, slot); }
}
