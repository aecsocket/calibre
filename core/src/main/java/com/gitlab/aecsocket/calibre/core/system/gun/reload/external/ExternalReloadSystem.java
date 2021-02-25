package com.gitlab.aecsocket.calibre.core.system.gun.reload.external;

import com.gitlab.aecsocket.calibre.core.system.CalibreSystem;
import com.gitlab.aecsocket.calibre.core.system.gun.GunSystem;
import com.gitlab.aecsocket.calibre.core.world.item.Item;

public interface ExternalReloadSystem extends CalibreSystem {
    <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event);
}
