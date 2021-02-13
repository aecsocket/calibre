package me.aecsocket.calibre.system.gun.reload.internal;

import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.gun.GunSystem;
import me.aecsocket.calibre.world.Item;

public interface InternalReloadSystem extends CalibreSystem {
    <I extends Item> void reload(GunSystem.Events.InternalReload<I> event);
}
