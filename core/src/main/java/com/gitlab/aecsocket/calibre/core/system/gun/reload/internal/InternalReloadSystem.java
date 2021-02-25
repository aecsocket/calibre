package com.gitlab.aecsocket.calibre.core.system.gun.reload.internal;

import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;

public interface InternalReloadSystem extends CalibreSystem {
    <I extends Item> void reload(GunSystem.Events.InternalReload<I> event);
}
