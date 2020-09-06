package me.aecsocket.calibre.hook;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.handle.EventHandle;
import me.aecsocket.calibre.item.animation.Animation;
import me.aecsocket.calibre.item.animation.AnimationAdapter;
import me.aecsocket.calibre.item.component.CalibreComponent;
import me.aecsocket.calibre.item.component.CalibreComponentAdapter;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptorAdapter;
import me.aecsocket.calibre.stat.AnimationStat;
import me.aecsocket.calibre.util.AcceptsCalibrePluginAdapter;
import me.aecsocket.calibre.util.protocol.CalibreProtocol;
import me.aecsocket.unifiedframework.gui.GUIVector;
import me.aecsocket.unifiedframework.item.ItemManager;
import me.aecsocket.unifiedframework.locale.TranslationMap;
import me.aecsocket.unifiedframework.locale.TranslationMapAdapter;
import me.aecsocket.unifiedframework.stat.Stat;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.json.DefaultedRuntimeTypeAdapterFactory;
import me.aecsocket.unifiedframework.util.json.JsonAdapters;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

/**
 * The core {@link CalibreHook}. This provides core GSON adapters and capability to deserialize the most
 * basic Calibre class. This does not provide adapters for default types.
 */
public class CalibreCoreHook implements CalibreHook {
    private final DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> componentDescriptorAdapter = DefaultedRuntimeTypeAdapterFactory.of(ComponentDescriptor.class);
    private final StatMapAdapter statMapAdapter = new StatMapAdapter();
    private CalibreComponentAdapter componentAdapter;
    private CalibrePlugin plugin;

    public DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> getComponentDescriptorAdapter() { return componentDescriptorAdapter; }
    public StatMapAdapter getStatMapAdapter() { return statMapAdapter; }
    public CalibreComponentAdapter getComponentAdapter() { return componentAdapter; }

    public CalibrePlugin getPlugin() { return plugin; }
    @Override public void acceptPlugin(CalibrePlugin plugin) { this.plugin = plugin; }

    @Override
    public void initialize() {
        componentAdapter = new CalibreComponentAdapter(plugin, statMapAdapter);
        ItemManager itemManager = plugin.getItemManager();
        itemManager.registerAdapter(componentAdapter);

        Bukkit.getPluginManager().registerEvents(new EventHandle(plugin), plugin);
        CalibreProtocol.initialize(plugin);
    }

    @Override
    public void initializeGson(GsonBuilder builder) {
        builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(componentAdapter)
                .registerTypeAdapterFactory(new AcceptsCalibrePluginAdapter(plugin))
                .registerTypeAdapterFactory(componentDescriptorAdapter)

                .registerTypeAdapter(Animation.class, new AnimationAdapter(plugin))
                .registerTypeAdapter(StatMap.class, statMapAdapter)
                .registerTypeAdapter(TranslationMap.class, new TranslationMapAdapter())
                .registerTypeAdapter(ComponentDescriptor.class, new ComponentDescriptorAdapter(plugin.getRegistry()));
    }
}
