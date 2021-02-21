package com.gitlab.aecsocket.calibre.core.system.gun;

import com.gitlab.aecsocket.calibre.core.world.user.ItemUser;
import com.gitlab.aecsocket.unifiedframework.core.loop.TickContext;

public interface SwayStabilization {
    boolean stabilizes(TickContext tickContext, ItemUser user);
}
