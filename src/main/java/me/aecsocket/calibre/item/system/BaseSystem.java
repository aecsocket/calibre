package me.aecsocket.calibre.item.system;

import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;

// todo docs
public abstract class BaseSystem implements CalibreSystem {
    protected transient CalibreComponent parent;
    protected transient CalibrePlugin plugin;

    public BaseSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibreComponent getParent() { return parent; }

    @Override
    public void initialize(CalibreComponent parent) {
        this.parent = parent;
        plugin = parent.getPlugin();
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getNameKey() { return "system." + getId(); }

    protected ComponentTree getTree() { return parent.getTree(); }
    protected <T> T stat(String key) { return parent.stat(key); }
    protected <T> T callEvent(T event) { return parent.callEvent(event); }
}
