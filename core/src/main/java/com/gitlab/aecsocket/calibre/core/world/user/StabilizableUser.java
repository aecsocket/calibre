package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;

public interface StabilizableUser extends ItemUser {
    boolean stabilize(TickContext tickContext);
    double stamina();
    double maxStamina();
    void reduceStamina(double amount);
}
