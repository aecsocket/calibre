package me.aecsocket.calibre.world.user;

import me.aecsocket.unifiedframework.util.vector.Vector2D;

public interface RecoilableUser extends ItemUser {
    void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter);
}
