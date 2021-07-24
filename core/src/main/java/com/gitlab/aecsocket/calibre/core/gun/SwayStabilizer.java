package com.gitlab.aecsocket.calibre.core.gun;

import com.gitlab.aecsocket.minecommons.core.vector.cartesian.Vector2;
import com.gitlab.aecsocket.sokol.core.system.System;
import com.gitlab.aecsocket.sokol.core.tree.event.ItemTreeEvent;

public interface SwayStabilizer extends System.Instance {
    Vector2 stabilization(ItemTreeEvent.Hold event);
}
