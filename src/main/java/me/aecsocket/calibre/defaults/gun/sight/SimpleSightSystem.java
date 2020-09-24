package me.aecsocket.calibre.defaults.gun.sight;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.system.CalibreSystem;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SimpleSightSystem implements CalibreSystem<Void>,
        SightSystem {
    private transient CalibrePlugin plugin;
    private transient CalibreComponent parent;
    private transient Map<String, Sight> sights = new HashMap<>();
    // such a hack :)
    transient JsonElement jSights;

    public SimpleSightSystem(CalibrePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public CalibrePlugin getPlugin() { return plugin; }
    @Override public void setPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override public String getId() { return "sight"; }

    @Override public CalibreComponent getParent() { return parent; }
    @Override public void acceptParent(CalibreComponent parent) {
        this.parent = parent;
        plugin.getStatMapAdapter().setStats(parent.getStats().getInitialMap());
        if (jSights != null)
            sights = plugin.getGson().fromJson(jSights, new TypeToken<Map<String, Sight>>(){}.getType());
    }

    @Override
    public @Nullable Collection<Class<?>> getServiceTypes() {
        return Arrays.asList(SightSystem.class);
    }

    @Override
    public Map<String, Sight> getSights() { return sights; }
    public void setSights(Map<String, Sight> sights) { this.sights = sights; }

    @Override public SimpleSightSystem clone() { try { return (SimpleSightSystem) super.clone(); } catch (CloneNotSupportedException e) { return null; } }
    @Override public SimpleSightSystem copy() { return clone(); }
}
