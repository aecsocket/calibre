package com.gitlab.aecsocket.calibre.core.world.user;

import com.gitlab.aecsocket.unifiedframework.core.scheduler.TaskContext;

public interface StabilizableUser extends ItemUser {
    boolean stabilize(TaskContext taskContext);
    double stamina();
    double maxStamina();
    void reduceStamina(double amount);
}
