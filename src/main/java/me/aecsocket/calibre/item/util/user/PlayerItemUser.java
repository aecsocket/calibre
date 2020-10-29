package me.aecsocket.calibre.item.util.user;

import me.aecsocket.calibre.defaults.system.gun.ShooterItemUser;
import me.aecsocket.calibre.item.ItemAnimation;
import me.aecsocket.calibre.util.CalibrePlayer;
import me.aecsocket.unifiedframework.util.Vector2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerItemUser extends LivingEntityItemUser implements AnimatableItemUser, ShooterItemUser {
    private final CalibrePlayer data;

    public PlayerItemUser(Player player, CalibrePlayer data) {
        super(player);
        this.data = data;
    }

    @Override public Player getEntity() { return (Player) super.getEntity(); }
    public CalibrePlayer getData() { return data; }

    @Override public ItemAnimation.Instance startAnimation(ItemAnimation animation, EquipmentSlot slot) { return data.startAnimation(animation, slot); }
    @Override public ItemAnimation.Instance getAnimation() { return data.getAnimation(); }

    @Override
    public void applyRecoil(Vector2 recoil, double recoilSpeed, double recoilRecovery, long recoilRecoveryAfter, double recoilRecoverySpeed) {
        data.applyRecoil(recoil, recoilSpeed, recoilRecovery, recoilRecoveryAfter, recoilRecoverySpeed);
    }
}
