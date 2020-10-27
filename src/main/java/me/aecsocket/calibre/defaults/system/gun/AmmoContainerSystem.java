package me.aecsocket.calibre.defaults.system.gun;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.defaults.system.ComponentStorageSystem;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.LoadTimeOnly;

import java.util.Collection;
import java.util.Collections;

public class AmmoContainerSystem extends ComponentStorageSystem implements AmmoStorageSystem {
    public static final String ID = "ammo_container";

    @LoadTimeOnly private int capacity;

    public AmmoContainerSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    @Override public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public AmmoContainerSystem clone() { return (AmmoContainerSystem) super.clone(); }
    @Override public AmmoContainerSystem copy() { return (AmmoContainerSystem) super.copy(); }
}
