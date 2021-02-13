package me.aecsocket.calibre.system.gun;

import me.aecsocket.calibre.world.user.ItemUser;
import me.aecsocket.unifiedframework.loop.TickContext;

public interface SwayStabilization {
    boolean stabilizes(TickContext tickContext, ItemUser user);
}
