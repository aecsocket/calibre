package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.defaults.system.ComponentProviderSystem;

public interface AmmoStorageSystem extends ComponentProviderSystem {
    String getIcon();
    String getEmptyIcon();
    int getCapacity();
}
