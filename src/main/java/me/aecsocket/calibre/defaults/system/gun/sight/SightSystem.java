package me.aecsocket.calibre.defaults.system.gun.sight;

import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.ComponentTree;
import me.aecsocket.calibre.item.system.BaseSystem;
import me.aecsocket.calibre.item.system.LoadTimeOnly;
import me.aecsocket.calibre.item.system.SystemInitializationException;
import me.aecsocket.calibre.item.util.LoadTimeDependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SightSystem extends BaseSystem {
    public static final String ID = "sight";

    /**
     * Temporarily stores info on deserialization for resolution later.
     */
    protected static class Dependencies {
        private JsonArray sights;
    }

    @LoadTimeDependencies private transient Dependencies dependencies;
    @LoadTimeOnly private transient List<Sight> sights = new ArrayList<>();

    public SightSystem(CalibrePlugin plugin) {
        super(plugin);
    }

    public List<Sight> getSights() { return sights; }
    public void setSights(List<Sight> sights) { this.sights = sights; }

    @Override
    public void systemInitialize(CalibreComponent parent) throws SystemInitializationException {
        if (dependencies.sights != null)
            sights = plugin.getGson().fromJson(dependencies.sights, new TypeToken<List<Sight>>() {}.getType());
    }

    @Override
    public void initialize(CalibreComponent parent, ComponentTree tree) {
        super.initialize(parent, tree);
    }

    @Override public String getId() { return ID; }
    @Override public Collection<String> getDependencies() { return Collections.emptyList(); }
    @Override public SightSystem clone() { return (SightSystem) super.clone(); }
    @Override public SightSystem copy() { return clone(); }

    @Override
    public String toString() { return "SightSystem" + sights; }
}
