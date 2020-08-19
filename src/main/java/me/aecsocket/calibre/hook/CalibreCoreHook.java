package me.aecsocket.calibre.hook;

import com.google.gson.*;
import me.aecsocket.calibre.CalibrePlugin;
import me.aecsocket.calibre.item.component.CalibreComponentAdapter;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptor;
import me.aecsocket.calibre.item.component.descriptor.ComponentDescriptorAdapter;
import me.aecsocket.calibre.item.system.TestSystem;
import me.aecsocket.calibre.util.AcceptsCalibrePluginAdapter;
import me.aecsocket.unifiedframework.locale.LocaleManager;
import me.aecsocket.unifiedframework.registry.Registry;
import me.aecsocket.unifiedframework.resource.Settings;
import me.aecsocket.unifiedframework.stat.StatMap;
import me.aecsocket.unifiedframework.stat.StatMapAdapter;
import me.aecsocket.unifiedframework.util.json.DefaultedRuntimeTypeAdapterFactory;

/**
 * The core {@link CalibreHook}. This provides core GSON adapters and capability to deserialize the most
 * basic Calibre class. This does not provide adapters for default types.
 */
public class CalibreCoreHook implements CalibreHook {
    private final DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> componentDescriptorAdapter = DefaultedRuntimeTypeAdapterFactory.of(ComponentDescriptor.class);
    private final StatMapAdapter statMapAdapter = new StatMapAdapter();

    public DefaultedRuntimeTypeAdapterFactory<ComponentDescriptor> getComponentDescriptorAdapter() { return componentDescriptorAdapter; }
    public StatMapAdapter getStatMapAdapter() { return statMapAdapter; }

    @Override
    public void initializeGson(CalibrePlugin plugin, GsonBuilder builder) {
        builder
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapterFactory(new AcceptsCalibrePluginAdapter(plugin))
                .registerTypeAdapter(StatMap.class, statMapAdapter)
                .registerTypeAdapterFactory(new CalibreComponentAdapter(plugin.getRegistry(), statMapAdapter))

                .registerTypeAdapterFactory(componentDescriptorAdapter)
                .registerTypeAdapter(ComponentDescriptor.class, new ComponentDescriptorAdapter());
    }

    @Override
    public void preLoadRegister(CalibrePlugin plugin, Registry registry, LocaleManager localeManager, Settings settings) {
        registry.register(new TestSystem()); // TODO remove
    }
}
