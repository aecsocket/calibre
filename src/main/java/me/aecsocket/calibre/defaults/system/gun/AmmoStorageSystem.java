package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.defaults.system.ComponentProviderSystem;
import me.aecsocket.calibre.item.component.CalibreComponentSlot;

public interface AmmoStorageSystem extends ComponentProviderSystem {
    String getIcon();
    String getEmptyIcon();
    int getCapacity();

    void reload(CalibreComponentSlot slot, GunSystem.Events.PreReload event);
}
