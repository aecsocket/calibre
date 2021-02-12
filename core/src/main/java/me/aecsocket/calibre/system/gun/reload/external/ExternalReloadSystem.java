package me.aecsocket.calibre.system.gun.reload.external;

import me.aecsocket.calibre.system.CalibreSystem;
import me.aecsocket.calibre.system.gun.GunSystem;
import me.aecsocket.calibre.world.Item;

public interface ExternalReloadSystem extends CalibreSystem {
    <I extends Item> void reload(GunSystem.Events.ExternalReload<I> event);
}
