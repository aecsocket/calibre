package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.util.vector.Vector2D;

public interface RecoilableUser extends ItemUser {
    void applyRecoil(Vector2D recoil, double recoilSpeed, double recoilRecovery, double recoilRecoverySpeed, long recoilRecoveryAfter);
}
