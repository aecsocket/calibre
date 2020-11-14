package me.aecsocket.calibre.defaults.system.gun.ammo;

import me.aecsocket.calibre.defaults.system.ComponentProviderSystem;
import me.aecsocket.calibre.defaults.system.gun.GunSystem;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;

public interface AmmoStorageSystem extends ComponentProviderSystem {
    int getCapacity();
    String getIcon();
    String getEmptyIcon();

    void reload(CalibreComponentSlot slot, GunSystem.Events.PreReload event);
}
