package me.aecsocket.calibre.world.user;

import me.aecsocket.unifiedframework.loop.TickContext;

public interface StabilizableUser extends ItemUser {
    boolean stabilize(TickContext tickContext);
    double stamina();
    double maxStamina();
    void reduceStamina(double amount);
}
