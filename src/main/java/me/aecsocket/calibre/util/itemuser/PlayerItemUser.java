package me.aecsocket.calibre.util.itemuser;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.unifiedframework.util.Vector2;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

/**
 * A {@link Player} item user.
 */
public class PlayerItemUser extends LivingEntityItemUser implements GunItemUser {
    private final CalibrePlugin plugin;

    public PlayerItemUser(CalibrePlugin plugin, Player player) {
        super(player);
        this.plugin = plugin;
    }

    public CalibrePlugin getPlugin() { return plugin; }

    @Override public Player getEntity() { return (Player) super.getEntity(); }

    @Override
    public void startAnimation(Animation animation, EquipmentSlot slot) {
        plugin.getPlayerData(getEntity()).startAnimation(animation, slot);
    }

    @Override
    public void setSpread(double spread) { plugin.getPlayerData(getEntity()).setSpread(spread); }
    @Override
    public double getSpread() { return plugin.getPlayerData(getEntity()).getSpread(); }

    @Override
    public void applyRecoil(Vector2 recoil, double recoilSpeed, long recoverAfter, double recoilRecovery, double recoilRecoverySpeed) {
        plugin.getPlayerData(getEntity()).applyRecoil(recoil, recoilSpeed, recoverAfter, recoilRecovery, recoilRecoverySpeed);
    }
}
