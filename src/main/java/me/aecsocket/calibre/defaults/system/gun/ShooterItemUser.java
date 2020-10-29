package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.item.util.user.ItemUser;
import me.aecsocket.unifiedframework.util.Vector2;

public interface ShooterItemUser extends ItemUser {
    void applyRecoil(Vector2 recoil, double recoilSpeed, double recoilRecovery, long recoilRecoveryAfter, double recoilRecoverySpeed);
}
