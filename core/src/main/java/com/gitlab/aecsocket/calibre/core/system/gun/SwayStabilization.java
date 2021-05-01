package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.scheduler.TaskContext;

public interface SwayStabilization {
    boolean stabilizes(TaskContext ctx, ItemUser user);
}
