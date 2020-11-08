package me.aecsocket.calibre.defaults.system.gun.firemode;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.util.HasDependencies;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FireModeSystem extends BaseSystem implements HasDependencies<FireModeSystem.Dependencies> {
    public static final String ID = "fire_mode";

    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    protected static class Dependencies {
        private JsonArray modes;
    }

    private transient Dependencies dependencies;
    private transient List<FireMode> modes = new ArrayList<>();

    public FireModeSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    @Override public Dependencies getLoadDependencies() { return dependencies; }
    @Override public void setLoadDependencies(Dependencies dependencies) { this.dependencies = dependencies; }
    @Override public Type getLoadDependenciesType() { return Dependencies.class; }

    public List<FireMode> getModes() { return modes; }
    public void setModes(List<FireMode> modes) { this.modes = modes; }

    @Override
    public void systemInitialize(CalibreComponent parent) throws SystemInitializationException {
        if (dependencies.modes != null)
            modes = plugin.getGson().fromJson(dependencies.modes, new TypeToken<List<FireMode>>(){}.getType());
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public FireModeSystem clone() { return (FireModeSystem) super.clone(); }
    @Override public FireModeSystem copy() { return clone(); }
}